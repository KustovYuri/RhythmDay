package com.rhythmday.app.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object RhythmDayIcons {
    val Home: ImageVector by lazy {
        icon("Home") {
            path {
                moveTo(3f, 11.5f)
                lineTo(12f, 4f)
                lineTo(21f, 11.5f)
            }
            path {
                moveTo(5.5f, 10.5f)
                lineTo(5.5f, 20f)
                lineTo(18.5f, 20f)
                lineTo(18.5f, 10.5f)
            }
            path {
                moveTo(9f, 20f)
                lineTo(9f, 14f)
                lineTo(15f, 14f)
                lineTo(15f, 20f)
            }
        }
    }

    val Calendar: ImageVector by lazy {
        icon("Calendar") {
            path {
                moveTo(7f, 3f)
                lineTo(7f, 6f)
                moveTo(17f, 3f)
                lineTo(17f, 6f)
                moveTo(4f, 8f)
                lineTo(20f, 8f)
            }
            path {
                moveTo(5f, 5f)
                lineTo(19f, 5f)
                curveTo(19.55f, 5f, 20f, 5.45f, 20f, 6f)
                lineTo(20f, 19f)
                curveTo(20f, 19.55f, 19.55f, 20f, 19f, 20f)
                lineTo(5f, 20f)
                curveTo(4.45f, 20f, 4f, 19.55f, 4f, 19f)
                lineTo(4f, 6f)
                curveTo(4f, 5.45f, 4.45f, 5f, 5f, 5f)
                close()
            }
            path {
                moveTo(8f, 12f)
                lineTo(8.01f, 12f)
                moveTo(12f, 12f)
                lineTo(12.01f, 12f)
                moveTo(16f, 12f)
                lineTo(16.01f, 12f)
                moveTo(8f, 16f)
                lineTo(8.01f, 16f)
                moveTo(12f, 16f)
                lineTo(12.01f, 16f)
            }
        }
    }

    val Analytics: ImageVector by lazy {
        icon("Analytics") {
            path {
                moveTo(4f, 5f)
                lineTo(4f, 19f)
                lineTo(20f, 19f)
            }
            path {
                moveTo(7f, 15f)
                lineTo(11f, 11f)
                lineTo(14f, 14f)
                lineTo(19f, 7f)
            }
        }
    }

    val Ai: ImageVector by lazy {
        icon("Ai") {
            path {
                moveTo(12f, 3f)
                lineTo(13.4f, 7.1f)
                lineTo(17.5f, 8.5f)
                lineTo(13.4f, 9.9f)
                lineTo(12f, 14f)
                lineTo(10.6f, 9.9f)
                lineTo(6.5f, 8.5f)
                lineTo(10.6f, 7.1f)
                close()
            }
        }
    }

    val Profile: ImageVector by lazy {
        icon("Profile") {
            path(strokeWidth = 2.4f) {
                moveTo(12f, 12f)
                curveTo(9.79f, 12f, 8f, 10.21f, 8f, 8f)
                curveTo(8f, 5.79f, 9.79f, 4f, 12f, 4f)
                curveTo(14.21f, 4f, 16f, 5.79f, 16f, 8f)
                curveTo(16f, 10.21f, 14.21f, 12f, 12f, 12f)
                close()
            }
            path(strokeWidth = 2.4f) {
                moveTo(4.5f, 20f)
                curveTo(4.5f, 15.86f, 7.86f, 12.5f, 12f, 12.5f)
                curveTo(16.14f, 12.5f, 19.5f, 15.86f, 19.5f, 20f)
            }
        }
    }

    private fun icon(
        name: String,
        paths: ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(paths).build()

    private fun ImageVector.Builder.path(
        strokeWidth: Float = 2.1f,
        block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
    ) {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        )
    }
}
