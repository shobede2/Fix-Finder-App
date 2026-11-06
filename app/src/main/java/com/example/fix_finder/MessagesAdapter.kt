package com.example.fix_finder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.fix_finder.data.model.ChatConversation

class MessagesAdapter(
    private var conversations: List<ChatConversation>,
    private val onItemClick: (ChatConversation) -> Unit
) : RecyclerView.Adapter<MessagesAdapter.ConversationViewHolder>() {

    fun updateData(newConversations: List<ChatConversation>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = conversations.size
            override fun getNewListSize(): Int = newConversations.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return conversations[oldItemPosition].id == newConversations[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return conversations[oldItemPosition] == newConversations[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.conversations = newConversations
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.bind(conversation, onItemClick)
    }

    override fun getItemCount(): Int = conversations.size

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgConvAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvConvName)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvConvCategory)
        private val tvLastMsg: TextView = itemView.findViewById(R.id.tvConvLastMsg)
        private val tvTime: TextView = itemView.findViewById(R.id.tvConvTime)

        fun bind(conversation: ChatConversation, onItemClick: (ChatConversation) -> Unit) {
            tvName.text = conversation.providerName
            tvCategory.text = conversation.providerCategory
            tvLastMsg.text = conversation.lastMessage
            tvTime.text = conversation.timestamp

            val avatarRes = when (conversation.avatarName) {
                "avatar_thabo" -> R.drawable.avatar_thabo
                "avatar_sipho" -> R.drawable.avatar_sipho
                "avatar_lerato" -> R.drawable.avatar_lerato
                else -> R.drawable.avatar_thabo
            }
            imgAvatar.setImageResource(avatarRes)

            itemView.setOnClickListener { onItemClick(conversation) }
        }
    }
}
