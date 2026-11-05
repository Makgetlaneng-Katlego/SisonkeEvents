package com.example.sisonkeevents.sso


data class SSOUserProfile(
    val subject: String,
    val email: String?,
    val displayName: String?,
    val provider: String = SSOConfig.PROVIDER
)