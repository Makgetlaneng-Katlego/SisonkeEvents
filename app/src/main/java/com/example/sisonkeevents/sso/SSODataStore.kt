package com.example.sisonkeevents.sso

import android.content.ContentValues
import com.example.sisonkeevents.data.OfflineDatabaseHelper

/**
 * Reads and writes SSO identity links against the app's SQLite database.
 *
 * Note: `user_account` is a SINGLE-ROW table (CHECK id = 1). Every Google
 * identity that signs in on this device is linked to `local_user_id = 1`.
 *
 * The schema (from OfflineDatabaseHelper v4):
 *   user_identities(
 *     id, local_user_id, provider, provider_user_id,
 *     email, display_name, created_at
 *   )
 *
 * All methods are safe to call from a background thread (they perform
 * blocking SQLite I/O). Reads return null on failure rather than throwing.
 */
class SSODataStore {

    private val db
        get() = OfflineDatabaseHelper.get().writableDatabase

    /**
     * Returns `user_account.id` (always 1) if the stored user's email
     * matches [email] case-insensitively, otherwise null.
     */
    fun findUserAccountIdByEmail(email: String): Long? {
        return try {
            db.query(
                "user_account",
                arrayOf("id"),
                "email = ? COLLATE NOCASE",
                arrayOf(email),
                null, null, null
            ).use { c ->
                if (c.moveToFirst()) c.getLong(0) else null
            }
        } catch (t: Throwable) {
            android.util.Log.w("SSODataStore", "findUserAccountIdByEmail failed", t)
            null
        }
    }

    /**
     * Returns the linked `local_user_id` for ([provider], [subject]), or null
     * if this Google account has never been linked on this device.
     *
     * The [subject] is the Google `sub` claim — stable across email changes,
     * unlike the email address.
     */
    fun findLinkedUserAccountId(
        subject: String,
        provider: String = SSOConfig.PROVIDER
    ): Long? {
        return try {
            db.query(
                "user_identities",
                arrayOf("local_user_id"),
                "provider = ? AND provider_user_id = ?",
                arrayOf(provider, subject),
                null, null, null
            ).use { c ->
                if (c.moveToFirst()) c.getLong(0) else null
            }
        } catch (t: Throwable) {
            android.util.Log.w("SSODataStore", "findLinkedUserAccountId failed", t)
            null
        }
    }

    /**
     * Inserts a new ([provider], [subject]) → [localUserId] mapping, or updates
     * the cached profile fields if it already exists. Safe to call on every
     * sign-in.
     *
     * @return true if the row was written successfully, false on any DB error.
     *         Callers should treat false as a hard failure (do not mark the
     *         user logged in without an identity link).
     */
    fun linkOrUpdateIdentity(
        subject: String,
        email: String?,
        displayName: String?,
        localUserId: Long,
        provider: String = SSOConfig.PROVIDER
    ): Boolean {
        return try {
            val updated = db.update(
                "user_identities",
                ContentValues().apply {
                    put("email", email)
                    put("display_name", displayName)
                    put("local_user_id", localUserId)
                },
                "provider = ? AND provider_user_id = ?",
                arrayOf(provider, subject)
            )
            if (updated > 0) return true

            val id = db.insert(
                "user_identities",
                null,
                ContentValues().apply {
                    put("local_user_id", localUserId)
                    put("provider", provider)
                    put("provider_user_id", subject)
                    put("email", email)
                    put("display_name", displayName)
                    put("created_at", System.currentTimeMillis() / 1000)
                }
            )
            id != -1L
        } catch (t: Throwable) {
            android.util.Log.w("SSODataStore", "linkOrUpdateIdentity failed", t)
            false
        }
    }

    /**
     * Deletes all SSO identity links and any session rows.
     * Called from the logout button in MainActivity.
     *
     * The Google account itself stays cached on the device — the next
     * sign-in is still one tap. Only the app-side link is removed.
     */
    fun clearAllSessions() {
        try {
            db.delete("user_identities", null, null)
        } catch (t: Throwable) {
            android.util.Log.w("SSODataStore", "clearAllSessions: user_identities delete failed", t)
        }
        try {
            db.delete("auth_sessions", null, null)
        } catch (t: Throwable) {
            // Table may not exist on older installs that never upgraded
            android.util.Log.w("SSODataStore", "clearAllSessions: auth_sessions delete failed", t)
        }
    }
}