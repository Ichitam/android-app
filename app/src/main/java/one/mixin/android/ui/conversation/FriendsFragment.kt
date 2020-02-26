package one.mixin.android.ui.conversation

import androidx.core.os.bundleOf
import javax.inject.Inject
import one.mixin.android.R
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.friends.BaseFriendsFragment
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.ui.conversation.ConversationFragment.Companion.CONVERSATION_ID
import one.mixin.android.ui.conversation.adapter.FriendsAdapter
import one.mixin.android.ui.conversation.adapter.FriendsViewHolder
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.User

class FriendsFragment : BaseFriendsFragment<FriendsViewHolder, ConversationViewModel>(), FriendsListener {
    init {
        adapter = FriendsAdapter(userCallback).apply {
            listener = this@FriendsFragment
        }
    }

    companion object {
        const val TAG = "FriendsFragment"

        fun newInstance(conversationId: String) = FriendsFragment().apply {
            arguments = bundleOf(
                CONVERSATION_ID to conversationId
            )
        }
    }

    override fun getModelClass() = ConversationViewModel::class.java

    @Inject
    lateinit var jobManager: MixinJobManager

    private val conversationId: String by lazy { requireArguments().getString(CONVERSATION_ID)!! }

    override fun getTitleResId() = R.string.contact_other_share

    override suspend fun getFriends() = viewModel.getFriends()

    private var friendClick: ((User) -> Unit)? = null

    fun setOnFriendClick(friendClick: (User) -> Unit) {
        this.friendClick = friendClick
    }

    override fun onItemClick(user: User) {
        if (friendClick != null) {
            friendClick!!(user)
            parentFragmentManager.beginTransaction().remove(this).commit()
        } else {
            val fw = ForwardMessage(ForwardCategory.CONTACT.name, sharedUserId = user.userId)
            ConversationActivity.show(requireContext(), conversationId, null, messages = arrayListOf(fw))
        }
    }
}
