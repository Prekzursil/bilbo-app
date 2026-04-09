package dev.bilbo.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Manages Supabase Auth state for the Bilbo social layer.
 *
 * Auth is *lazy* — the user is never prompted to log in until they explicitly tap
 * "Find a Focus Buddy" (or any other social entry point). Before that point,
 * [authState] is [AuthState.Unauthenticated] and the app works fully offline.
 *
 * Token refresh is handled transparently by the Supabase SDK.
 *
 * All methods are suspending — call them from a coroutine / viewModelScope.
 */
class AuthManager(
    private val supabaseClient: SupabaseClient,
) {

    // ── Auth state ────────────────────────────────────────────────────────────

    sealed class AuthState {
        /** No user signed in. Social features unavailable. */
        data object Unauthenticated : AuthState()

        /** Sign-in (or token refresh) is in progress. */
        data object Loading : AuthState()

        /** A user is signed in and the session is valid. */
        data class Authenticated(val user: UserInfo) : AuthState()

        /** An error occurred during sign-in or token refresh. */
        data class Error(val message: String) : AuthState()
    }

    sealed class AuthResult {
        data object Success : AuthResult()
        data class Failure(val message: String) : AuthResult()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    /** Observable auth state. Collect in UI to react to sign-in/sign-out changes. */
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** True when a user is currently authenticated. */
    val isAuthenticated: Boolean
        get() = _authState.value is AuthState.Authenticated

    /** The current user, or null if not authenticated. */
    val currentUser: UserInfo?
        get() = (_authState.value as? AuthState.Authenticated)?.user

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Restores any existing session from the Supabase SDK's persisted token store.
     * Call this at app start (or when the social layer is first loaded).
     */
    suspend fun restoreSession() {
        _authState.value = AuthState.Loading
        runCatching {
            supabaseClient.auth.awaitInitialization()
            val session = supabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                val user = supabaseClient.auth.retrieveUserForCurrentSession(updateSession = true)
                _authState.value = AuthState.Authenticated(user)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }.onFailure { e ->
            _authState.value = AuthState.Error(e.message ?: "Failed to restore session")
        }
    }

    // ── Sign-in methods ───────────────────────────────────────────────────────

    /**
     * Signs in with email and password.
     *
     * On success, [authState] transitions to [AuthState.Authenticated].
     * On failure, [authState] transitions to [AuthState.Error] and [AuthResult.Failure] is returned.
     *
     * This method is called *lazily* — only when the user initiates a social action.
     */
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        _authState.value = AuthState.Loading
        return runCatching {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val user = supabaseClient.auth.retrieveUserForCurrentSession(updateSession = true)
            _authState.value = AuthState.Authenticated(user)
            AuthResult.Success
        }.getOrElse { e ->
            val msg = e.message ?: "Sign-in failed"
            _authState.value = AuthState.Error(msg)
            AuthResult.Failure(msg)
        }
    }

    /**
     * Signs up a new user with email and password.
     *
     * After sign-up, the user typically needs to confirm their email before the session
     * becomes valid. Until then, [authState] stays [AuthState.Unauthenticated].
     */
    suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        _authState.value = AuthState.Loading
        return runCatching {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            // After sign-up, session may not be active yet (email confirmation required)
            val session = supabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                val user = supabaseClient.auth.retrieveUserForCurrentSession(updateSession = true)
                _authState.value = AuthState.Authenticated(user)
            } else {
                _authState.value = AuthState.Unauthenticated  // awaiting email confirmation
            }
            AuthResult.Success
        }.getOrElse { e ->
            val msg = e.message ?: "Sign-up failed"
            _authState.value = AuthState.Error(msg)
            AuthResult.Failure(msg)
        }
    }

    /**
     * Initiates Google OAuth sign-in.
     *
     * On Android/iOS the OAuth flow opens a browser tab; the result is handled by the
     * platform-specific deep-link callback which should call [handleOAuthCallback].
     */
    suspend fun signInWithGoogle(): AuthResult {
        _authState.value = AuthState.Loading
        return runCatching {
            supabaseClient.auth.signInWith(Google)
            // The actual session is set via the OAuth callback; the state will be
            // updated when [handleOAuthCallback] is called.
            AuthResult.Success
        }.getOrElse { e ->
            val msg = e.message ?: "Google sign-in failed"
            _authState.value = AuthState.Error(msg)
            AuthResult.Failure(msg)
        }
    }

    /**
     * Handles the deep-link callback after an OAuth redirect.
     * Call this from the platform's URL handler (Activity.onNewIntent on Android,
     * Scene.openURL on iOS).
     *
     * @param url The full redirect URL containing the OAuth code/tokens.
     */
    suspend fun handleOAuthCallback(url: String): AuthResult {
        return runCatching {
            supabaseClient.auth.importAuthToken(url)
            val user = supabaseClient.auth.retrieveUserForCurrentSession(updateSession = true)
            _authState.value = AuthState.Authenticated(user)
            AuthResult.Success
        }.getOrElse { e ->
            val msg = e.message ?: "OAuth callback handling failed"
            _authState.value = AuthState.Error(msg)
            AuthResult.Failure(msg)
        }
    }

    // ── Sign-out ──────────────────────────────────────────────────────────────

    /**
     * Signs out the current user and clears the local session.
     * [authState] transitions to [AuthState.Unauthenticated].
     */
    suspend fun signOut() {
        runCatching {
            supabaseClient.auth.signOut()
        }
        _authState.value = AuthState.Unauthenticated
    }

    // ── Token refresh ─────────────────────────────────────────────────────────

    /**
     * Explicitly refreshes the access token.
     * Normally not needed — the Supabase SDK refreshes tokens automatically.
     * Call if you receive a 401 from a Supabase edge function and want to retry.
     */
    suspend fun refreshToken(): AuthResult {
        return runCatching {
            supabaseClient.auth.refreshCurrentSession()
            val user = supabaseClient.auth.retrieveUserForCurrentSession(updateSession = false)
            _authState.value = AuthState.Authenticated(user)
            AuthResult.Success
        }.getOrElse { e ->
            val msg = e.message ?: "Token refresh failed"
            _authState.value = AuthState.Error(msg)
            AuthResult.Failure(msg)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the current JWT access token, or null if unauthenticated.
     * Used to set the `Authorization: Bearer …` header on manual HTTP calls.
     */
    suspend fun getAccessToken(): String? =
        supabaseClient.auth.currentSessionOrNull()?.accessToken

    /**
     * Returns the current user's UUID (Supabase `auth.users.id`), or null.
     */
    val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    /**
     * Clears any error state, e.g. after the user dismisses an error dialog.
     */
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }
}
