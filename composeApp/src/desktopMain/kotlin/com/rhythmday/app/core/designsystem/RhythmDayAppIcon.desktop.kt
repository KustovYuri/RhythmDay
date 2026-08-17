package com.rhythmday.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
actual fun RhythmDayAppIcon(modifier: Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(RhythmDayPalette.Teal),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = RhythmDayIcons.Calendar,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(34.dp)
        )
    }
}
