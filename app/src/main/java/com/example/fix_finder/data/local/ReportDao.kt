package com.example.fix_finder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReportDao {

    @Query("SELECT * FROM local_reports ORDER BY createdAt DESC")
    suspend fun getAllReports(): List<LocalReportEntity>

    @Query("SELECT * FROM local_reports WHERE userId = :userId OR userId = 'usr_demo' ORDER BY createdAt DESC")
    suspend fun getUserReports(userId: String): List<LocalReportEntity>

    @Query("SELECT * FROM local_reports WHERE isSynced = 0")
    suspend fun getPendingUnsyncedReports(): List<LocalReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: LocalReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<LocalReportEntity>)

    @Query("DELETE FROM local_reports WHERE id = :id")
    suspend fun deleteReport(id: String)
}
