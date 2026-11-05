package com.example.sisonkeevents.sso

object SSOConfig {

    /**
     * OAuth 2.0 Web application client ID (Google Cloud Console → Credentials).
     *
     * This is the "Web application" client, NOT the Android client. The Android
     * client is only used by Play Services to verify the app's identity and is
     * not referenced in code. Credential Manager uses this value as the token
     * audience (`setServerClientId`), even though there is no redirect URI.
     *
     * Must be exactly the Client ID string as shown in the console — the ID
     * already ends in `.apps.googleusercontent.com`; do not append it again.
     */
    const val WEB_CLIENT_ID = "803697142492-2tn4783mr3ejnu9hcmsverb3kjgqval6.apps.googleusercontent.com"

    /** Stable provider key used in the `user_identities` table. */
    const val PROVIDER = "google"
}