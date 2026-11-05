package com.example.sisonkeevents.data

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordUtils {
    private const val COST = 12

    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(COST, password.toCharArray())
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        return try {
            BCrypt.verifyer().verify(password.toCharArray(), storedHash).verified
        } catch (e: Exception) {
            false
        }
    }
}