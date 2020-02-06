package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_pin_bottom_sheet_address.view.*
import kotlinx.android.synthetic.main.layout_pin_biometric.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.vo.Address
import one.mixin.android.widget.BottomSheet

class PinAddrBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PinAddrBottomSheetDialogFragment"

        const val ADD = 0
        const val DELETE = 1
        const val MODIFY = 2

        const val ARGS_ASSET_ID = "args_asset_id"
        const val ARGS_ASSET_NAME = "args_asset_name"
        const val ARGS_ASSET_URL = "args_asset_url"
        const val ARGS_CHAIN_URL = "args_chain_url"
        const val ARGS_LABEL = "args_label"
        const val ARGS_DESTINATION = "args_destination"
        const val ARGS_TAG = "args_tag"
        const val ARGS_ADDRESS_ID = "args_address_id"
        const val ARGS_TYPE = "args_type"

        fun newInstance(
            assetId: String? = null,
            assetName: String? = null,
            assetUrl: String? = null,
            chainIconUrl: String? = null,
            label: String,
            destination: String,
            tag: String? = null,
            addressId: String? = null,
            type: Int = ADD
        ) = PinAddrBottomSheetDialogFragment().apply {
            val b = bundleOf(
                ARGS_ASSET_ID to assetId,
                ARGS_ASSET_NAME to assetName,
                ARGS_ASSET_URL to assetUrl,
                ARGS_CHAIN_URL to chainIconUrl,
                ARGS_LABEL to label,
                ARGS_DESTINATION to destination,
                ARGS_ADDRESS_ID to addressId,
                ARGS_TYPE to type,
                ARGS_TAG to tag
            )
            arguments = b
        }
    }

    private val assetId: String? by lazy { arguments!!.getString(ARGS_ASSET_ID) }
    private val assetName: String? by lazy { arguments!!.getString(ARGS_ASSET_NAME) }
    private val assetUrl: String? by lazy { arguments!!.getString(ARGS_ASSET_URL) }
    private val chainIconUrl: String? by lazy { arguments!!.getString(ARGS_CHAIN_URL) }
    private val label: String? by lazy { arguments!!.getString(ARGS_LABEL) }
    private val destination: String? by lazy { arguments!!.getString(ARGS_DESTINATION) }
    private val addressId: String? by lazy { arguments!!.getString(ARGS_ADDRESS_ID) }
    private val type: Int by lazy { arguments!!.getInt(ARGS_TYPE) }
    private val addressTag: String? by lazy { arguments!!.getString(ARGS_TAG) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_pin_bottom_sheet_address, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_view.right_iv.setOnClickListener { dismiss() }
        contentView.title.text = getTitle()
        contentView.asset_icon.bg.loadImage(assetUrl, R.drawable.ic_avatar_place_holder)
        contentView.asset_icon.badge.loadImage(chainIconUrl, R.drawable.ic_avatar_place_holder)
        contentView.asset_name.text = label
        contentView.asset_address.text = destination
        contentView.pay_tv.text = getTipText()
        contentView.biometric_tv.text = getBiometricText()
    }

    override fun getBiometricInfo(): BiometricInfo {
        return BiometricInfo(
            getTitle(),
            label ?: "",
            destination ?: "",
            getTipText()
        )
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return if (type == ADD || type == MODIFY) {
            bottomViewModel.syncAddr(assetId!!, destination, label, addressTag, pin)
        } else {
            bottomViewModel.deleteAddr(addressId!!, pin)
        }
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        lifecycleScope.launch {
            if (type == ADD || type == MODIFY) {
                bottomViewModel.saveAddr(response.data as Address)
            } else {
                bottomViewModel.deleteLocalAddr(addressId!!)
            }
            contentView.biometric_layout.showPin(false)
        }
        return true
    }

    private fun getTitle() = getString(when (type) {
        ADD -> R.string.withdrawal_addr_add
        MODIFY -> R.string.withdrawal_addr_modify
        else -> R.string.withdrawal_addr_delete
    }, assetName)

    private fun getTipText() = getString(when (type) {
        ADD -> R.string.withdrawal_addr_pin_add
        DELETE -> R.string.withdrawal_addr_pin_delete
        MODIFY -> R.string.withdrawal_addr_pin_modify
        else -> R.string.withdrawal_addr_pin_add
    })

    private fun getBiometricText() = getString(when (type) {
        ADD -> R.string.withdrawal_addr_biometric_add
        DELETE -> R.string.withdrawal_addr_biometric_delete
        MODIFY -> R.string.withdrawal_addr_biometric_modify
        else -> R.string.withdrawal_addr_biometric_add
    })
}
