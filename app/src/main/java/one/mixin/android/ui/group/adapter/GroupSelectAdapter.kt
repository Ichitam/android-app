package one.mixin.android.ui.group.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_group_select.view.*
import one.mixin.android.R
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class GroupSelectAdapter(val removeUser: (User) -> Unit) : RecyclerView.Adapter<GroupSelectAdapter.SelectViewHolder>() {

    var checkedUsers: List<User>? = null

    class SelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_group_select, parent, false)
        return SelectViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return checkedUsers.notNullWithElse({ it.size }, 0)
    }

    override fun onBindViewHolder(holder: SelectViewHolder, position: Int) {
        checkedUsers?.let { list ->
            val user = list[position]
            holder.itemView.avatar_view.setInfo(user.fullName, user.avatarUrl, user.userId)
            holder.itemView.name_tv.text = user.fullName
            user.showVerifiedOrBot(holder.itemView.verified_iv, holder.itemView.bot_iv)
        }
        holder.itemView.setOnClickListener {
            checkedUsers?.let { list ->
                val user = list[position]
                removeUser(user)
            }
        }
    }
}
