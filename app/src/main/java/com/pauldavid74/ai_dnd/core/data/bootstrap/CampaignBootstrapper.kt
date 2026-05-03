package com.pauldavid74.ai_dnd.core.data.bootstrap

import android.app.Application
import android.content.AssetManager
import android.util.Log
import com.pauldavid74.ai_dnd.core.data.repository.GameRepository
import com.pauldavid74.ai_dnd.core.domain.model.CampaignImportPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignBootstrapper @Inject constructor(
    private val application: Application,
    private val gameRepository: GameRepository
) {

    private val assetManager: AssetManager = application.assets

    suspend fun initializeCampaigns() {
        withContext(Dispatchers.IO) {
            try {
                val assetNames = assetManager.list("") ?: return@withContext
                val jsonFiles = assetNames.filter { it.endsWith(".json", ignoreCase = true) }

                Log.d("CampaignBootstrapper", "Found ${jsonFiles.size} JSON asset(s) to process")

                for (fileName in jsonFiles) {
                    processAssetFile(fileName)
                }
            } catch (e: Exception) {
                Log.e("CampaignBootstrapper", "Failed to list assets", e)
            }
        }
    }

    private suspend fun processAssetFile(fileName: String) {
        try {
            assetManager.open(fileName).use { inputStream ->
                val jsonString = inputStream.readBytes().decodeToString()
                val payload = Json.decodeFromString<CampaignImportPayload>(jsonString)
                gameRepository.importExternalCampaign(payload)
                Log.i("CampaignBootstrapper", "Successfully processed asset: $fileName")
            }
        } catch (e: Exception) {
            Log.e("CampaignBootstrapper", "Failed to process asset $fileName", e)
        }
    }
}