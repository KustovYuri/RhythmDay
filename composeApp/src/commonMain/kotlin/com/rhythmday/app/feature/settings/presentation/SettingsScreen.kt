package com.rhythmday.app.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythmday.app.core.designsystem.RhythmDayPalette

private val CardShape = RoundedCornerShape(8.dp)
private val ControlBackground = Color(0xFFEDF3F5)
private val EnabledControlBackground = Color(0xFFDDEFEA)

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionRequester = rememberReminderPermissionRequester { granted ->
        if (granted) viewModel.enableReminder() else viewModel.showPermissionDenied()
    }

    LaunchedEffect(state.reminderEnabled) {
        if (state.reminderEnabled) permissionRequester.check()
    }

    SettingsScreen(
        state = state,
        contentPadding = contentPadding,
        onBack = onBack,
        onReminderEnabledChange = { enabled ->
            if (enabled) permissionRequester.request() else viewModel.disableReminder()
        },
        onReminderTimeChange = viewModel::setReminderTime,
        onRequestPermission = permissionRequester::request
    )
}

@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit,
    onRequestPermission: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = contentPadding.calculateTopPadding() + 2.dp,
            end = 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsHeader(onBack)
        }
        item {
            ReminderCard(
                state = state,
                onEnabledChange = onReminderEnabledChange,
                onOpenTimePicker = { showTimePicker = true },
                onRequestPermission = onRequestPermission
            )
        }
    }

    if (showTimePicker) {
        ReminderTimeDialog(
            initialHour = state.reminderHour,
            initialMinute = state.reminderMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                onReminderTimeChange(hour, minute)
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CardShape)
                .background(RhythmDayPalette.Panel)
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Назад",
                tint = RhythmDayPalette.Ink,
                modifier = Modifier.size(21.dp)
            )
        }
        Text(
            text = "Настройки",
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Ink,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(42.dp))
    }
}

@Composable
private fun ReminderCard(
    state: SettingsUiState,
    onEnabledChange: (Boolean) -> Unit,
    onOpenTimePicker: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = RhythmDayPalette.Panel
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CardShape)
                        .background(
                            if (state.reminderEnabled) EnabledControlBackground else ControlBackground
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = RhythmDayPalette.TealDark,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Напоминание о записи",
                        style = MaterialTheme.typography.titleMedium,
                        color = RhythmDayPalette.Ink
                    )
                    Text(
                        text = "Каждый день",
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        color = RhythmDayPalette.Muted
                    )
                }
                Switch(
                    checked = state.reminderEnabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RhythmDayPalette.Teal,
                        uncheckedThumbColor = RhythmDayPalette.Muted,
                        uncheckedTrackColor = ControlBackground,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            if (state.reminderEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(CardShape)
                        .background(EnabledControlBackground)
                        .clickable(role = Role.Button, onClick = onOpenTimePicker)
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = RhythmDayPalette.TealDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Время напоминания",
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = RhythmDayPalette.Ink
                    )
                    Text(
                        text = state.reminderTimeLabel(),
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = RhythmDayPalette.TealDark
                    )
                }
            }

            if (state.permissionDenied) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Разрешите уведомления и точные напоминания в настройках устройства",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(CardShape)
                            .background(ControlBackground)
                            .clickable(role = Role.Button, onClick = onRequestPermission)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Разрешить",
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RhythmDayPalette.TealDark
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
            shape = CardShape,
            color = RhythmDayPalette.Panel
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Время напоминания",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    color = RhythmDayPalette.Ink
                )
                TimePicker(
                    state = pickerState,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DialogButton(
                        text = "Отмена",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        background = ControlBackground,
                        contentColor = RhythmDayPalette.Ink
                    )
                    DialogButton(
                        text = "Сохранить",
                        onClick = { onConfirm(pickerState.hour, pickerState.minute) },
                        modifier = Modifier.weight(1f),
                        background = RhythmDayPalette.Teal,
                        contentColor = Color.White,
                        showCheck = true
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    background: Color,
    contentColor: Color,
    showCheck: Boolean = false
) {
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(CardShape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showCheck) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
        }
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

private fun SettingsUiState.reminderTimeLabel(): String =
    "${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')}"
