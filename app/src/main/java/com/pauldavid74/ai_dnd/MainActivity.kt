package com.pauldavid74.ai_dnd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pauldavid74.ai_dnd.core.data.bootstrap.CampaignBootstrapper
import com.pauldavid74.ai_dnd.core.ui.theme.AiDnDTheme
import com.pauldavid74.ai_dnd.feature.game.GameScreen
import com.pauldavid74.ai_dnd.feature.game.charactersheet.CharacterSheetScreen
import com.pauldavid74.ai_dnd.feature.home.HomeScreen
import com.pauldavid74.ai_dnd.feature.pregame.character.CharacterCreationScreen
import com.pauldavid74.ai_dnd.feature.settings.AboutScreen
import com.pauldavid74.ai_dnd.feature.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var campaignBootstrapper: CampaignBootstrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Bootstrap campaigns from assets
        lifecycleScope.launch(Dispatchers.IO) {
            campaignBootstrapper.initializeCampaigns()
        }

        setContent {
            AiDnDTheme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onNewCampaign = { navController.navigate("character_creation") },
                                onContinueCampaign = { id -> navController.navigate("game/$id") },
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                        composable("character_creation") {
                            CharacterCreationScreen(
                                onSettingsClick = {
                                    navController.navigate("settings")
                                },
                                onComplete = { characterId ->
                                    navController.navigate("game/$characterId")
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToHome = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onAboutClick = { navController.navigate("about") }
                            )
                        }
                        composable("about") {
                            AboutScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = "game/{characterId}",
                            arguments = listOf(navArgument("characterId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val characterId = backStackEntry.arguments?.getLong("characterId") ?: return@composable
                            GameScreen(
                                characterId = characterId,
                                onSettingsClick = { navController.navigate("settings") },
                                onCharacterSheetClick = { navController.navigate("character_sheet/$characterId") }
                            )
                        }
                        composable(
                            route = "character_sheet/{characterId}",
                            arguments = listOf(navArgument("characterId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val characterId = backStackEntry.arguments?.getLong("characterId") ?: return@composable
                            CharacterSheetScreen(
                                characterId = characterId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
