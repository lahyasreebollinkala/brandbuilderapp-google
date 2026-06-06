package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "brand_campaigns")
data class BrandCampaign(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productName: String,
    val productDescription: String,
    val brandTheme: String,
    val brandColors: String,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Base64-encoded image data
    val billboardImageBase64: String? = null,
    val newspaperImageBase64: String? = null,
    val socialPostImageBase64: String? = null,
    
    // Exact prompts sent for auditing / consistency tracking
    val billboardPrompt: String? = null,
    val newspaperPrompt: String? = null,
    val socialPostPrompt: String? = null
)
