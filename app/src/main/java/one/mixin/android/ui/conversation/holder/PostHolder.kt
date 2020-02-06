package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import io.noties.markwon.Markwon
import kotlinx.android.synthetic.main.item_chat_action.view.chat_name
import kotlinx.android.synthetic.main.item_chat_post.view.*
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip

class PostHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    init {
        itemView.chat_tv.layoutParams.width = itemView.context.maxItemWidth()
        itemView.chat_tv.maxHeight = itemView.context.maxItemWidth() * 10 / 16
        itemView.chat_tv.round(dp3)
    }

    private val dp6 by lazy {
        MixinApplication.appContext.dpToPx(6f)
    }

    private val dp1 by lazy {
        MixinApplication.appContext.dpToPx(1f)
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            if (isLast) {
                itemView.chat_time.setBackgroundResource(R.drawable.chat_bubble_shadow_last)
            } else {
                itemView.chat_time.setBackgroundResource(R.drawable.chat_bubble_shadow)
            }
            (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
        } else {
            if (isLast) {
                itemView.chat_time.setBackgroundResource(R.drawable.chat_bubble_shadow)
            } else {
                itemView.chat_time.setBackgroundResource(R.drawable.chat_bubble_shadow)
            }
            (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
        }
        (itemView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd =
            if (isMe && !isLast) {
                dp6
            } else {
                dp1
            }
        val lp = (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_post_me_last,
                    R.drawable.chat_bubble_post_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_post_me,
                    R.drawable.chat_bubble_post_me_night
                )
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_post_other_last,
                    R.drawable.chat_bubble_post_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_post_other,
                    R.drawable.chat_bubble_post_other_night
                )
            }
        }
    }

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener,
        miniMarkwon: Markwon
    ) {
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        itemView.chat_tv.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
            }
        }

        itemView.chat_tv.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            } else {
                onItemListener.onPostClick(itemView, messageItem)
            }
        }
        itemView.chat_layout.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onPostClick(itemView, messageItem)
            }
        }

        if (!messageItem.thumbImage.isNullOrEmpty()) {
            miniMarkwon.setMarkdown(itemView.chat_tv, messageItem.thumbImage)
        } else if (!messageItem.content.isNullOrEmpty()) {
            miniMarkwon.setMarkdown(itemView.chat_tv, messageItem.content.split("\n").take(20).joinToString("\n"))
        } else {
            itemView.chat_tv.text = null
        }

        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
            }
        }

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }

        val isMe = meId == messageItem.userId
        if (isFirst && !isMe) {
            itemView.chat_name.visibility = View.VISIBLE
            itemView.chat_name.text = messageItem.userFullName
            if (messageItem.appId != null) {
                itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
                itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
            } else {
                itemView.chat_name.setCompoundDrawables(null, null, null, null)
            }
            itemView.chat_name.setTextColor(getColorById(messageItem.userId))
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            itemView.chat_name.visibility = View.GONE
        }

        if (messageItem.appId != null) {
            itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
            itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
        } else {
            itemView.chat_name.setCompoundDrawables(null, null, null, null)
        }
        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal(), true) { statusIcon, secretIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, secretIcon, null, statusIcon, null)
        }
        chatLayout(isMe, isLast)
    }
}
