package com.example.engine

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false
        }
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bm = context.getSystemService(BiometricManager::class.java) ?: return false
                bm.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                ) == BiometricManager.BIOMETRIC_SUCCESS
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                val bm = context.getSystemService(BiometricManager::class.java) ?: return false
                @Suppress("DEPRECATION")
                bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
            }
        }.getOrDefault(false)
    }

    /**
     * Launches Android's system Fingerprint / BiometricPrompt on API 28+.
     * Returns true if the biometric prompt was launched, or false if unavailable
     * (in which case caller falls back to the Liquid Glass Nova PIN UI).
     */
    fun promptBiometricUnlock(
        activity: Activity,
        title: String = "Unlock Private Vault",
        subtitle: String = "Verify fingerprint to access your Incognito tabs",
        negativeButtonLabel: String = "Use Nova PIN",
        onSuccess: () -> Unit,
        onUsePinOrCancel: () -> Unit,
        onError: (String) -> Unit = {}
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !isBiometricAvailable(activity)) {
            return false
        }

        return runCatching {
            val executor = ContextCompat.getMainExecutor(activity)
            val cancellationSignal = CancellationSignal()

            val builder = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButton(negativeButtonLabel, executor) { _, _ ->
                    onUsePinOrCancel()
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setConfirmationRequired(false)
            }

            builder.build().authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                            errorCode == 13
                        ) {
                            onUsePinOrCancel()
                        } else {
                            onError(errString?.toString() ?: "Biometric authentication failed")
                            onUsePinOrCancel()
                        }
                    }

                    override fun onAuthenticationFailed() {
                        // System prompt keeps listening until user succeeds or cancels
                    }
                }
            )
            true
        }.getOrDefault(false)
    }
}
