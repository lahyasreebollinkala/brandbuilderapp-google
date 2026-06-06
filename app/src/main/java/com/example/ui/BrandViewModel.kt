package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.BrandCampaign
import com.example.data.BrandCampaignRepository
import com.example.network.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class GenerationState {
    object Idle : GenerationState()
    object Generating : GenerationState()
    data class Success(val base64: String) : GenerationState()
    data class Error(val message: String) : GenerationState()
}

data class CampaignGenerationStatus(
    val productName: String = "",
    val productDescription: String = "",
    val brandTheme: String = "",
    val brandColors: String = "",
    val billboardState: GenerationState = GenerationState.Idle,
    val newspaperState: GenerationState = GenerationState.Idle,
    val socialPostState: GenerationState = GenerationState.Idle,
    val isSavingToHistory: Boolean = false,
    val errorSummary: String? = null
)

class BrandViewModel(
    application: Application,
    private val repository: BrandCampaignRepository
) : AndroidViewModel(application) {

    // Inputs
    val productName = MutableStateFlow("")
    val productDescription = MutableStateFlow("")
    val brandTheme = MutableStateFlow("Minimalist & Clean")
    val brandColors = MutableStateFlow("Emerald Green and Gold")

    // Generation State
    private val _generationStatus = MutableStateFlow(CampaignGenerationStatus())
    val generationStatus: StateFlow<CampaignGenerationStatus> = _generationStatus

    // Selection/Auditing State
    private val _selectedCampaignId = MutableStateFlow<Int?>(null)
    val selectedCampaignId: StateFlow<Int?> = _selectedCampaignId

    private val _currentViewingCampaign = MutableStateFlow<BrandCampaign?>(null)
    val currentViewingCampaign: StateFlow<BrandCampaign?> = _currentViewingCampaign

    // List of saved campaigns ordered by timestamp
    val allCampaigns: StateFlow<List<BrandCampaign>> = repository.allCampaigns
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun resetInputs() {
        productName.value = ""
        productDescription.value = ""
        brandTheme.value = "Minimalist & Clean"
        brandColors.value = "Emerald Green and Gold"
        _generationStatus.value = CampaignGenerationStatus()
        _selectedCampaignId.value = null
        _currentViewingCampaign.value = null
    }

    fun selectCampaign(campaign: BrandCampaign) {
        _selectedCampaignId.value = campaign.id
        _currentViewingCampaign.value = campaign
        
        // Sync inputs back if they want to edit or iterate
        productName.value = campaign.productName
        productDescription.value = campaign.productDescription
        brandTheme.value = campaign.brandTheme
        brandColors.value = campaign.brandColors

        _generationStatus.value = CampaignGenerationStatus(
            productName = campaign.productName,
            productDescription = campaign.productDescription,
            brandTheme = campaign.brandTheme,
            brandColors = campaign.brandColors,
            billboardState = campaign.billboardImageBase64?.let { GenerationState.Success(it) } ?: GenerationState.Idle,
            newspaperState = campaign.newspaperImageBase64?.let { GenerationState.Success(it) } ?: GenerationState.Idle,
            socialPostState = campaign.socialPostImageBase64?.let { GenerationState.Success(it) } ?: GenerationState.Idle
        )
    }

    fun deleteCampaign(campaign: BrandCampaign) {
        viewModelScope.launch {
            repository.deleteCampaignById(campaign.id)
            if (_selectedCampaignId.value == campaign.id) {
                resetInputs()
            }
        }
    }

    fun startCampaignGeneration() {
        val name = productName.value.trim()
        val desc = productDescription.value.trim()
        val theme = brandTheme.value
        val colors = brandColors.value.trim()

        if (name.isEmpty() || desc.isEmpty()) {
            _generationStatus.value = _generationStatus.value.copy(
                errorSummary = "Product Name and Description are required!"
            )
            return
        }

        _selectedCampaignId.value = null
        _currentViewingCampaign.value = null

        _generationStatus.value = CampaignGenerationStatus(
            productName = name,
            productDescription = desc,
            brandTheme = theme,
            brandColors = colors,
            billboardState = GenerationState.Generating,
            newspaperState = GenerationState.Generating,
            socialPostState = GenerationState.Generating,
            errorSummary = null
        )

        // Generate each medium in parallel
        val apiKey = BuildConfig.GEMINI_API_KEY
        val model = "gemini-2.5-flash-image"

        val consistencyGuide = "To preserve coherent product consistency and look, ensure the main product itself keeps its exact design language, material textures, primary shape, color tones, and brand markings. The product must look identical across all advertisement formats. Focus exclusively on the product's branding and aesthetic. There must be zero people, models, human features, hands, or silhouettes. Strictly no human beings."

        // 1. Billboard Prompt
        val billboardPrompt = "A premium outdoor architectural highway billboard advertisement showcasing: $desc. Brand theme is $theme styled in colors: $colors. The product is named '$name'. The poster is clean, professional, and visually dominating. $consistencyGuide"

        // 2. Newspaper Prompt
        val newspaperPrompt = "A sleek editorial newspaper print advertisement printed page showing: $desc. Set in elegant classic design typography with light newsprint textures, ink lines, and neat columns. Brand theme is $theme with palette $colors. Product name '$name' is prominently displayed. $consistencyGuide"

        // 3. Social Post Prompt
        val socialPostPrompt = "A studio-level lifestyle social media post showing: $desc. Minimal aesthetic composition with neat shadows, warm natural lighting, and elegant empty space. Brand theme is $theme styled in $colors. High quality catalog photography. Product name '$name' featured. $consistencyGuide"

        viewModelScope.launch {
            // Billboard Generation
            val billboardJob = async {
                generateSingleAsset(model, apiKey, billboardPrompt, "16:9")
            }

            // Newspaper Generation
            val newspaperJob = async {
                generateSingleAsset(model, apiKey, newspaperPrompt, "3:4")
            }

            // Social Post Generation
            val socialPostJob = async {
                generateSingleAsset(model, apiKey, socialPostPrompt, "1:1")
            }

            val billboardRes = billboardJob.await()
            val newspaperRes = newspaperJob.await()
            val socialPostRes = socialPostJob.await()

            _generationStatus.value = _generationStatus.value.copy(
                billboardState = billboardRes,
                newspaperState = newspaperRes,
                socialPostState = socialPostRes
            )

            // If all are successful, auto-save to Room database history
            if (billboardRes is GenerationState.Success &&
                newspaperRes is GenerationState.Success &&
                socialPostRes is GenerationState.Success) {
                
                _generationStatus.value = _generationStatus.value.copy(isSavingToHistory = true)
                val newCampaign = BrandCampaign(
                    productName = name,
                    productDescription = desc,
                    brandTheme = theme,
                    brandColors = colors,
                    billboardImageBase64 = billboardRes.base64,
                    newspaperImageBase64 = newspaperRes.base64,
                    socialPostImageBase64 = socialPostRes.base64,
                    billboardPrompt = billboardPrompt,
                    newspaperPrompt = newspaperPrompt,
                    socialPostPrompt = socialPostPrompt
                )
                val id = repository.insertCampaign(newCampaign)
                _selectedCampaignId.value = id.toInt()
                _currentViewingCampaign.value = newCampaign.copy(id = id.toInt())
                _generationStatus.value = _generationStatus.value.copy(isSavingToHistory = false)
            } else {
                val errorList = mutableListOf<String>()
                if (billboardRes is GenerationState.Error) errorList.add("Billboard failed: ${billboardRes.message}")
                if (newspaperRes is GenerationState.Error) errorList.add("Newspaper failed: ${newspaperRes.message}")
                if (socialPostRes is GenerationState.Error) errorList.add("Social Post failed: ${socialPostRes.message}")
                
                _generationStatus.value = _generationStatus.value.copy(
                    errorSummary = "Some campaign assets failed to generate. You can retry individual mediums below.\n${errorList.joinToString("\n")}"
                )
            }
        }
    }

    fun regenerateMedium(mediumName: String) {
        val name = productName.value.trim()
        val desc = productDescription.value.trim()
        val theme = brandTheme.value
        val colors = brandColors.value.trim()

        if (name.isEmpty() || desc.isEmpty()) return

        val apiKey = BuildConfig.GEMINI_API_KEY
        val model = "gemini-2.5-flash-image"

        val consistencyGuide = "To preserve coherent product consistency and look, ensure the main product itself keeps its exact design language, material textures, primary shape, color tones, and brand markings. The product must look identical across all advertisement formats. Focus exclusively on the product's branding and aesthetic. There must be zero people, models, human features, hands, or silhouettes. Strictly no human beings."

        viewModelScope.launch {
            when (mediumName.lowercase()) {
                "billboard" -> {
                    _generationStatus.value = _generationStatus.value.copy(billboardState = GenerationState.Generating, errorSummary = null)
                    val prompt = "A premium outdoor architectural highway billboard advertisement showcasing: $desc. Brand theme is $theme styled in colors: $colors. The product is named '$name'. The poster is clean, professional, and visually dominating. $consistencyGuide"
                    val result = generateSingleAsset(model, apiKey, prompt, "16:9")
                    _generationStatus.value = _generationStatus.value.copy(billboardState = result)
                    updateCurrentCampaign(mediumName, result, prompt)
                }
                "newspaper" -> {
                    _generationStatus.value = _generationStatus.value.copy(newspaperState = GenerationState.Generating, errorSummary = null)
                    val prompt = "A sleek editorial newspaper print advertisement printed page showing: $desc. Set in elegant classic design typography with light newsprint textures, ink lines, and neat columns. Brand theme is $theme with palette $colors. Product name '$name' is prominently displayed. $consistencyGuide"
                    val result = generateSingleAsset(model, apiKey, prompt, "3:4")
                    _generationStatus.value = _generationStatus.value.copy(newspaperState = result)
                    updateCurrentCampaign(mediumName, result, prompt)
                }
                "social post" -> {
                    _generationStatus.value = _generationStatus.value.copy(socialPostState = GenerationState.Generating, errorSummary = null)
                    val prompt = "A studio-level lifestyle social media post showing: $desc. Minimal aesthetic composition with neat shadows, warm natural lighting, and elegant empty space. Brand theme is $theme styled in $colors. High quality catalog photography. Product name '$name' featured. $consistencyGuide"
                    val result = generateSingleAsset(model, apiKey, prompt, "1:1")
                    _generationStatus.value = _generationStatus.value.copy(socialPostState = result)
                    updateCurrentCampaign(mediumName, result, prompt)
                }
            }
        }
    }

    private suspend fun updateCurrentCampaign(mediumName: String, result: GenerationState, prompt: String) {
        val campaign = _currentViewingCampaign.value ?: return
        if (result is GenerationState.Success) {
            val updated = when (mediumName.lowercase()) {
                "billboard" -> campaign.copy(billboardImageBase64 = result.base64, billboardPrompt = prompt)
                "newspaper" -> campaign.copy(newspaperImageBase64 = result.base64, newspaperPrompt = prompt)
                "social post" -> campaign.copy(socialPostImageBase64 = result.base64, socialPostPrompt = prompt)
                else -> campaign
            }
            repository.updateCampaign(updated)
            _currentViewingCampaign.value = updated
        }
    }

    private suspend fun generateSingleAsset(
        model: String,
        apiKey: String,
        prompt: String,
        aspectRatio: String
    ): GenerationState {
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseModalities = listOf("TEXT", "IMAGE"),
                imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = "512px"), // Use 512px for faster, highly reliable responses on Nano-Banana
                temperature = 1.0f
            )
        )

        try {
            val response = RetrofitClient.service.generateImageContent(model, apiKey, request)
            val extractedBase64 = extractBase64(response)
            return if (extractedBase64 != null) {
                GenerationState.Success(extractedBase64)
            } else {
                GenerationState.Error("Image generation response did not contain image data.")
            }
        } catch (e: Exception) {
            return GenerationState.Error(e.localizedMessage ?: "Network error occurred")
        }
    }

    private fun extractBase64(response: GenerateContentResponse): String? {
        val candidates = response.candidates ?: return null
        for (candidate in candidates) {
            val parts = candidate.content?.parts ?: continue
            for (part in parts) {
                val inlineData = part.inlineData ?: continue
                if (inlineData.mimeType.startsWith("image/") && !inlineData.data.isNullOrEmpty()) {
                    return inlineData.data
                }
            }
        }
        return null
    }
}

class BrandViewModelFactory(
    private val application: Application,
    private val repository: BrandCampaignRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrandViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BrandViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
