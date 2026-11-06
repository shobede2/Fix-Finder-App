package com.example.fix_finder.data.repository

import com.example.fix_finder.data.model.ChatConversation
import com.example.fix_finder.data.model.ChatMessage
import com.example.fix_finder.data.model.ServiceProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChatRepository {

    private val conversations = mutableListOf(
        ChatConversation(
            id = "conv_1",
            providerId = "tech_1",
            providerName = "Thabo Mokoena",
            providerCategory = "Plumbing Professional",
            avatarName = "avatar_thabo",
            lastMessage = "I am on my way to check the kitchen sink tap leak.",
            timestamp = "10:30 AM",
            unreadCount = 1
        ),
        ChatConversation(
            id = "conv_2",
            providerId = "tech_3",
            providerName = "Lerato Ndlovu",
            providerCategory = "Electrical Specialist",
            avatarName = "avatar_lerato",
            lastMessage = "Hello! I can assist with your DB board inspection tomorrow.",
            timestamp = "Yesterday",
            unreadCount = 0
        ),
        ChatConversation(
            id = "conv_3",
            providerId = "tech_2",
            providerName = "Sipho Khumalo",
            providerCategory = "Drain & Pipe Emergency",
            avatarName = "avatar_sipho",
            lastMessage = "Thanks for contacting me! What address in Polokwane?",
            timestamp = "May 8",
            unreadCount = 0
        )
    )

    private val messagesMap = mutableMapOf(
        "conv_1" to mutableListOf(
            ChatMessage("m1", "conv_1", "User", "Hi Thabo, are you available for a tap repair today?", "10:15 AM", true),
            ChatMessage("m2", "conv_1", "Thabo Mokoena", "Hello! Yes I am in Polokwane Central today.", "10:18 AM", false),
            ChatMessage("m3", "conv_1", "Thabo Mokoena", "I am on my way to check the kitchen sink tap leak.", "10:30 AM", false)
        ),
        "conv_2" to mutableListOf(
            ChatMessage("m4", "conv_2", "User", "Hi Lerato, do you do inverter wiring?", "Yesterday", true),
            ChatMessage("m5", "conv_2", "Lerato Ndlovu", "Hello! I can assist with your DB board inspection tomorrow.", "Yesterday", false)
        ),
        "conv_3" to mutableListOf(
            ChatMessage("m6", "conv_3", "User", "Hi Sipho, emergency blocked drain issue here.", "May 8", true),
            ChatMessage("m7", "conv_3", "Sipho Khumalo", "Thanks for contacting me! What address in Polokwane?", "May 8", false)
        )
    )

    fun getConversations(): List<ChatConversation> = conversations.toList()

    fun getMessages(conversationId: String): List<ChatMessage> {
        return messagesMap[conversationId]?.toList() ?: emptyList()
    }

    fun getOrCreateConversation(provider: ServiceProvider): ChatConversation {
        val existing = conversations.find { it.providerId == provider.id }
        if (existing != null) return existing

        val newConv = ChatConversation(
            id = "conv_" + System.currentTimeMillis(),
            providerId = provider.id,
            providerName = provider.name,
            providerCategory = "${provider.categoryName} Professional",
            avatarName = provider.avatarName,
            lastMessage = "Started a conversation with ${provider.name}",
            timestamp = getCurrentTime(),
            unreadCount = 0
        )
        conversations.add(0, newConv)
        messagesMap[newConv.id] = mutableListOf(
            ChatMessage(
                id = "msg_" + System.currentTimeMillis(),
                conversationId = newConv.id,
                senderName = provider.name,
                text = "Hello! Thank you for reaching out to ${provider.name} (${provider.categoryName}). How can I help you today?",
                timestamp = getCurrentTime(),
                isFromUser = false
            )
        )
        return newConv
    }

    fun sendMessage(conversationId: String, text: String): ChatMessage {
        val conv = conversations.find { it.id == conversationId }
        val time = getCurrentTime()
        val userMsg = ChatMessage(
            id = "msg_" + System.currentTimeMillis(),
            conversationId = conversationId,
            senderName = "User",
            text = text,
            timestamp = time,
            isFromUser = true
        )

        val msgList = messagesMap.getOrPut(conversationId) { mutableListOf() }
        msgList.add(userMsg)

        if (conv != null) {
            conv.lastMessage = "You: $text"
            conv.timestamp = time
        }

        return userMsg
    }

    fun generateAutoReply(conversationId: String, providerName: String): ChatMessage {
        val time = getCurrentTime()
        val replies = listOf(
            "Got it! I am reviewing your message and will update you shortly.",
            "Thanks for the details! I can head over to your location soon.",
            "Sounds good! Let me know if you need any additional tools or parts for the job.",
            "No problem at all! I will confirm as soon as I finish my current booking."
        )
        val replyText = replies.random()

        val replyMsg = ChatMessage(
            id = "msg_reply_" + System.currentTimeMillis(),
            conversationId = conversationId,
            senderName = providerName,
            text = replyText,
            timestamp = time,
            isFromUser = false
        )

        val msgList = messagesMap.getOrPut(conversationId) { mutableListOf() }
        msgList.add(replyMsg)

        val conv = conversations.find { it.id == conversationId }
        if (conv != null) {
            conv.lastMessage = replyText
            conv.timestamp = time
        }

        return replyMsg
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }
}
