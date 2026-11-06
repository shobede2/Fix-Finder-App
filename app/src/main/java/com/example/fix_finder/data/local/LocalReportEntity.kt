package com.example.fix_finder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.fix_finder.data.model.ServiceRequest

@Entity(tableName = "local_reports")
data class LocalReportEntity(
    @PrimaryKey
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
    val status: String,
    val createdAt: String,
    val imageUrl: String? = null,
    val isSynced: Boolean = true
) {
    fun toServiceRequest(): ServiceRequest {
        return ServiceRequest(
            id = id,
            userId = userId,
            userName = userName,
            providerId = providerId,
            providerName = providerName,
            providerCategory = providerCategory,
            serviceName = serviceName,
            description = description,
            address = address,
            date = date,
            time = time,
            estimatedPrice = estimatedPrice,
            status = status,
            createdAt = createdAt,
            imageUrl = imageUrl,
            isSynced = isSynced
        )
    }

    companion object {
        fun fromServiceRequest(req: ServiceRequest, isSynced: Boolean = true): LocalReportEntity {
            return LocalReportEntity(
                id = req.id,
                userId = req.userId,
                userName = req.userName,
                providerId = req.providerId,
                providerName = req.providerName,
                providerCategory = req.providerCategory,
                serviceName = req.serviceName,
                description = req.description,
                address = req.address,
                date = req.date,
                time = req.time,
                estimatedPrice = req.estimatedPrice,
                status = req.status,
                createdAt = req.createdAt,
                imageUrl = req.imageUrl,
                isSynced = isSynced
            )
        }
    }
}
