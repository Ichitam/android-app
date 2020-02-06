package one.mixin.android.ui.setting

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import java.util.Locale
import kotlinx.android.synthetic.main.fragment_setting.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants.Account.PREF_LANGUAGE
import one.mixin.android.Constants.Account.PREF_SET_LANGUAGE
import one.mixin.android.Constants.Theme.THEME_CURRENT_ID
import one.mixin.android.Constants.Theme.THEME_DEFAULT_ID
import one.mixin.android.Constants.Theme.THEME_NIGHT_ID
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putString
import one.mixin.android.ui.device.DeviceFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.Session

class SettingFragment : Fragment() {
    companion object {
        const val TAG = "SettingFragment"

        const val ARGS_RECREATE = "args_recreate"

        fun newInstance() = SettingFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        layoutInflater.inflate(R.layout.fragment_setting, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener {
            activity?.onBackPressed()
        }
        about_rl.setOnClickListener {
            navTo(AboutFragment.newInstance(), AboutFragment.TAG)
        }
        desktop_rl.setOnClickListener {
            DeviceFragment.newInstance().showNow(parentFragmentManager, DeviceFragment.TAG)
        }
        storage_rl.setOnClickListener {
            navTo(SettingDataStorageFragment.newInstance(), SettingDataStorageFragment.TAG)
        }
        backup_rl.setOnClickListener {
            navTo(BackUpFragment.newInstance(), BackUpFragment.TAG)
        }
        privacy_rl.setOnClickListener {
            navTo(PrivacyFragment.newInstance(), PrivacyFragment.TAG)
        }
        wallet_rl.setOnClickListener {
            if (Session.getAccount()?.hasPin == true) {
                navTo(WalletSettingFragment.newInstance(), WalletSettingFragment.TAG)
            } else {
                navTo(WalletPasswordFragment.newInstance(false), WalletPasswordFragment.TAG)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            night_mode_rl.isVisible = true
            night_mode_desc_sw.isChecked =
                defaultSharedPreferences.getInt(
                    THEME_CURRENT_ID,
                    THEME_DEFAULT_ID
                ) == THEME_NIGHT_ID
            night_mode_desc_sw.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    defaultSharedPreferences.putInt(THEME_CURRENT_ID, THEME_NIGHT_ID)
                } else {
                    defaultSharedPreferences.putInt(THEME_CURRENT_ID, THEME_DEFAULT_ID)
                }
                MainActivity.reopen(requireContext())
            }
        } else {
            night_mode_rl.isVisible = false
        }
        language_rl.setOnClickListener { showLanguageAlert() }
        notification_rl.setOnClickListener {
            navTo(NotificationsFragment.newInstance(), NotificationsFragment.TAG)
        }
    }

    private fun showLanguageAlert() {
        val choice = resources.getStringArray(R.array.language_names)
        val setLanguage = defaultSharedPreferences.getBoolean(PREF_SET_LANGUAGE, false)
        val selectItem = if (setLanguage) {
            val language =
                defaultSharedPreferences.getString(PREF_LANGUAGE, Locale.ENGLISH.language)
            if (language == Locale.SIMPLIFIED_CHINESE.language) {
                1
            } else {
                0
            }
        } else {
            if (Locale.getDefault().language == Locale.SIMPLIFIED_CHINESE.language) {
                1
            } else {
                0
            }
        }
        var newSelectItem = selectItem
        AlertDialog.Builder(requireContext(), R.style.MixinAlertDialogTheme)
            .setTitle(R.string.language)
            .setSingleChoiceItems(choice, selectItem) { _, which ->
                newSelectItem = which
            }
            .setPositiveButton(R.string.group_ok) { dialog, _ ->
                if (newSelectItem != selectItem) {
                    defaultSharedPreferences.putString(
                        PREF_LANGUAGE,
                        when (newSelectItem) {
                            0 -> Locale.ENGLISH.language
                            else -> Locale.CHINA.language
                        }
                    )
                    defaultSharedPreferences.putBoolean(PREF_SET_LANGUAGE, true)

                    MainActivity.reopen(requireContext())
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
