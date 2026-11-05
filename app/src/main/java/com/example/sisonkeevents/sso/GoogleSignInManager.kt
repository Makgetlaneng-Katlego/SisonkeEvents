package com.example.sisonkeevents.sso

import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.SecureRandom

/**
 * Wraps Google's Credential Manager API behind a coroutine-friendly API.
 *
 * Flow:
 *   1. Generate a secure nonce (anti-replay).
 *   2. Ask Credential Manager for a Google ID token.
 *   3. Google shows the account picker (or silently re-signs in).
 *   4. Parse the ID token payload to extract sub / email / name.
 *
 * Never throws for expected failures — returns a sealed-class result.
 */
class GoogleSignInManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): GoogleSignInResult = withContext(Dispatchers.Main) {
        val nonce = generateSecureNonce()

        val googleIdOption = GetGoogleIdOption.Builder()
            // Required: server client ID (the *Web* OAuth client ID). Without this,
            // GetGoogleIdOption.Builder.build() throws IllegalArgumentException.
            .setServerClientId(SSOConfig.WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val response: GetCredentialResponse = credentialManager.getCredential(
                context = context,
                request = request
            )
            parseResponse(response)
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Error(e.message ?: "Sign-in was cancelled.")
        } catch (t: Throwable) {
            GoogleSignInResult.Error(t.message ?: "Unexpected sign-in error.")
        }
    }

    private fun parseResponse(response: GetCredentialResponse): GoogleSignInResult {
        val credential = response.credential

        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleSignInResult.Error("Unexpected credential type: ${credential.type}")
        }

        return try {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleCredential.idToken
            val profile = parseIdTokenPayload(idToken)
                ?: return GoogleSignInResult.Error("Could not parse ID token.")

            GoogleSignInResult.Success(profile = profile, idToken = idToken)
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Error("Invalid Google ID token response.")
        } catch (t: Throwable) {
            GoogleSignInResult.Error(t.message ?: "Failed to read credential.")
        }
    }

    private fun parseIdTokenPayload(idToken: String): SSOUserProfile? {
        return try {
            val parts = idToken.split(".")
            if (parts.size < 2) return null

            val payloadJson = String(
                Base64.decode(
                    parts[1],
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                )
            )

            val json = JSONObject(payloadJson)
            val sub = json.optString("sub").takeIf { it.isNotBlank() } ?: return null

            SSOUserProfile(
                subject = sub,
                email = json.optString("email").takeIf { it.isNotBlank() },
                displayName = json.optString("name").takeIf { it.isNotBlank() },
                provider = SSOConfig.PROVIDER
            )
        } catch (t: Throwable) {
            null
        }
    }

    private fun generateSecureNonce(byteLength: Int = 32): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
    }
}

sealed interface GoogleSignInResult {
    data class Success(
        val profile: SSOUserProfile,
        val idToken: String
    ) : GoogleSignInResult

    data class Error(val message: String) : GoogleSignInResult
}