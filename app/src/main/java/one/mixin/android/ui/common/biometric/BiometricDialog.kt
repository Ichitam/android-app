package one.mixin.android.ui.common.biometric

import android.content.Context
import android.os.CancellationSignal
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import com.crashlytics.android.Crashlytics
import java.nio.charset.Charset
import java.security.InvalidKeyException
import moe.feng.support.biometricprompt.BiometricPromptCompat
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.util.BiometricUtil
import org.jetbrains.anko.getStackTraceString
import org.jetbrains.anko.toast

class BiometricInfo(
    val title: String,
    val subTitle: String,
    val description: String,
    val negativeBtnText: String
)

class BiometricDialog(
    private val context: Context,
    private val biometricInfo: BiometricInfo
) {
    var callback: Callback? = null
    private var cancellationSignal: CancellationSignal? = null

    fun show() {
        val biometricPromptBuilder = BiometricPromptCompat.Builder(context)
        biometricPromptBuilder.setTitle(biometricInfo.title)
            .setSubtitle(biometricInfo.subTitle)
            .setDescription(biometricInfo.description)
            .setNegativeButton(biometricInfo.negativeBtnText) { _, _ ->
                callback?.showPin()
            }
        val biometricPrompt = biometricPromptBuilder.build()
        val cipher = try {
            BiometricUtil.getDecryptCipher(context)
        } catch (e: Exception) {
            when (e) {
                is UserNotAuthenticatedException -> callback?.showAuthenticationScreen()
                is InvalidKeyException, is NullPointerException -> {
                    BiometricUtil.deleteKey(context)
                    context.toast(R.string.wallet_biometric_invalid)
                    callback?.onCancel()
                }
                else ->
                    Crashlytics.log(Log.ERROR, BiometricUtil.CRASHLYTICS_BIOMETRIC, "getDecryptCipher. ${e.getStackTraceString()}")
            }
            return
        }
        val cryptoObject = BiometricPromptCompat.DefaultCryptoObject(cipher)
        cancellationSignal = CancellationSignal().apply {
            setOnCancelListener { context.toast(R.string.cancel) }
        }
        biometricPrompt.authenticate(cryptoObject, cancellationSignal, biometricCallback)
    }

    private val biometricCallback = object : BiometricPromptCompat.IAuthenticationCallback {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            if (errorCode == BiometricPromptCompat.BIOMETRIC_ERROR_CANCELED || errorCode == BiometricPromptCompat.BIOMETRIC_ERROR_USER_CANCELED) {
                callback?.onCancel()
            } else if (errorCode == BiometricPromptCompat.BIOMETRIC_ERROR_LOCKOUT ||
                errorCode == BiometricPromptCompat.BIOMETRIC_ERROR_LOCKOUT_PERMANENT) {
                cancellationSignal?.cancel()
                callback?.showPin()
            } else {
                errString?.let { context.toast(it) }
            }
        }

        override fun onAuthenticationSucceeded(result: BiometricPromptCompat.IAuthenticationResult) {
            val cipher = result.cryptoObject?.cipher
            if (cipher != null) {
                try {
                    val encrypt = context.defaultSharedPreferences.getString(Constants.BIOMETRICS_PIN, null)
                    val decryptByteArray = cipher.doFinal(Base64.decode(encrypt, Base64.URL_SAFE))
                    val pin = decryptByteArray.toString(Charset.defaultCharset())
                    callback?.onPinComplete(pin)
                } catch (e: Exception) {
                    Crashlytics.log(Log.ERROR, BiometricUtil.CRASHLYTICS_BIOMETRIC, "onAuthenticationSucceeded  ${e.getStackTraceString()}")
                }
            }
        }

        override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
        }

        override fun onAuthenticationFailed() {
        }
    }

    interface Callback {
        fun onPinComplete(pin: String)

        fun showPin()

        fun showAuthenticationScreen()

        fun onCancel()
    }
}
