package com.example.fix_finder.data.repository

import android.content.Context
import com.example.fix_finder.data.api.ApiClient
import com.example.fix_finder.data.api.FixFinderApiService
import com.example.fix_finder.data.local.FixFinderDatabase
import com.example.fix_finder.data.local.LocalReportEntity
import com.example.fix_finder.data.local.NetworkObserver
import com.example.fix_finder.data.local.NotificationHelper
import com.example.fix_finder.data.model.*
import com.google.gson.Gson
import java.util.UUID

sealed class ResultState<out T> {
    data class Success<out T>(val data: T) : ResultState<T>()
    data class Error(val message: String) : ResultState<Nothing>()
    object Loading : ResultState<Nothing>()
}

class ServiceRepository(
    private val apiService: FixFinderApiService = ApiClient.apiService,
    private val context: Context? = null
) {
    private val gson = Gson()
    private val db = context?.let { FixFinderDatabase.getInstance(it) }
    private val networkObserver = context?.let { NetworkObserver(it) }

    suspend fun getCategories(): ResultState<List<ServiceCategory>> {
        return try {
            val response = apiService.getCategories()
            if (response.isSuccessful && response.body() != null) {
                ResultState.Success(response.body()!!)
            } else {
                ResultState.Error("Failed to fetch service categories.")
            }
        } catch (e: Exception) {
            ResultState.Error("Network error: ${e.localizedMessage ?: "Connection failed"}")
        }
    }

    suspend fun getProviders(
        query: String? = null,
        category: String? = null,
        location: String? = null
    ): ResultState<List<ServiceProvider>> {
        return try {
            val response = apiService.getProviders(query, category, location)
            if (response.isSuccessful && response.body() != null) {
                ResultState.Success(response.body()!!)
            } else {
                ResultState.Error("Failed to fetch service providers.")
            }
        } catch (e: Exception) {
            ResultState.Error("Network error: ${e.localizedMessage ?: "Connection failed"}")
        }
    }

    suspend fun getProviderById(id: String): ResultState<ServiceProvider> {
        return try {
            val response = apiService.getProviderById(id)
            if (response.isSuccessful && response.body() != null) {
                ResultState.Success(response.body()!!)
            } else {
                ResultState.Error("Service provider not found.")
            }
        } catch (e: Exception) {
            ResultState.Error("Network error: ${e.localizedMessage ?: "Connection failed"}")
        }
    }

    suspend fun createServiceRequest(request: ServiceRequestCreate): ResultState<ServiceRequest> {
        if (request.description.isBlank()) {
            return ResultState.Error("Please describe the problem or service required.")
        }
        if (request.address.isBlank()) {
            return ResultState.Error("Please enter a valid service location.")
        }

        val isOnline = networkObserver?.isOnline() ?: true

        if (!isOnline) {
            // Save locally to Room database for offline sync
            val offlineId = "REQ-OFFLINE-" + UUID.randomUUID().toString().take(6)
            val offlineReq = ServiceRequest(
                id = offlineId,
                userId = request.userId,
                userName = request.userName,
                providerId = request.providerId,
                providerName = request.providerName,
                providerCategory = request.providerCategory,
                serviceName = request.serviceName,
                description = request.description,
                address = request.address,
                date = request.date,
                time = request.time,
                estimatedPrice = request.estimatedPrice,
                status = ServiceRequest.STATUS_PENDING,
                createdAt = "Saved Offline",
                imageUrl = request.imageUrl,
                isSynced = false
            )

            db?.reportDao()?.insertReport(LocalReportEntity.fromServiceRequest(offlineReq, isSynced = false))

            context?.let {
                NotificationHelper.showNotification(
                    it,
                    "Saved Offline (Pending Sync)",
                    "Your report for ${request.serviceName} was saved locally. It will auto-sync when online."
                )
            }

            return ResultState.Success(offlineReq)
        }

        return try {
            val response = apiService.createServiceRequest(request)
            if (response.isSuccessful && response.body() != null) {
                val createdReq = response.body()!!
                db?.reportDao()?.insertReport(LocalReportEntity.fromServiceRequest(createdReq, isSynced = true))

                context?.let {
                    NotificationHelper.showReportSubmittedNotification(it, createdReq.id, createdReq.serviceName)
                }

                ResultState.Success(createdReq)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                ResultState.Error(errorMsg ?: "Failed to submit service request.")
            }
        } catch (e: Exception) {
            ResultState.Error("Network error: ${e.localizedMessage ?: "Connection failed"}")
        }
    }

    suspend fun getUserRequests(userId: String): ResultState<List<ServiceRequest>> {
        val isOnline = networkObserver?.isOnline() ?: true

        if (!isOnline) {
            val localReports = db?.reportDao()?.getUserReports(userId)?.map { it.toServiceRequest() } ?: emptyList()
            return ResultState.Success(localReports)
        }

        return try {
            val response = apiService.getUserRequests(userId)
            if (response.isSuccessful && response.body() != null) {
                val remoteRequests = response.body()!!

                // Cache remote requests in Room
                db?.reportDao()?.insertAll(remoteRequests.map { LocalReportEntity.fromServiceRequest(it, isSynced = true) })

                // Include any unsynced local offline requests
                val unsyncedLocal = db?.reportDao()?.getPendingUnsyncedReports()?.map { it.toServiceRequest() } ?: emptyList()
                val combined = unsyncedLocal + remoteRequests

                ResultState.Success(combined)
            } else {
                val cached = db?.reportDao()?.getUserReports(userId)?.map { it.toServiceRequest() } ?: emptyList()
                if (cached.isNotEmpty()) ResultState.Success(cached) else ResultState.Error("Failed to load service requests.")
            }
        } catch (e: Exception) {
            val cached = db?.reportDao()?.getUserReports(userId)?.map { it.toServiceRequest() } ?: emptyList()
            if (cached.isNotEmpty()) {
                ResultState.Success(cached)
            } else {
                ResultState.Error("Network error: ${e.localizedMessage ?: "Connection failed"}")
            }
        }
    }

    suspend fun syncPendingOfflineReports(): Int {
        val pending = db?.reportDao()?.getPendingUnsyncedReports() ?: return 0
        if (pending.isEmpty()) return 0

        var syncedCount = 0
        for (localEntity in pending) {
            try {
                val payload = ServiceRequestCreate(
                    userId = localEntity.userId,
                    userName = localEntity.userName,
                    providerId = localEntity.providerId,
                    providerName = localEntity.providerName,
                    providerCategory = localEntity.providerCategory,
                    serviceName = localEntity.serviceName,
                    description = localEntity.description,
                    address = localEntity.address,
                    date = localEntity.date,
                    time = localEntity.time,
                    estimatedPrice = localEntity.estimatedPrice,
                    imageUrl = localEntity.imageUrl
                )
                val response = apiService.createServiceRequest(payload)
                if (response.isSuccessful && response.body() != null) {
                    val syncedRemote = response.body()!!
                    db.reportDao().deleteReport(localEntity.id)
                    db.reportDao().insertReport(LocalReportEntity.fromServiceRequest(syncedRemote, isSynced = true))
                    syncedCount++
                }
            } catch (e: Exception) {
                // Keep for next sync attempt
            }
        }

        if (syncedCount > 0 && context != null) {
            NotificationHelper.showSyncCompletedNotification(context, syncedCount)
        }

        return syncedCount
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val map = gson.fromJson(errorBody, Map::class.java)
            (map["message"] ?: map["error"])?.toString()
        } catch (e: Exception) {
            null
        }
    }
}
