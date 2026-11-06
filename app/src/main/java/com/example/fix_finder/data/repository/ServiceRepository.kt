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
    private val appContext = context?.applicationContext
    private val db = appContext?.let { FixFinderDatabase.getInstance(it) }
    private val networkObserver = appContext?.let { NetworkObserver(it) }

    suspend fun getCategories(): ResultState<List<ServiceCategory>> {
        return try {
            val response = apiService.getCategories()
            if (response.isSuccessful && response.body() != null) {
                ResultState.Success(response.body()!!)
            } else {
                ResultState.Error("Failed to fetch service categories.")
            }
        } catch (e: Throwable) {
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
        } catch (e: Throwable) {
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
        } catch (e: Throwable) {
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

        val isOnline = try { networkObserver?.isOnline() ?: true } catch (_: Throwable) { true }

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

            try {
                db?.reportDao()?.insertReport(LocalReportEntity.fromServiceRequest(offlineReq, isSynced = false))
            } catch (_: Throwable) {}

            appContext?.let {
                try {
                    NotificationHelper.showNotification(
                        it,
                        "Saved Offline (Pending Sync)",
                        "Your report for ${request.serviceName} was saved locally. It will auto-sync when online."
                    )
                } catch (_: Throwable) {}
            }

            return ResultState.Success(offlineReq)
        }

        return try {
            val response = apiService.createServiceRequest(request)
            if (response.isSuccessful && response.body() != null) {
                val createdReq = response.body()!!
                try {
                    db?.reportDao()?.insertReport(LocalReportEntity.fromServiceRequest(createdReq, isSynced = true))
                } catch (_: Throwable) {}

                appContext?.let {
                    try {
                        NotificationHelper.showReportSubmittedNotification(it, createdReq.id, createdReq.serviceName)
                    } catch (_: Throwable) {}
                }

                ResultState.Success(createdReq)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                ResultState.Error(errorMsg ?: "Failed to submit service request.")
            }
        } catch (e: Throwable) {
            ResultState.Error("Network error: ${e.localizedMessage ?: "Connection failed"}")
        }
    }

    suspend fun getUserRequests(userId: String): ResultState<List<ServiceRequest>> {
        val isOnline = try { networkObserver?.isOnline() ?: true } catch (_: Throwable) { true }

        if (!isOnline) {
            val localReports = try {
                db?.reportDao()?.getUserReports(userId)?.map { it.toServiceRequest() } ?: emptyList()
            } catch (_: Throwable) {
                emptyList()
            }
            return ResultState.Success(localReports)
        }

        return try {
            val response = apiService.getUserRequests(userId)
            if (response.isSuccessful && response.body() != null) {
                val remoteRequests = response.body()!!

                // Cache remote requests in Room
                try {
                    db?.reportDao()?.insertAll(remoteRequests.map { LocalReportEntity.fromServiceRequest(it, isSynced = true) })
                } catch (_: Throwable) {}

                // Include any unsynced local offline requests
                val unsyncedLocal = try {
                    db?.reportDao()?.getPendingUnsyncedReports()?.map { it.toServiceRequest() } ?: emptyList()
                } catch (_: Throwable) {
                    emptyList()
                }
                val combined = unsyncedLocal + remoteRequests

                ResultState.Success(combined)
            } else {
                val cached = try {
                    db?.reportDao()?.getUserReports(userId)?.map { it.toServiceRequest() } ?: emptyList()
                } catch (_: Throwable) {
                    emptyList()
                }
                if (cached.isNotEmpty()) ResultState.Success(cached) else ResultState.Error("Failed to load service requests.")
            }
        } catch (e: Throwable) {
            val cached = try {
                db?.reportDao()?.getUserReports(userId)?.map { it.toServiceRequest() } ?: emptyList()
            } catch (_: Throwable) {
                emptyList()
            }
            if (cached.isNotEmpty()) {
                ResultState.Success(cached)
            } else {
                ResultState.Error("Network error: ${e.localizedMessage ?: "Connection failed"}")
            }
        }
    }

    suspend fun syncPendingOfflineReports(): Int {
        val pending = try {
            db?.reportDao()?.getPendingUnsyncedReports() ?: return 0
        } catch (_: Throwable) {
            return 0
        }
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
                    try {
                        db.reportDao().deleteReport(localEntity.id)
                        db.reportDao().insertReport(LocalReportEntity.fromServiceRequest(syncedRemote, isSynced = true))
                    } catch (_: Throwable) {}
                    syncedCount++
                }
            } catch (_: Throwable) {
                // Keep for next sync attempt
            }
        }

        if (syncedCount > 0 && appContext != null) {
            try {
                NotificationHelper.showSyncCompletedNotification(appContext, syncedCount)
            } catch (_: Throwable) {}
        }

        return syncedCount
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val map = gson.fromJson(errorBody, Map::class.java)
            (map["message"] ?: map["error"])?.toString()
        } catch (_: Throwable) {
            null
        }
    }
}
