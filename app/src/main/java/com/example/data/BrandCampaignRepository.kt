package com.example.data

import kotlinx.coroutines.flow.Flow

class BrandCampaignRepository(private val dao: BrandCampaignDao) {
    val allCampaigns: Flow<List<BrandCampaign>> = dao.getAllCampaigns()

    suspend fun getCampaignById(id: Int): BrandCampaign? {
        return dao.getCampaignById(id)
    }

    suspend fun insertCampaign(campaign: BrandCampaign): Long {
        return dao.insertCampaign(campaign)
    }

    suspend fun updateCampaign(campaign: BrandCampaign) {
        dao.updateCampaign(campaign)
    }

    suspend fun deleteCampaignById(id: Int) {
        dao.deleteCampaignById(id)
    }
}
