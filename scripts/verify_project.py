#!/usr/bin/env python3
"""
Static consistency checks for the AiWatch project.

This does NOT replace a Gradle build -- it cannot resolve androidx symbols or
type-check Kotlin. What it does catch, without a JVM or an Android SDK, is the
class of mistake that is easy to make and expensive to discover in CI:

  * unparseable Kotlin (real tree-sitter parse, not a regex)
  * malformed AndroidManifest / resource XML
  * version-catalog aliases referenced but never declared
  * Gradle modules declared in settings.gradle.kts that do not exist
  * manifest components with no backing Kotlin class
  * @string / @drawable / @color / @mipmap / @style references that do not resolve
  * R.string.* references from Kotlin with no matching resource
  * duplicate resource names inside one qualifier folder
  * malformed GitHub Actions workflow YAML

Run:  python3 scripts/verify_project.py
"""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODULES = ("core", "app", "wear")
ANDROID_NS = "http://schemas.android.com/apk/res/android"

errors: list[str] = []
warnings: list[str] = []
checks_run = 0


def check(name: str, fn):
    global checks_run
    checks_run += 1
    try:
        fn()
        print(f"  ok    {name}")
    except Exception as exc:  # noqa: BLE001 - report, never crash the run
        print(f"  FAIL  {name}: {exc}")
        errors.append(f"{name}: {exc}")


def kt_files() -> list[Path]:
    return sorted(p for p in ROOT.rglob("*.kt") if "/build/" not in str(p))


def xml_files() -> list[Path]:
    return sorted(p for p in ROOT.rglob("*.xml") if "/build/" not in str(p))


# --------------------------------------------------------------------- Kotlin


def check_kotlin_parses() -> None:
    try:
        import tree_sitter_kotlin
        from tree_sitter import Language, Parser
    except ImportError:
        warnings.append(
            "tree-sitter-kotlin is not installed; Kotlin syntax was NOT verified. "
            "Install with: pip install tree-sitter tree-sitter-kotlin"
        )
        return

    language = Language(tree_sitter_kotlin.language())
    try:
        parser = Parser(language)
    except TypeError:  # older binding
        parser = Parser()
        parser.set_language(language)

    bad: list[str] = []
    parsed = 0
    for path in kt_files():
        source = path.read_bytes()
        tree = parser.parse(source)
        parsed += 1
        problems = []
        stack = [tree.root_node]
        while stack:
            node = stack.pop()
            if node.type == "ERROR" or node.is_missing:
                problems.append(node.start_point[0] + 1)
            stack.extend(node.children)
        if problems:
            bad.append(f"{rel(path)} (lines {sorted(set(problems))[:5]})")

    if parsed == 0:
        raise AssertionError("no Kotlin files were parsed - is the tree empty?")
    if bad:
        raise AssertionError(f"{len(bad)} file(s) failed to parse:\n    " + "\n    ".join(bad))
    print(f"        parsed {parsed} Kotlin file(s)")


# ------------------------------------------------------------------------ XML


def check_xml_parses() -> None:
    bad = []
    for path in xml_files():
        try:
            ET.fromstring(path.read_text(encoding="utf-8"))
        except ET.ParseError as exc:
            bad.append(f"{rel(path)}: {exc}")
    if bad:
        raise AssertionError("\n    " + "\n    ".join(bad))


# ------------------------------------------------------------- version catalog


def catalog_aliases() -> dict[str, set[str]]:
    """Returns {section: {accessor}} where accessor uses dots, as Gradle exposes it."""
    toml = (ROOT / "gradle" / "libs.versions.toml").read_text(encoding="utf-8")
    aliases: dict[str, set[str]] = {"libraries": set(), "plugins": set()}
    section = None
    for raw in toml.splitlines():
        line = raw.strip()
        if line.startswith("["):
            section = line.strip("[]")
            continue
        if section in aliases and "=" in line and not line.startswith("#"):
            alias = line.split("=", 1)[0].strip()
            if alias:
                aliases[section].add(alias.replace("-", "."))
    return aliases


def check_catalog_references() -> None:
    aliases = catalog_aliases()
    if not aliases["libraries"] or not aliases["plugins"]:
        raise AssertionError("no aliases were read from libs.versions.toml")

    pattern = re.compile(r"\blibs\.([a-z0-9]+(?:\.[a-z0-9]+)*)", re.IGNORECASE)
    missing: list[str] = []
    for gradle_file in sorted(ROOT.rglob("*.gradle.kts")):
        if "/build/" in str(gradle_file):
            continue
        for lineno, line in enumerate(gradle_file.read_text(encoding="utf-8").splitlines(), 1):
            if line.strip().startswith("//"):
                continue
            for match in pattern.finditer(line):
                accessor = match.group(1).lower()
                # `libs.versions.foo` addresses the [versions] table, not an alias.
                if accessor.startswith("versions."):
                    continue
                # `libs.plugins.foo` addresses [plugins]; anything else [libraries].
                if accessor.startswith("plugins."):
                    pool, key = aliases["plugins"], accessor.removeprefix("plugins.")
                else:
                    pool, key = aliases["libraries"], accessor
                if key not in pool:
                    missing.append(f"{rel(gradle_file)}:{lineno} libs.{match.group(1)}")
    if missing:
        raise AssertionError("unknown catalog accessor(s):\n    " + "\n    ".join(sorted(set(missing))))
    total = sum(len(v) for v in aliases.values())
    print(f"        {total} catalog aliases available "
          f"({len(aliases['libraries'])} libraries, {len(aliases['plugins'])} plugins)")


def check_versions_table_used() -> None:
    toml = (ROOT / "gradle" / "libs.versions.toml").read_text(encoding="utf-8")
    declared = set()
    section = None
    for raw in toml.splitlines():
        line = raw.strip()
        if line.startswith("["):
            section = line.strip("[]")
            continue
        if section == "versions" and "=" in line and not line.startswith("#"):
            declared.add(line.split("=", 1)[0].strip())

    referenced = set(re.findall(r"version\.ref\s*=\s*\"([^\"]+)\"", toml))
    unknown = referenced - declared
    if unknown:
        raise AssertionError(f"version.ref points at undeclared version(s): {sorted(unknown)}")


# -------------------------------------------------------------------- modules


def check_module_layout() -> None:
    settings = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
    included = set(re.findall(r"include\(\":([A-Za-z0-9_.-]+)\"\)", settings))
    if included != set(MODULES):
        raise AssertionError(
            f"settings.gradle.kts includes {sorted(included)}, expected {sorted(MODULES)}"
        )

    problems = []
    for module in MODULES:
        base = ROOT / module
        if not (base / "build.gradle.kts").exists():
            problems.append(f"{module}/build.gradle.kts is missing")
        if module != "core" and not (base / "src" / "main" / "AndroidManifest.xml").exists():
            problems.append(f"{module} has no AndroidManifest.xml")
    if problems:
        raise AssertionError("\n    " + "\n    ".join(problems))


# ------------------------------------------------------------------ manifests


def manifest_components(module: str) -> list[str]:
    path = ROOT / module / "src" / "main" / "AndroidManifest.xml"
    root = ET.fromstring(path.read_text(encoding="utf-8"))
    names = []
    for tag in ("activity", "service", "receiver", "provider"):
        for node in root.iter(tag):
            name = node.get(f"{{{ANDROID_NS}}}name")
            if name:
                names.append(name)
    return names


def check_manifest_classes_exist() -> None:
    problems = []
    for module in MODULES:
        manifest = ROOT / module / "src" / "main" / "AndroidManifest.xml"
        if not manifest.exists():
            continue
        src_root = ROOT / module / "src" / "main" / "java"
        for component in manifest_components(module):
            if component.startswith("."):
                fqcn = namespace_of(module) + component
            elif component.startswith("androidx.") or component.startswith("com.google."):
                continue
            else:
                fqcn = component
            relative = fqcn.replace(".", "/") + ".kt"
            if not (src_root / relative).exists():
                problems.append(f"{module}: {component} -> {relative} not found")
    if problems:
        raise AssertionError("\n    " + "\n    ".join(problems))


def namespace_of(module: str) -> str:
    build = (ROOT / module / "build.gradle.kts").read_text(encoding="utf-8")
    match = re.search(r"namespace\s*=\s*\"([^\"]+)\"", build)
    if not match:
        raise AssertionError(f"{module}/build.gradle.kts declares no namespace")
    return match.group(1)


def check_namespace_matches_sources() -> None:
    problems = []
    for module in MODULES:
        namespace = namespace_of(module)
        src_root = ROOT / module / "src" / "main" / "java"
        if not src_root.exists():
            continue
        expected = src_root / namespace.replace(".", "/")
        if not expected.exists():
            problems.append(f"{module}: namespace {namespace} has no matching source directory")
    if problems:
        raise AssertionError("\n    " + "\n    ".join(problems))


# ------------------------------------------------------------------ resources


def resource_dirs(module: str) -> list[Path]:
    res = ROOT / module / "src" / "main" / "res"
    return [d for d in sorted(res.iterdir()) if d.is_dir()] if res.exists() else []


def collect_resources(module: str) -> dict[str, set[str]]:
    """Maps resource type -> names defined anywhere under res/ (plus the manifest)."""
    found: dict[str, set[str]] = {}

    def add(kind: str, name: str) -> None:
        found.setdefault(kind, set()).add(name)

    for folder in resource_dirs(module):
        for path in sorted(folder.glob("*.xml")):
            kind = folder.name.split("-")[0]
            if kind in {"values", }:
                try:
                    root = ET.fromstring(path.read_text(encoding="utf-8"))
                except ET.ParseError:
                    continue
                for child in root:
                    tag = child.tag
                    name = child.get("name")
                    if name and tag in {"string", "color", "dimen", "bool", "integer", "style", "plurals", "string-array"}:
                        add({"string": "string", "color": "color", "style": "style"}.get(tag, tag), name)
            else:
                add(kind, path.stem)
    return found


def check_resource_references_resolve() -> None:
    pattern = re.compile(r"@(string|color|drawable|mipmap|style|dimen|bool|integer|array)/([A-Za-z0-9_.]+)")
    problems = []

    for module in MODULES:
        defined = collect_resources(module)
        # core is a library; its resources are visible to consumers.
        if module != "core":
            for kind, names in collect_resources("core").items():
                defined.setdefault(kind, set()).update(names)
        # Framework resources.
        scan_roots = [ROOT / module / "src"]
        if module != "core":
            scan_roots.append(ROOT / "core" / "src")

        for root_dir in scan_roots:
            if not root_dir.exists():
                continue
            for path in sorted(root_dir.rglob("*.xml")):
                text = path.read_text(encoding="utf-8")
                for kind, name in pattern.findall(text):
                    # Style resources are dotted by convention
                    # (@style/Theme.AiWatch.Phone); every other type may carry a
                    # qualifier suffix instead, so only those get split.
                    base = name if kind == "style" else name.split(".")[0]
                    if base in ANDROID_BUILTINS:
                        continue
                    if base not in defined.get(kind, set()):
                        problems.append(f"{rel(path)}: @{kind}/{name} is not defined")
    if problems:
        raise AssertionError("\n    " + "\n    ".join(sorted(set(problems))))


def check_duplicate_resource_names() -> None:
    problems = []
    for module in MODULES:
        for folder in resource_dirs(module):
            if not folder.name.startswith("values"):
                continue
            seen: dict[tuple[str, str], str] = {}
            for path in sorted(folder.glob("*.xml")):
                try:
                    root = ET.fromstring(path.read_text(encoding="utf-8"))
                except ET.ParseError:
                    continue
                for child in root:
                    name = child.get("name")
                    if not name:
                        continue
                    key = (child.tag, name)
                    if key in seen:
                        problems.append(
                            f"{module}/{folder.name}: {child.tag} '{name}' defined in "
                            f"{seen[key]} and {path.name}"
                        )
                    seen[key] = path.name
    if problems:
        raise AssertionError("\n    " + "\n    ".join(problems))


def check_kotlin_string_resources() -> None:
    pattern = re.compile(r"\bR\.string\.([A-Za-z0-9_]+)")
    problems = []
    for module in MODULES:
        defined = collect_resources(module).get("string", set())
        if module != "core":
            defined |= collect_resources("core").get("string", set())
        src = ROOT / module / "src" / "main" / "java"
        if not src.exists():
            continue
        for path in sorted(src.rglob("*.kt")):
            for name in pattern.findall(path.read_text(encoding="utf-8")):
                if name not in defined:
                    problems.append(f"{rel(path)}: R.string.{name} is not defined")
    if problems:
        raise AssertionError("\n    " + "\n    ".join(sorted(set(problems))))


ANDROID_BUILTINS = {
    "Theme_DeviceDefault",
}


# ------------------------------------------------------------------ workflows


def check_workflows_parse() -> None:
    import yaml

    workflow_dir = ROOT / ".github" / "workflows"
    files = sorted(workflow_dir.glob("*.yml")) + sorted(workflow_dir.glob("*.yaml"))
    if not files:
        raise AssertionError("no workflow files found")
    for path in files:
        data = yaml.safe_load(path.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            raise AssertionError(f"{rel(path)} did not parse to a mapping")
        # `on:` is parsed as the boolean True by YAML 1.1 parsers.
        triggers = data.get("on", data.get(True))
        if not triggers:
            raise AssertionError(f"{rel(path)} declares no triggers")
        jobs = data.get("jobs")
        if not jobs:
            raise AssertionError(f"{rel(path)} declares no jobs")
        for job_name, job in jobs.items():
            if "runs-on" not in job:
                raise AssertionError(f"{rel(path)}: job '{job_name}' has no runs-on")
            steps = job.get("steps")
            if not steps:
                raise AssertionError(f"{rel(path)}: job '{job_name}' has no steps")
    print(f"        {len(files)} workflow file(s) valid")


def check_gradle_delimiters_balanced() -> None:
    problems = []
    pairs = {")": "(", "]": "[", "}": "{"}
    for path in sorted(ROOT.rglob("*.gradle.kts")):
        if "/build/" in str(path):
            continue
        text = strip_kotlin_comments(path.read_text(encoding="utf-8"))
        stack: list[str] = []
        in_string = False
        quote = ""
        index = 0
        while index < len(text):
            char = text[index]
            if in_string:
                if char == "\\":
                    index += 2
                    continue
                if char == quote:
                    in_string = False
            else:
                if char in "\"'":
                    in_string = True
                    quote = char
                elif char in "([{":
                    stack.append(char)
                elif char in ")]}":
                    if not stack or stack[-1] != pairs[char]:
                        problems.append(f"{rel(path)}: unbalanced '{char}' at offset {index}")
                        break
                    stack.pop()
            index += 1
        if stack:
            problems.append(f"{rel(path)}: unclosed {stack}")
    if problems:
        raise AssertionError("\n    " + "\n    ".join(problems))


def strip_kotlin_comments(text: str) -> str:
    text = re.sub(r"//.*", "", text)
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    return text


# --------------------------------------------------- intra-project references


DECL_PATTERN = re.compile(
    r"^(?P<indent>\s*)"
    r"(?:@\w+(?:\([^)]*\))?\s*)*"
    r"(?:public\s+|internal\s+|private\s+|protected\s+)?"
    r"(?:suspend\s+|inline\s+|operator\s+|infix\s+|tailrec\s+|external\s+|abstract\s+|open\s+|"
    r"data\s+|enum\s+|sealed\s+|value\s+|annotation\s+|inner\s+|companion\s+)*"
    r"(?:fun|class|object|interface)\s+"
    r"(?:<[^>]*>\s*)?"
    r"(?:[A-Za-z0-9_]+\s*\.\s*)?"
    r"(?P<name>[A-Za-z0-9_]+)",
    re.MULTILINE,
)

# `Name(` where Name is capitalised and not a member access or an annotation.
CALL_PATTERN = re.compile(r"(?<![\w.@$])([A-Z][A-Za-z0-9_]*)\s*(?:<[^<>]*>)?\s*\(")

IMPORT_PATTERN = re.compile(r"^import\s+([\w.]+)(?:\s+as\s+(\w+))?", re.MULTILINE)


VALUE_PATTERN = re.compile(
    r"^(?P<indent>\s*)"
    r"(?:@\w+(?:\([^)]*\))?\s*)*"
    r"(?:public\s+|internal\s+|private\s+|protected\s+|const\s+|lateinit\s+)*"
    r"(?:val|var)\s+"
    r"(?:[A-Za-z0-9_]+\s*\.\s*)?"
    r"(?P<name>[A-Za-z0-9_]+)",
    re.MULTILINE,
)

ENUM_CLASS_PATTERN = re.compile(r"enum\s+class\s+[A-Za-z0-9_]+[^{]*\{")

# Auto-imported by Kotlin/JVM: kotlin.*, kotlin.collections.*, java.lang.*.
STDLIB_NAMES = {
    "Any", "Array", "ArrayDeque", "ArrayList", "Boolean", "Byte", "Char", "Double",
    "Enum", "Error", "Exception", "Float", "HashMap", "HashSet", "IllegalStateException",
    "IllegalArgumentException", "IndexOutOfBoundsException", "Int", "Long", "Nothing",
    "NullPointerException", "NumberFormatException", "Object", "Pair", "Result",
    "RuntimeException", "Short", "String", "StringBuilder", "Throwable", "Triple",
    "Unit", "UnsupportedOperationException",
}


def enum_entries(text: str) -> set[str]:
    """Extracts the entry names declared in every enum body in the file."""
    entries: set[str] = set()
    for header in ENUM_CLASS_PATTERN.finditer(text):
        depth = 1
        index = header.end()
        body_start = index
        while index < len(text) and depth > 0:
            if text[index] == "{":
                depth += 1
            elif text[index] == "}":
                depth -= 1
            elif depth == 1 and text[index] == ";":
                break
            index += 1
        body = text[body_start:index]
        # Entries run up to the first ';' (or the closing brace), comma separated.
        for part in body.split(","):
            match = re.match(r"\s*([A-Z][A-Za-z0-9_]*)\s*(?:\(|$)", part)
            if match:
                entries.add(match.group(1))
    return entries


def project_declarations() -> set[str]:
    names: set[str] = set()
    for path in kt_files():
        text = path.read_text(encoding="utf-8")
        for pattern in (DECL_PATTERN, VALUE_PATTERN):
            for match in pattern.finditer(text):
                names.add(match.group("name"))
        names |= enum_entries(text)
    return names


def check_internal_imports_resolve() -> None:
    """Every `import com.aiwatch...` must name something we actually declare."""
    declared = project_declarations()
    problems = []
    for path in kt_files():
        text = path.read_text(encoding="utf-8")
        for match in IMPORT_PATTERN.finditer(text):
            target = match.group(1)
            if not target.startswith("com.aiwatch."):
                continue
            symbol = target.rsplit(".", 1)[-1]
            if symbol not in declared:
                problems.append(f"{rel(path)}: import {target}")
    if problems:
        raise AssertionError("\n    " + "\n    ".join(sorted(set(problems))))
    print(f"        {len(declared)} project declarations indexed")


def check_composable_calls_resolve() -> None:
    """
    Capitalised call sites (composables and constructors) must be declared in this
    project or explicitly imported. Catches calls to helpers that were renamed,
    deleted, or never written -- the most likely compile break in this codebase.
    """
    declared = project_declarations()
    problems = []

    for path in kt_files():
        text = strip_kotlin_comments(path.read_text(encoding="utf-8"))
        imported = set()
        for match in IMPORT_PATTERN.finditer(text):
            imported.add(match.group(2) or match.group(1).rsplit(".", 1)[-1])
        # A wildcard import means we cannot reason about this file.
        if ".*" in text:
            continue

        code_only = re.sub(r'"(?:\\.|[^"\\])*"', '""', text)
        for match in CALL_PATTERN.finditer(code_only):
            name = match.group(1)
            if name in declared or name in imported or name in STDLIB_NAMES:
                continue
            problems.append(f"{rel(path)}: {name}(")

    if problems:
        unique = sorted(set(problems))
        raise AssertionError(f"{len(unique)} unresolved call site(s):\n    " + "\n    ".join(unique))


def mask_comments_and_strings(text: str) -> str:
    """
    Replaces comment and string contents with spaces, preserving length so byte
    offsets still align with the parse tree. Handles // and /* */ comments and
    both "..." and triple-quoted strings.
    """
    out = list(text)
    i = 0
    n = len(text)

    def blank(start: int, end: int) -> None:
        for index in range(start, min(end, n)):
            if out[index] != "\n":
                out[index] = " "

    while i < n:
        two = text[i:i + 2]
        if two == "//":
            end = text.find("\n", i)
            end = n if end < 0 else end
            blank(i, end)
            i = end
        elif two == "/*":
            end = text.find("*/", i + 2)
            end = n if end < 0 else end + 2
            blank(i, end)
            i = end
        elif text.startswith('"""', i):
            end = text.find('"""', i + 3)
            end = n if end < 0 else end + 3
            blank(i, end)
            i = end
        elif text[i] == '"':
            j = i + 1
            while j < n:
                if text[j] == "\\":
                    j += 2
                    continue
                if text[j] == '"':
                    j += 1
                    break
                if text[j] == "\n":
                    break
                j += 1
            blank(i, j)
            i = j
        else:
            i += 1
    return "".join(out)


def check_no_break_in_lambda() -> None:
    """
    `break`/`continue` inside an inline lambda is an experimental Kotlin feature
    and fails to compile without an opt-in flag. It is easy to write by accident
    in a `?: run { ... break }` tail, and the compiler message is cryptic.

    Locates the keywords by regex and uses the tree-sitter AST for the exact byte
    ranges of lambda bodies, loops, strings and comments.
    """
    try:
        import tree_sitter_kotlin
        from tree_sitter import Language, Parser
    except ImportError:
        warnings.append("tree-sitter-kotlin missing; break-in-lambda check skipped")
        return

    language = Language(tree_sitter_kotlin.language())
    try:
        parser = Parser(language)
    except TypeError:
        parser = Parser()
        parser.set_language(language)

    loops = {"while_statement", "for_statement", "do_while_statement"}
    keyword = re.compile(r"\b(break|continue)\b")

    problems = []
    for path in kt_files():
        text = path.read_text(encoding="utf-8")
        tree = parser.parse(text.encode("utf-8"))

        lambdas, loop_ranges = [], []
        stack = [tree.root_node]
        while stack:
            node = stack.pop()
            if node.type == "lambda_literal":
                lambdas.append((node.start_byte, node.end_byte))
            elif node.type in loops:
                loop_ranges.append((node.start_byte, node.end_byte))
            stack.extend(node.children)

        # tree-sitter's comment coverage for this grammar is incomplete, so mask
        # comments and strings textually instead. Offsets are preserved so the
        # masked positions still line up with the AST ranges.
        searchable = mask_comments_and_strings(text)

        def innermost(ranges, pos):
            best = None
            for start, end in ranges:
                if start <= pos < end and (best is None or end - start < best[1] - best[0]):
                    best = (start, end)
            return best

        for match in keyword.finditer(searchable):
            pos = match.start()
            lam = innermost(lambdas, pos)
            if lam is None:
                continue
            loop = innermost(loop_ranges, pos)
            # Both ranges contain `pos`, so one nests inside the other. The jump
            # is legal only when the loop is the innermost of the two, i.e. the
            # lambda is the loop's own body. If the lambda is innermost, the jump
            # tries to leave it -- the experimental case.
            lambda_is_innermost = loop is None or (
                loop[0] <= lam[0] and lam[1] <= loop[1]
            )
            if lambda_is_innermost:
                line = text.count("\n", 0, pos) + 1
                problems.append(f"{rel(path)}:{line} '{match.group(1)}' inside a lambda")

    if problems:
        raise AssertionError("\n    " + "\n    ".join(sorted(set(problems))))


# --------------------------------------------------------------------- helper


def rel(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


def main() -> int:
    print("AiWatch project checks")
    print("-" * 46)
    check("Kotlin sources parse (tree-sitter)", check_kotlin_parses)
    check("No break/continue inside lambdas", check_no_break_in_lambda)
    check("XML resources and manifests parse", check_xml_parses)
    check("Gradle delimiters balanced", check_gradle_delimiters_balanced)
    check("Version catalog references resolve", check_catalog_references)
    check("version.ref targets declared", check_versions_table_used)
    check("Module layout matches settings", check_module_layout)
    check("Manifest components have classes", check_manifest_classes_exist)
    check("Namespaces match source directories", check_namespace_matches_sources)
    check("Resource references resolve", check_resource_references_resolve)
    check("No duplicate resource names", check_duplicate_resource_names)
    check("R.string.* references resolve", check_kotlin_string_resources)
    check("Internal imports resolve", check_internal_imports_resolve)
    check("Capitalised call sites resolve", check_composable_calls_resolve)
    check("GitHub workflows parse", check_workflows_parse)
    print("-" * 46)

    if warnings:
        print(f"{len(warnings)} warning(s):")
        for warning in warnings:
            print(f"  ! {warning}")

    if errors:
        print(f"\nFAILED: {errors.__len__()} of {checks_run} checks")
        return 1

    print(f"\nPASSED: all {checks_run} checks")
    return 0


if __name__ == "__main__":
    sys.exit(main())
