package one.mixin.android.ui.group

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.uber.autodispose.autoDispose
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_invite.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.group.InviteActivity.Companion.ARGS_ID
import one.mixin.android.util.ErrorHandler

class InviteFragment : BaseFragment() {
    companion object {
        const val TAG = "InviteFragment"

        fun putBundle(id: String): Bundle {
            return Bundle().apply {
                putString(ARGS_ID, id)
            }
        }

        fun newInstance(bundle: Bundle): InviteFragment {
            val fragment = InviteFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private val conversationId: String by lazy {
        requireArguments().getString(ARGS_ID)!!
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val inviteViewModel: InviteViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(InviteViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_invite, container, false)

    private fun refreshUrl() {
        inviteViewModel.findConversation(conversationId).autoDispose(stopScope).subscribe({
            if (it.isSuccess && it.data != null) {
                inviteViewModel.updateCodeUrl(conversationId, it.data!!.codeUrl)
            }
        }, { ErrorHandler.handleError(it) })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }

        inviteViewModel.getConversation(conversationId).observe(viewLifecycleOwner, Observer {
            it.notNullWithElse({
                val url = it.codeUrl
                invite_link.text = url
                invite_forward.setOnClickListener {
                    context?.let {
                        ForwardActivity.show(it, url)
                    }
                }
                invite_copy.setOnClickListener {
                    context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, url))
                    context?.toast(R.string.copy_success)
                }
                invite_share.setOnClickListener {
                    val sendIntent = Intent()
                    sendIntent.action = Intent.ACTION_SEND
                    sendIntent.putExtra(Intent.EXTRA_TEXT, url)
                    sendIntent.type = "text/plain"
                    startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.invite_title)))
                }
            }, {
                context?.toast(R.string.invite_invalid)
            })
        })

        invite_revoke.setOnClickListener {
            inviteViewModel.rotate(conversationId).autoDispose(stopScope).subscribe({
                if (it.isSuccess) {
                    val cr = it.data!!
                    invite_link.text = cr.codeUrl
                    inviteViewModel.updateCodeUrl(cr.conversationId, cr.codeUrl)
                }
            }, {
                ErrorHandler.handleError(it)
            })
        }

        refreshUrl()
    }
}
