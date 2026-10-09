package com.karyar.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate200

/**
 * Android-Iconics inspired icon framework.
 * Provides scalable, vector-consistent icon containers with alpha fills,
 * contoured borders, gradient accents, and badges.
 */
enum class IconicsSize(val boxSize: Dp, val iconSize: Dp, val cornerRadius: Dp) {
    TINY(24.dp, 13.dp, 6.dp),
    SMALL(32.dp, 16.dp, 8.dp),
    MEDIUM(40.dp, 20.dp, 11.dp),
    LARGE(48.dp, 24.dp, 14.dp),
    HERO(58.dp, 28.dp, 16.dp)
}

@Composable
fun IconicsBox(
    icon: ImageVector,
    color: Color = AmberAccent,
    size: IconicsSize = IconicsSize.MEDIUM,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    shape: Shape = RoundedCornerShape(size.cornerRadius),
    badgeColor: Color? = null,
    badgeText: String? = null,
    isCircle: Boolean = false,
    contentDescription: String? = null
) {
    val actualShape = if (isCircle) CircleShape else shape
    val bg = backgroundColor ?: color.copy(alpha = 0.12f)
    val border = borderColor ?: color.copy(alpha = 0.22f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .size(size.boxSize)
                .clip(actualShape)
                .border(1.dp, border, actualShape),
            shape = actualShape,
            color = bg
        ) {
            Box(
                modifier = Modifier
                    .size(size.boxSize)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                color.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = color,
                    modifier = Modifier.size(size.iconSize)
                )
            }
        }

        // Badge if specified
        if (badgeColor != null || badgeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
            ) {
                if (badgeText != null) {
                    Surface(
                        shape = CircleShape,
                        color = badgeColor ?: RoseAccent,
                        modifier = Modifier.border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                } else if (badgeColor != null) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Clickable Iconics Button with tactile feedback
 */
@Composable
fun IconicsButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AmberAccent,
    size: IconicsSize = IconicsSize.MEDIUM,
    contentDescription: String? = null,
    enabled: Boolean = true,
    badgeColor: Color? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(size.cornerRadius))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        IconicsBox(
            icon = icon,
            color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            size = size,
            badgeColor = badgeColor,
            contentDescription = contentDescription
        )
    }
}
