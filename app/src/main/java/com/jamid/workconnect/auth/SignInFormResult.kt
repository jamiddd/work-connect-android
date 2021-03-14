package com.jamid.workconnect.auth

data class SignInFormResult(
    var isValid: Boolean = false,
    var emailError: String? = null,
    var passwordError: String? = null
)