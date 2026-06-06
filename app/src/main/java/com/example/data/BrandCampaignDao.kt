package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BrandCampaignDao {
    @Query("SELECT * FROM brand_campaigns ORDER BY timestamp DESC")
    fun getAllCampaigns(): Flow<List<BrandCampaign>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: BrandCampaign): Long

    @Update
    suspend fun updateCampaign(campaign: BrandCampaign)

    @Query("SELECT * FROM brand_campaigns WHERE id = :id")
    suspend fun getCampaignById(id: Int): BrandCampaign?

    @Query("DELETE FROM brand_campaigns WHERE id = :id")
    suspend fun deleteCampaignById(id: Int)
}
