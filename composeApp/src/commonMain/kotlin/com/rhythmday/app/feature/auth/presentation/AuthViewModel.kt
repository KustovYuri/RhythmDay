package com.rhythmday.app.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val repository: AuthRepository
) : ViewModel() {
    val session: StateFlow<AuthSession> = repository.session

    private val mutableOperationState = MutableStateFlow(AuthOperationState())
    val operationState: StateFlow<AuthOperationState> = mutableOperationState.asStateFlow()

    fun signIn(email: String) {
        runOperation { repository.signIn(email) }
    }

    fun register(firstName: String, lastName: String, email: String) {
        runOperation { repository.register(firstName, lastName, email) }
    }

    fun requestPasswordReset() {
        completeLocalAction(AuthAction.PasswordResetRequested)
    }

    fun updateEmail(email: String) {
        runOperation(AuthAction.EmailChanged) { repository.updateEmail(email) }
    }

    fun changePassword() {
        completeLocalAction(AuthAction.PasswordChanged)
    }

    fun signOut() {
        runOperation { repository.signOut() }
    }

    fun clearOperationState() {
        mutableOperationState.value = AuthOperationState()
    }

    private fun completeLocalAction(action: AuthAction) {
        mutableOperationState.value = AuthOperationState(completedAction = action)
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
                .onFailure {
                    mutableOperationState.value = AuthOperationState(
                        errorMessage = "Не удалось сохранить изменения"
                    )
                }
        }
    }
}
