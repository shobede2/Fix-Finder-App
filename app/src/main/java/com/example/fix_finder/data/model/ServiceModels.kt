package com.example.fix_finder.data.model

data class ServiceCategory(
    val id: String,
    val name: String,
    val iconName: String,
    val description: String
)

data class ServiceOffer(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val priceDisplay: String
)

data class ServiceProvider(
    val id: String,
    val name: String,
    val avatarName: String,
    val categoryId: String,
    val categoryName: String,
    val rating: Double,
    val reviewsCount: Int,
    val yearsExperience: Int,
    val location: String,
    val startingPriceDisplay: String,
    val jobsCompleted: Int,
    val responseTime: String,
    val bio: String,
    val isVerified: Boolean = true,
    val services: List<ServiceOffer> = emptyList(),
    val reviews: List<ProviderReview> = emptyList()
)

data class ProviderReview(
    val id: String,
    val reviewerName: String,
    val rating: Double,
    val comment: String,
    val date: String
)

data class ServiceRequest(
    val id: String,
    val userId: String,
    val userName: String,
    val providerId: String,
    val providerName: String,
    val providerCategory: String,
    val serviceName: String,
    val description: String,
    val address: String,
    val date: String,
    val time: String,
    val estimatedPrice: String,
    val status: String = STATUS_PENDING, // PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
    val createdAt: String,
    val imageUrl: String? = null,
    val isSynced: Boolean = true
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_ACCEPTED = "ACCEPTED"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}

data class ServiceRequestCreate(
    val userId: String,
    val userName: String,
    val providerId: String,
    val providerName: String,
    val providerCategory: String,
    val serviceName: String,
    val description: String,
    val address: String,
    val date: String,
    val time: String,
    val estimatedPrice: String,
    val imageUrl: String? = null
)

data class UserSettings(
    val userId: String,
    val pushNotifications: Boolean = true,
    val emailAlerts: Boolean = true,
    val smsAlerts: Boolean = false,
    val preferredLocation: String = "Polokwane, Limpopo",
    val themeMode: String = THEME_SYSTEM, // "SYSTEM", "LIGHT", "DARK"
    val preferredLanguage: String = "en" // "en" (English), "zu" (isiZulu)
) {
    companion object {
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
    }
}

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    var isRead: Boolean = false,
    val type: String = TYPE_INFO
) {
    companion object {
        const val TYPE_INFO = "INFO"
        const val TYPE_BOOKING = "BOOKING"
        const val TYPE_PROMO = "PROMO"
        const val TYPE_SYSTEM = "SYSTEM"
    }
}
