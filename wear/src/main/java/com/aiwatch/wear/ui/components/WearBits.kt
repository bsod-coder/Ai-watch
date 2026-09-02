package com.aiwatch.wear.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.aiwatch.wear.ui.theme.WearPalette
import com.aiwatch.wear.ui.theme.WearType

/** Section heading used at the top of each scrollable screen. */
@Composable
fun WatchHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = WearType.SectionLabel,
        color = WearPalette.OnSurfaceMuted,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

/**
 * The main tappable row. A hairline-tinted pill rather than a filled card, which
 * keeps the watch face calm when a list has ten entries.
 */
@Composable
fun WatchRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tint: Color = WearPalette.Accent,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WearPalette.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = WearType.RowTitle,
                color = WearPalette.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = WearType.BodySmall,
                    color = WearPalette.OnSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

/** Circular icon button sized for a fingertip on a round display. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    tint: Color = WearPalette.Accent,
) {
    val background = when {
        !enabled -> WearPalette.Surface
        filled -> tint
        else -> WearPalette.SurfaceRaised
    }
    val contentColor = when {
        !enabled -> WearPalette.OnSurfaceMuted
        filled -> WearPalette.OnAccent
        else -> tint
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Small label chip; used for the model name and status markers. */
@Composable
fun WatchPill(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = WearPalette.Accent,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = WearType.BodySmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun WatchDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WearPalette.Hairline),
    )
}

@Composable
fun WatchEmpty(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = WearType.RowTitle,
            color = WearPalette.OnSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = WearType.BodySmall,
            color = WearPalette.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(6.dp))
            action()
        }
    }
}
