package com.example.fix_finder

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.fix_finder.data.model.ChatMessage

class ChatAdapter(
    private var messages: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ChatMessageViewHolder>() {

    fun updateData(newMessages: List<ChatMessage>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = messages.size
            override fun getNewListSize(): Int = newMessages.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return messages[oldItemPosition].id == newMessages[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return messages[oldItemPosition] == newMessages[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.messages = newMessages
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatMessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    class ChatMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.layoutMessageContainer)
        private val cardBubble: CardView = itemView.findViewById(R.id.cardMessageBubble)
        private val tvText: TextView = itemView.findViewById(R.id.tvMessageText)
        private val tvTime: TextView = itemView.findViewById(R.id.tvMessageTime)

        fun bind(message: ChatMessage) {
            tvText.text = message.text
            tvTime.text = message.timestamp

            if (message.isFromUser) {
                container.gravity = Gravity.END
                cardBubble.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1B2A4A"))) // Navy
                tvText.setTextColor(Color.WHITE)
                tvTime.setTextColor(Color.parseColor("#E0E7FF"))
            } else {
                container.gravity = Gravity.START
                cardBubble.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F1F5F9"))) // Light Grey
                tvText.setTextColor(Color.parseColor("#0F172A"))
                tvTime.setTextColor(Color.parseColor("#64748B"))
            }
        }
    }
}
