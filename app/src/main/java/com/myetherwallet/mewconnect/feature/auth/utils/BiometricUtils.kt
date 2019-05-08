package com.myetherwallet.mewconnect.feature.auth.utils

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.text.TextUtils
import androidx.biometric.BiometricPrompt
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.fragment.app.FragmentActivity
import com.myetherwallet.mewconnect.content.data.Network
import com.myetherwallet.mewconnect.core.persist.prefenreces.KeyStore
import com.myetherwallet.mewconnect.core.persist.prefenreces.PreferencesManager
import com.myetherwallet.mewconnect.core.utils.MewLog
import com.myetherwallet.mewconnect.core.utils.crypto.keystore.BiometricKeystoreHelper
import java.util.concurrent.Executors
import javax.crypto.Cipher

/**
 * Created by BArtWell on 15.04.2019.
 */

private const val TAG = "BiometricUtils"

object BiometricUtils {

    // TODO: migrate to biometric hardware checkout
    fun isAvailable(context: Context) = FingerprintManagerCompat.from(context).isHardwareDetected

    fun authenticate(activity: FragmentActivity, successCallback: (cipher: Cipher?) -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                MewLog.d(TAG, "onAuthenticationSucceeded")
                successCallback(result.cryptoObject?.cipher)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                MewLog.d(TAG, "onAuthenticationFailed")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                MewLog.d(TAG, "onAuthenticationError: errorCode=$errorCode; errString=$errString")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Set the title to display.")
                .setSubtitle("Set the subtitle to display.")
                .setDescription("Set the description to display")
                .setNegativeButtonText("Negative Button")
                .build()
        val cryptoObject = BiometricPrompt.CryptoObject(BiometricKeystoreHelper(activity).getDecryptCipher())
        val biometricPrompt = BiometricPrompt(activity, Executors.newSingleThreadExecutor(), callback)
        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }

    fun isEnabled(context: Context, preferences: PreferencesManager): Boolean {
        val isEnabled = !TextUtils.isEmpty(preferences.applicationPreferences.getWalletMnemonic(KeyStore.BIOMETRIC))
        if (isEnabled) {
            try {
                BiometricKeystoreHelper(context).getEncryptCipher()
                return true
            } catch (e: KeyPermanentlyInvalidatedException) {
                MewLog.w(TAG, "KeyPermanentlyInvalidatedException")
                BiometricKeystoreHelper(context).removeKey()
            }
        }
        return false
    }

    fun disable(preferences: PreferencesManager) {
        preferences.applicationPreferences.removeWalletMnemonic(KeyStore.BIOMETRIC)
        for (network in Network.values()) {
            preferences.getWalletPreferences(network).removeWalletPrivateKey(KeyStore.BIOMETRIC)
        }
    }
}