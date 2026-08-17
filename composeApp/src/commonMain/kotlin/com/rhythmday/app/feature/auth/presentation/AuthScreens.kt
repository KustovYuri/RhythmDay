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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythmday.app.core.designsystem.RhythmDayAppIcon
import com.rhythmday.app.core.designsystem.RhythmDayPalette

enum class AuthStartDestination {
    Login,
    Registration
}

private enum class AuthDestination {
    Login,
    Registration,
    PasswordRecovery
}

private val AuthShape = RoundedCornerShape(8.dp)
private val AuthFieldBackground = Color(0xFFEDF3F5)
private val AuthAccentBackground = Color(0xFFDDEFEA)
private val AuthErrorBackground = Color(0xFFF8E9E8)

@Composable
fun AuthFlow(
    viewModel: AuthViewModel,
    initialDestination: AuthStartDestination,
    onContinueAsGuest: () -> Unit
) {
    var destination by remember(initialDestination) {
        mutableStateOf(
            if (initialDestination == AuthStartDestination.Registration) {
                AuthDestination.Registration
            } else {
                AuthDestination.Login
            }
        )
    }

    LaunchedEffect(destination) {
        viewModel.clearOperationState()
    }

    when (destination) {
        AuthDestination.Login -> LoginScreen(
            viewModel = viewModel,
            onOpenRegistration = { destination = AuthDestination.Registration },
            onOpenPasswordRecovery = { destination = AuthDestination.PasswordRecovery },
            onContinueAsGuest = onContinueAsGuest
        )

        AuthDestination.Registration -> RegistrationScreen(
            viewModel = viewModel,
            onBack = { destination = AuthDestination.Login }
        )

        AuthDestination.PasswordRecovery -> PasswordRecoveryScreen(
            viewModel = viewModel,
            onBack = { destination = AuthDestination.Login }
        )
    }
}

@Composable
fun AuthLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RhythmDayPalette.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RhythmDayAppIcon(modifier = Modifier.size(64.dp))
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 20.dp).size(24.dp),
                color = RhythmDayPalette.Teal,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun LoginScreen(
    viewModel: AuthViewModel,
    onOpenRegistration: () -> Unit,
    onOpenPasswordRecovery: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val operation by viewModel.operationState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val currentEmailError = emailError(email).takeIf { submitted }
    val currentPasswordError = passwordError(password).takeIf { submitted }

    LoginAuthPage(topContent = { BrandHeader() }) {
        AuthSubtitle("Войдите, чтобы продолжить вести дневник")
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            icon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            error = currentEmailError
        )
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Пароль",
            icon = Icons.Outlined.Lock,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isPassword = true,
            error = currentPasswordError
        )
        Text(
            text = "Забыли пароль?",
            modifier = Modifier
                .align(Alignment.End)
                .clip(AuthShape)
                .clickable(role = Role.Button, onClick = onOpenPasswordRecovery)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.TealDark
        )
        operation.errorMessage?.let { message -> AuthError(message) }
        GuestEntry(onClick = onContinueAsGuest)
        PrimaryAuthButton(
            text = "Войти",
            isBusy = operation.isBusy,
            onClick = {
                submitted = true
                if (emailError(email) == null && passwordError(password) == null) {
                    viewModel.signIn(email)
                }
            }
        )
        AuthSwitchRow(
            prefix = "Нет аккаунта?",
            action = "Зарегистрироваться",
            onClick = onOpenRegistration
        )
    }
}

@Composable
private fun RegistrationScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val operation by viewModel.operationState.collectAsStateWithLifecycle()
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    AuthPage(
        topContent = { AuthBackHeader(title = "Создать аккаунт", onBack = onBack) },
        contentTopPadding = 28.dp,
        centerContent = false,
        footerContent = {
            operation.errorMessage?.let { message -> AuthError(message) }
            PrimaryAuthButton(
                text = "Создать аккаунт",
                isBusy = operation.isBusy,
                onClick = {
                    submitted = true
                    val valid = listOf(
                        nameError(firstName),
                        nameError(lastName),
                        emailError(email),
                        passwordError(password),
                        confirmationError(password, confirmation)
                    ).all { error -> error == null }
                    if (valid) viewModel.register(firstName, lastName, email)
                }
            )
            AuthSwitchRow(
                prefix = "Уже есть аккаунт?",
                action = "Войти",
                onClick = onBack
            )
        }
    ) {
        AuthTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = "Имя",
            icon = Icons.Outlined.Person,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            error = nameError(firstName).takeIf { submitted }
        )
        AuthTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = "Фамилия",
            icon = Icons.Outlined.Person,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            error = nameError(lastName).takeIf { submitted }
        )
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            icon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            error = emailError(email).takeIf { submitted }
        )
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Пароль",
            icon = Icons.Outlined.Lock,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isPassword = true,
            error = passwordError(password).takeIf { submitted }
        )
        AuthTextField(
            value = confirmation,
            onValueChange = { confirmation = it },
            label = "Повторите пароль",
            icon = Icons.Outlined.Lock,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isPassword = true,
            error = confirmationError(password, confirmation).takeIf { submitted }
        )
    }
}

@Composable
private fun PasswordRecoveryScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val operation by viewModel.operationState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val completed = operation.completedAction == AuthAction.PasswordResetRequested

    AuthPage(
        topContent = {
            AuthBackHeader(
                title = if (completed) "Проверьте почту" else "Восстановление пароля",
                onBack = onBack
            )
        },
        contentTopPadding = 28.dp,
        centerContent = false
    ) {
        if (completed) {
            AuthSuccess(
                title = "Проверьте почту",
                text = "Мы подготовили запрос на восстановление для ${email.trim()}. После подключения сервера сюда придёт письмо со ссылкой.",
                buttonText = "Вернуться ко входу",
                onClick = onBack,
                showTitle = false
            )
        } else {
            AuthSubtitle("Укажите email, который используется для входа")
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                icon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                error = emailError(email).takeIf { submitted }
            )
            PrimaryAuthButton(
                text = "Продолжить",
                isBusy = operation.isBusy,
                onClick = {
                    submitted = true
                    if (emailError(email) == null) viewModel.requestPasswordReset()
                }
            )
        }
    }
}

@Composable
private fun AuthPage(
    topContent: @Composable () -> Unit,
    contentTopPadding: Dp = 12.dp,
    centerContent: Boolean = true,
    footerContent: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RhythmDayPalette.Background)
            .statusBarsPadding()
            .imePadding()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 18.dp, end = 20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
                topContent()
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = contentTopPadding,
                end = 20.dp,
                bottom = 28.dp
            ),
            verticalArrangement = if (centerContent) Arrangement.Center else Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content
                )
            }
        }
        footerContent?.let { footer ->
            Box(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = footer
                )
            }
        }
    }
}

@Composable
private fun LoginAuthPage(
    topContent: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RhythmDayPalette.Background)
            .statusBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
                topContent()
            }
        }
        item {
            Column(
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

@Composable
private fun BrandHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RhythmDayAppIcon(modifier = Modifier.size(92.dp))
        Text(
            text = "Ритм дня",
            modifier = Modifier.padding(top = 10.dp),
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Black,
            color = RhythmDayPalette.Ink
        )
    }
}

@Composable
private fun AuthBackHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(AuthShape)
                .background(AuthFieldBackground)
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
}

@Composable
private fun AuthSubtitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = RhythmDayPalette.Muted,
        textAlign = TextAlign.Center
    )
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardOptions: KeyboardOptions,
    isPassword: Boolean = false,
    error: String? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        },
        trailingIcon = if (isPassword) {
            {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Outlined.VisibilityOff
                    } else {
                        Icons.Outlined.Visibility
                    },
                    contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AuthShape)
                        .clickable(role = Role.Button) { passwordVisible = !passwordVisible }
                        .padding(10.dp)
                )
            }
        } else {
            null
        },
        supportingText = error?.let { message -> { Text(message) } },
        isError = error != null,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        shape = AuthShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = AuthFieldBackground,
            unfocusedContainerColor = AuthFieldBackground,
            disabledContainerColor = AuthFieldBackground,
            errorContainerColor = AuthErrorBackground,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedLeadingIconColor = RhythmDayPalette.TealDark,
            unfocusedLeadingIconColor = RhythmDayPalette.Muted,
            focusedLabelColor = RhythmDayPalette.TealDark
        )
    )
}

@Composable
internal fun PrimaryAuthButton(
    text: String,
    isBusy: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(AuthShape)
            .background(if (isBusy) RhythmDayPalette.Teal.copy(alpha = 0.62f) else RhythmDayPalette.Teal)
            .clickable(enabled = !isBusy, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(19.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun AuthSwitchRow(prefix: String, action: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(prefix, fontSize = 13.sp, color = RhythmDayPalette.Muted)
        Text(
            text = action,
            modifier = Modifier
                .clip(AuthShape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = RhythmDayPalette.TealDark
        )
    }
}

@Composable
private fun GuestEntry(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = AuthShape,
        color = RhythmDayPalette.Panel
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Продолжить без входа",
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = RhythmDayPalette.Ink
                )
                Text(
                    text = "Записи останутся на этом устройстве",
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
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AuthError(message: String) {
    Text(
        text = message,
        modifier = Modifier.fillMaxWidth().clip(AuthShape).background(AuthErrorBackground).padding(10.dp),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
internal fun AuthSuccess(
    title: String,
    text: String,
    buttonText: String,
    onClick: () -> Unit,
    showTitle: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(58.dp).clip(AuthShape).background(AuthAccentBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = RhythmDayPalette.TealDark,
                modifier = Modifier.size(30.dp)
            )
        }
        if (showTitle) {
            Text(
                text = title,
                modifier = Modifier.padding(top = 18.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = RhythmDayPalette.Ink,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = text,
            modifier = Modifier.padding(
                top = if (showTitle) 8.dp else 18.dp,
                bottom = 22.dp
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = RhythmDayPalette.Muted,
            textAlign = TextAlign.Center
        )
        PrimaryAuthButton(text = buttonText, isBusy = false, onClick = onClick)
    }
}
