package com.example.fix_finder.data.model

data class ChatConversation(
    val id: String,
    val providerId: String,
    val providerName: String,
    val providerCategory: String,
    val avatarName: String,
    var lastMessage: String,
    var timestamp: String,
    var unreadCount: Int = 0
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isFromUser: Boolean
)
