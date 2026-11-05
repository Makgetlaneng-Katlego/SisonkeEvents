package com.example.sisonkeevents.sso

import android.content.ContentValues
import com.example.sisonkeevents.data.OfflineDatabaseHelper


class SSODataStore {

    private val db
        get() = OfflineDatabaseHelper.get().writableDatabase

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