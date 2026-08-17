package com.rhythmday.app.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythmday.app.core.designsystem.RhythmDayPalette
import com.rhythmday.app.feature.auth.domain.AuthSession
import com.rhythmday.app.feature.auth.domain.UserProfile

private val ProfileShape = RoundedCornerShape(8.dp)
private val ProfileFieldBackground = Color(0xFFEDF3F5)
private val ProfileAccentBackground = Color(0xFFD2ECE7)
private val LogoutBackground = Color(0xFFF8E9E8)

@Composable
fun ProfileRoute(
    viewModel: AuthViewModel,
    isGuest: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangeEmail: () -> Unit,
    onChangePassword: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenRegistration: () -> Unit
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val user = (session as? AuthSession.Authenticated)?.user
    val showLogout = !isGuest && user != null
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = contentPadding.calculateTopPadding() + 2.dp,
                end = 18.dp,
                bottom = contentPadding.calculateBottomPadding() + if (showLogout) 84.dp else 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ProfileHeader(onBack = onBack) }
            item {
                if (isGuest || user == null) {
                    GuestProfileSummary()
                } else {
                    UserProfileSummary(user)
                }
            }
            if (isGuest || user == null) {
                item {
                    ProfileSection(title = "Аккаунт") {
                        ProfileAction(
                            icon = Icons.AutoMirrored.Outlined.Login,
                            title = "Войти в аккаунт",
                            subtitle = "Вернуться к сохранённому профилю",
                            onClick = onOpenLogin
                        )
                        ProfileAction(
                            icon = Icons.Outlined.PersonAdd,
                            title = "Создать аккаунт",
                            subtitle = "Добавить имя и email",
                            onClick = onOpenRegistration
                        )
                    }
                }
            } else {
                item {
                    ProfileSection(title = "Аккаунт") {
                        ProfileAction(
                            icon = Icons.Outlined.Email,
                            title = "Изменить email",
                            subtitle = user.email,
                            onClick = onChangeEmail
                        )
                        ProfileAction(
                            icon = Icons.Outlined.Lock,
                            title = "Сменить пароль",
                            subtitle = "Обновить данные для входа",
                            onClick = onChangePassword
                        )
                    }
                }
            }
            item {
                ProfileSection(title = "Приложение") {
                    ProfileAction(
                        icon = Icons.Outlined.Settings,
                        title = "Настройки",
                        subtitle = "Уведомления и время напоминания",
                        onClick = onOpenSettings
                    )
                }
            }
            item { LocalDataNotice(isGuest = isGuest || user == null) }
        }

        if (showLogout) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(RhythmDayPalette.Background)
                    .padding(
                        start = 18.dp,
                        top = 8.dp,
                        end = 18.dp,
                        bottom = contentPadding.calculateBottomPadding() + 18.dp
                    )
            ) {
                LogoutButton(onClick = { showLogoutDialog = true })
            }
        }
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                viewModel.signOut()
            }
        )
    }
}

@Composable
fun ChangeEmailRoute(
    viewModel: AuthViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val operation by viewModel.operationState.collectAsStateWithLifecycle()
    val currentEmail = (session as? AuthSession.Authenticated)?.user?.email.orEmpty()
    var email by remember(currentEmail) { mutableStateOf(currentEmail) }
    var submitted by remember { mutableStateOf(false) }
    val completed = operation.completedAction == AuthAction.EmailChanged

    LaunchedEffect(Unit) { viewModel.clearOperationState() }

    AccountFormPage(contentPadding = contentPadding, onBack = onBack, title = "Изменить email") {
        if (completed) {
            AuthSuccess(
                title = "Email изменён",
                text = "Теперь в профиле используется ${email.trim().lowercase()}.",
                buttonText = "Вернуться в профиль",
                onClick = onBack
            )
        } else {
            Text(
                text = "Новый адрес будет использоваться для входа после подключения сервера.",
                style = MaterialTheme.typography.bodyLarge,
                color = RhythmDayPalette.Muted
            )
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Новый email",
                icon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                error = when {
                    !submitted -> null
                    email.trim().equals(currentEmail, ignoreCase = true) -> "Укажите другой email"
                    else -> emailError(email)
                }
            )
            operation.errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            PrimaryAuthButton(
                text = "Сохранить email",
                isBusy = operation.isBusy,
                onClick = {
                    submitted = true
                    if (
                        emailError(email) == null &&
                        !email.trim().equals(currentEmail, ignoreCase = true)
                    ) {
                        viewModel.updateEmail(email)
                    }
                }
            )
        }
    }
}

@Composable
fun ChangePasswordRoute(
    viewModel: AuthViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val operation by viewModel.operationState.collectAsStateWithLifecycle()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val completed = operation.completedAction == AuthAction.PasswordChanged

    LaunchedEffect(Unit) { viewModel.clearOperationState() }

    AccountFormPage(contentPadding = contentPadding, onBack = onBack, title = "Смена пароля") {
        if (completed) {
            AuthSuccess(
                title = "Пароль обновлён",
                text = "Изменение сохранено локально. Серверная проверка будет подключена позднее.",
                buttonText = "Вернуться в профиль",
                onClick = onBack
            )
        } else {
            Text(
                text = "Используйте не менее 8 символов, добавьте буквы и цифры.",
                style = MaterialTheme.typography.bodyLarge,
                color = RhythmDayPalette.Muted
            )
            AuthTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = "Текущий пароль",
                icon = Icons.Outlined.Lock,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isPassword = true,
                error = passwordError(currentPassword).takeIf { submitted }
            )
            AuthTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "Новый пароль",
                icon = Icons.Outlined.Lock,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isPassword = true,
                error = when {
                    !submitted -> null
                    newPassword == currentPassword -> "Новый пароль должен отличаться"
                    else -> passwordError(newPassword)
                }
            )
            AuthTextField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = "Повторите новый пароль",
                icon = Icons.Outlined.Lock,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isPassword = true,
                error = confirmationError(newPassword, confirmation).takeIf { submitted }
            )
            PrimaryAuthButton(
                text = "Сменить пароль",
                isBusy = operation.isBusy,
                onClick = {
                    submitted = true
                    val valid = passwordError(currentPassword) == null &&
                        passwordError(newPassword) == null &&
                        newPassword != currentPassword &&
                        confirmationError(newPassword, confirmation) == null
                    if (valid) viewModel.changePassword()
                }
            )
        }
    }
}

@Composable
private fun ProfileHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileBackButton(onBack)
        Text(
            text = "Профиль",
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
private fun UserProfileSummary(user: UserProfile) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = ProfileShape, color = RhythmDayPalette.Panel) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileAvatar(user.initials)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.titleLarge,
                    color = RhythmDayPalette.Ink
                )
                Text(
                    text = user.email,
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RhythmDayPalette.Muted
                )
                Text(
                    text = "Локальный профиль",
                    modifier = Modifier.padding(top = 7.dp),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RhythmDayPalette.TealDark
                )
            }
        }
    }
}

@Composable
private fun GuestProfileSummary() {
    Surface(modifier = Modifier.fillMaxWidth(), shape = ProfileShape, color = RhythmDayPalette.Panel) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileAvatar("Г")
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Гостевой режим",
                    style = MaterialTheme.typography.titleLarge,
                    color = RhythmDayPalette.Ink
                )
                Text(
                    text = "Можно пользоваться дневником без аккаунта",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RhythmDayPalette.Muted
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(initials: String) {
    Box(
        modifier = Modifier.size(58.dp).clip(ProfileShape).background(ProfileAccentBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Black,
            color = RhythmDayPalette.TealDark
        )
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 2.dp),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.Muted
        )
        content()
    }
}

@Composable
private fun ProfileAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ProfileShape)
            .background(ProfileFieldBackground)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(ProfileShape).background(RhythmDayPalette.Panel),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = RhythmDayPalette.TealDark, modifier = Modifier.size(19.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = RhythmDayPalette.Ink
            )
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = RhythmDayPalette.Muted
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = RhythmDayPalette.TealDark,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun LocalDataNotice(isGuest: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(ProfileShape).background(RhythmDayPalette.Panel).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = RhythmDayPalette.TealDark,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = if (isGuest) {
                "Дневниковые записи сохраняются только на этом устройстве. Вход в аккаунт не удалит их."
            } else {
                "Пока профиль и дневниковые записи хранятся на устройстве. Синхронизация появится вместе с сервером."
            },
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = RhythmDayPalette.Muted
        )
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(ProfileShape)
            .background(LogoutBackground)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Logout,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(7.dp))
        Text(
            text = "Выйти из профиля",
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun LogoutDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 330.dp),
            shape = ProfileShape,
            color = RhythmDayPalette.Panel
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Выйти из профиля?",
                    style = MaterialTheme.typography.titleLarge,
                    color = RhythmDayPalette.Ink
                )
                Text(
                    text = "Записи останутся на устройстве. Для возврата потребуется снова войти.",
                    modifier = Modifier.padding(top = 7.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RhythmDayPalette.Muted
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DialogAction(
                        text = "Отмена",
                        background = ProfileFieldBackground,
                        contentColor = RhythmDayPalette.Ink,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    DialogAction(
                        text = "Выйти",
                        background = LogoutBackground,
                        contentColor = MaterialTheme.colorScheme.error,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogAction(
    text: String,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier.height(42.dp).clip(ProfileShape).background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = contentColor)
    }
}

@Composable
private fun AccountFormPage(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = contentPadding.calculateTopPadding() + 2.dp,
            end = 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileBackButton(onBack)
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        fontSize = 20.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = RhythmDayPalette.Ink,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(42.dp))
                }
                content()
            }
        }
    }
}

@Composable
private fun ProfileBackButton(onBack: () -> Unit) {
    Box(
        modifier = Modifier.size(42.dp).clip(ProfileShape).background(RhythmDayPalette.Panel)
            .clickable(role = Role.Button, onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Назад",
            tint = RhythmDayPalette.Ink,
            modifier = Modifier.size(20.dp)
        )
    }
}
