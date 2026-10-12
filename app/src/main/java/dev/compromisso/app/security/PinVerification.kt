package dev.compromisso.app.security

sealed interface PinVerification {
    data object Success : PinVerification
    data object Invalid : PinVerification
    data object NotConfigured : PinVerification
    data class Locked(val remainingMs: Long) : PinVerification
    data object CredentialUnavailable : PinVerification
}
