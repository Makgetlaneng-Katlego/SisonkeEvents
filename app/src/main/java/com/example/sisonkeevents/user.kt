package com.example.sisonkeevents

import com.example.sisonkeevents.data.OfflineDatabaseHelper
import com.example.sisonkeevents.data.PasswordUtils
import com.example.sisonkeevents.data.UserAccountRow

class user {
    var Firstname: String? = null
    var LastName: String? = null
    var email: String? = null
    var PhoneNumber: String? = null
    var password: String? = null // Stores the BCrypt hash
    var authProvider: String = "LOCAL"

    init {
        loadFromDb()
    }

    private fun loadFromDb() {
        val row = OfflineDatabaseHelper.get().loadUserAccount()
        if (row != null) {
            Firstname = row.firstName
            LastName = row.lastName
            email = row.email
            PhoneNumber = row.phone
            password = row.password
            authProvider = row.authProvider
        }
    }

    private fun saveToDb() {
        val row = UserAccountRow(Firstname, LastName, email, PhoneNumber, password, authProvider)
        OfflineDatabaseHelper.get().saveUserAccount(row)
    }
    fun updateUserDetails(
        firstname: String,
        lastname: String,
        email: String,
        phonenumber: String,
        password: String
    ) {
        updateUserDetails(firstname, lastname, email, phonenumber, password, "LOCAL")
    }
    fun updateUserDetails(
        firstname: String,
        lastname: String,
        email: String,
        phonenumber: String,
        password: String,
        authProvider: String
    ) {
        this.Firstname = firstname
        this.LastName = lastname
        this.email = email
        this.PhoneNumber = phonenumber
        this.authProvider = authProvider
        this.password = PasswordUtils.hashPassword(password)

        saveToDb()
    }

    fun updatePassword(newPassword: String) {
        this.password = PasswordUtils.hashPassword(newPassword)

        this.authProvider = "LOCAL"
        saveToDb()
    }

    fun updateEmail(newEmail: String) {
        this.email = newEmail
        saveToDb()
    }

    fun updateProfileDetails(fn: String, ln: String, ph: String) {
        this.Firstname = fn
        this.LastName = ln
        this.PhoneNumber = ph
        saveToDb()
    }

    fun verifyUser(inputEmail: String, inputPassword: String): Boolean {
        if (this.email == inputEmail && authProvider == "LOCAL") {
            val storedHash = this.password ?: return false
            return PasswordUtils.verifyPassword(inputPassword, storedHash)
        }
        return false
    }

    fun verifyPasswords(pw: String, cp: String): Boolean {
        return pw == cp
    }
}