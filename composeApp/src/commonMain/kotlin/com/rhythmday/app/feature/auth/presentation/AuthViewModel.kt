package com.rhythmday.app.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmday.app.core.presentation.UserMessageManager
import com.rhythmday.app.core.presentation.toRussianUserMessage
import com.rhythmday.app.feature.auth.domain.AuthRepository
import com.rhythmday.app.feature.auth.domain.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthAction {
    PasswordResetRequested,
    EmailChanged,
    PasswordChanged
}

data class AuthOperationState(
    val isBusy: Boolean = false,
    val completedAction: AuthAction? = null,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val messages: UserMessageManager
) : ViewModel() {
    val session: StateFlow<AuthSession> = repository.session

    private val mutableOperationState = MutableStateFlow(AuthOperationState())
    val operationState: StateFlow<AuthOperationState> = mutableOperationState.asStateFlow()

    fun signIn(email: String, password: String) {
        runOperation { repository.signIn(email, password) }
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        passwordConfirmation: String,
        timezone: String
    ) {
        runOperation {
            repository.register(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                passwordConfirmation = passwordConfirmation,
                timezone = timezone
            )
        }
    }

    fun requestPasswordReset(email: String) {
        runOperation(AuthAction.PasswordResetRequested) {
            repository.requestPasswordReset(email)
        }
    }

    fun updateEmail(email: String) {
        runOperation(AuthAction.EmailChanged) { repository.updateEmail(email) }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        newPasswordConfirmation: String
    ) {
        runOperation(AuthAction.PasswordChanged) {
            repository.changePassword(currentPassword, newPassword, newPasswordConfirmation)
        }
    }

    fun signOut() {
        runOperation { repository.signOut() }
    }

    fun clearOperationState() {
        mutableOperationState.value = AuthOperationState()
    }

    private fun runOperation(
        completedAction: AuthAction? = null,
        operation: suspend () -> Unit
    ) {
        if (mutableOperationState.value.isBusy) return

        viewModelScope.launch {
            mutableOperationState.value = AuthOperationState(isBusy = true)
            runCatching { operation() }
                .onSuccess {
                    mutableOperationState.value = AuthOperationState(
                        completedAction = completedAction
                    )
                }
                .onFailure { error ->
                    val message = error.toRussianUserMessage(
                        fallback = "Не удалось подключиться к серверу"
                    )
                    messages.show(message)
                    mutableOperationState.value = AuthOperationState(
                        errorMessage = message
                    )
                }
        }
    }
}
