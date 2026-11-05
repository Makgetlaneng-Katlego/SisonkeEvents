package com.example.sisonkeevents.data

import com.google.common.truth.Truth.assertThat
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class PasswordUtilsTest {

    @Test
    fun `hashPassword returns bcrypt-prefixed hash`() {
        val hash = PasswordUtils.hashPassword("SuperSecret123!")
        assertThat(hash).startsWith("$2a$")
    }
    @Test
    fun `hashPassword is not deterministic due to random salt`() {
        val a = PasswordUtils.hashPassword("SamePassword")
        val b = PasswordUtils.hashPassword("SamePassword")
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `verifyPassword returns true for matching password`() {
        val hash = PasswordUtils.hashPassword("CorrectHorseBatteryStaple")
        assertThat(PasswordUtils.verifyPassword("CorrectHorseBatteryStaple", hash)).isTrue()
    }

    @Test
    fun `verifyPassword returns false for wrong password`() {
        val hash = PasswordUtils.hashPassword("CorrectHorseBatteryStaple")
        assertThat(PasswordUtils.verifyPassword("wrongPassword", hash)).isFalse()
    }

    @Test
    fun `verifyPassword returns false for malformed hash`() {
        assertThat(PasswordUtils.verifyPassword("anything", "not-a-bcrypt-hash")).isFalse()
    }
}