package one.mixin.android.util

import android.app.KeyguardManager
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import com.crashlytics.android.Crashlytics
import java.nio.charset.Charset
import java.security.InvalidKeyException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import one.mixin.android.Constants
import one.mixin.android.Constants.BIOMETRICS_ALIAS
import one.mixin.android.R
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.extension.remove
import one.mixin.android.extension.toast
import org.jetbrains.anko.getStackTraceString

object BiometricUtil {

    const val REQUEST_CODE_CREDENTIALS = 101

    const val CRASHLYTICS_BIOMETRIC = "biometric"

    private fun isSupport(ctx: Context): Boolean =
        BiometricManager.from(ctx).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS &&
            isKeyguardSecure(ctx) &&
            isSecureHardware() &&
            !RootUtil.isDeviceRooted

    fun isSupportWithErrorInfo(ctx: Context): Pair<Boolean, String?> {
        val canAuthResult = BiometricManager.from(ctx).canAuthenticate()
        if (canAuthResult != BiometricManager.BIOMETRIC_SUCCESS) {
            return Pair(false, getAuthErrorMsg(canAuthResult))
        }
        if (!isKeyguardSecure(ctx)) {
            return Pair(false, "The PIN, pattern or password is NOT set or a SIM card is unlocked")
        }
        if (!isSecureHardware()) {
            return Pair(false, "The key NOT resides inside secure hardware (TEE)")
        }
        if (RootUtil.isDeviceRooted) {
            return Pair(false, "The device has been rooted")
        }
        return Pair(true, null)
    }

    fun showAuthenticationScreen(fragment: Fragment) {
        val intent = fragment.requireContext().getSystemService<KeyguardManager>()?.createConfirmDeviceCredentialIntent(
            fragment.requireContext().getString(R.string.wallet_biometric_screen_lock),
            fragment.requireContext().getString(R.string.wallet_biometric_screen_lock_desc))
        if (intent != null) {
            fragment.activity?.startActivityForResult(intent, REQUEST_CODE_CREDENTIALS)
        }
    }

    fun savePin(ctx: Context, pin: String, fragment: Fragment): Boolean {
        val cipher = try {
            getEncryptCipher()
        } catch (e: Exception) {
            when (e) {
                is UserNotAuthenticatedException -> showAuthenticationScreen(fragment)
                is InvalidKeyException -> {
                    deleteKey(ctx)
                    ctx.toast(R.string.wallet_biometric_invalid)
                }
                else -> Crashlytics.log(Log.ERROR, CRASHLYTICS_BIOMETRIC, "getEncryptCipher. ${e.getStackTraceString()}")
            }
            return false
        }
        val iv = Base64.encodeBytes(cipher.iv, Base64.URL_SAFE)
        ctx.defaultSharedPreferences.putString(Constants.BIOMETRICS_IV, iv)
        val encrypt = cipher.doFinal(pin.toByteArray(Charset.defaultCharset()))
        val result = Base64.encodeBytes(encrypt, Base64.URL_SAFE)
        ctx.defaultSharedPreferences.putString(Constants.BIOMETRICS_PIN, result)
        return true
    }

    fun deleteKey(ctx: Context) {
        try {
            val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
            }
            ks.deleteEntry(BIOMETRICS_ALIAS)
        } catch (e: Exception) {
            Crashlytics.log(Log.ERROR, CRASHLYTICS_BIOMETRIC, "delete entry BIOMETRICS_ALIAS failed. ${e.getStackTraceString()}")
        }

        ctx.defaultSharedPreferences.apply {
            remove(Constants.BIOMETRICS_IV)
            remove(Constants.Account.PREF_BIOMETRICS)
            remove(Constants.BIOMETRICS_PIN)
            remove(Constants.BIOMETRIC_PIN_CHECK)
            remove(Constants.BIOMETRIC_INTERVAL)
        }
    }

    fun shouldShowBiometric(ctx: Context): Boolean {
        if (!isSupport(ctx)) return false

        val openBiometrics = ctx.defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
        val biometricPinCheck = ctx.defaultSharedPreferences.getLong(Constants.BIOMETRIC_PIN_CHECK, 0)
        val biometricInterval = ctx.defaultSharedPreferences.getLong(Constants.BIOMETRIC_INTERVAL, Constants.BIOMETRIC_INTERVAL_DEFAULT)
        val currTime = System.currentTimeMillis()
        return openBiometrics && currTime - biometricPinCheck <= biometricInterval
    }

    private fun getAuthErrorMsg(code: Int): String {
        return when (code) {
            BIOMETRIC_ERROR_NO_HARDWARE -> "There is no biometric hardware."
            BIOMETRIC_ERROR_NONE_ENROLLED -> "The user does not have any biometrics enrolled."
            else -> "The hardware is unavailable. Try again later."
        }
    }

    private fun isKeyguardSecure(ctx: Context): Boolean {
        val keyguardManager = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val isKeyguardSecure = keyguardManager.isKeyguardSecure
        if (!isKeyguardSecure) {
            deleteKey(ctx)
        }
        return isKeyguardSecure
    }

    private fun getKey(): SecretKey? {
        val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        var key: SecretKey? = null
        try {
            key = ks.getKey(BIOMETRICS_ALIAS, null) as? SecretKey
        } catch (e: Exception) {
            Crashlytics.log(Log.ERROR, CRASHLYTICS_BIOMETRIC, "getKey BIOMETRICS_ALIAS failed. ${e.getStackTraceString()}")
        }
        try {
            if (key == null) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(BIOMETRICS_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(
                            KeyProperties.BLOCK_MODE_CBC,
                            KeyProperties.BLOCK_MODE_CTR,
                            KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7,
                            KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setUserAuthenticationRequired(true)
                        .setUserAuthenticationValidityDurationSeconds(2 * 60 * 60)
                        .build())
                key = keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Crashlytics.log(Log.ERROR, CRASHLYTICS_BIOMETRIC, "keyGenerator init failed. ${e.getStackTraceString()}")
        }
        return key
    }

    private fun getEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        return cipher
    }

    fun getDecryptCipher(ctx: Context): Cipher {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val iv = ctx.defaultSharedPreferences.getString(Constants.BIOMETRICS_IV, null)
        val ivSpec = IvParameterSpec(Base64.decode(iv, Base64.URL_SAFE))
        cipher.init(Cipher.DECRYPT_MODE, getKey(), ivSpec)
        return cipher
    }

    private fun isSecureHardware(): Boolean {
        val key = getKey() ?: return false

        val factory = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
        val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
        return keyInfo.isInsideSecureHardware && keyInfo.isUserAuthenticationRequirementEnforcedBySecureHardware
    }
}
