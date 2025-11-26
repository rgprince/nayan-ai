package com.rgprince.nayanai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rgprince.nayanai.ui.screens.ChatScreen
import com.rgprince.nayanai.ui.screens.HomeScreen
import com.rgprince.nayanai.ui.screens.ModelManagerScreen
import com.rgprince.nayanai.ui.theme.NayanAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NayanAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NayanAIApp()
                }
            }
        }
    }
}

@Composable
fun NayanAIApp() {
    val navController = rememberNavController()
    var currentModelPath by remember { mutableStateOf<String?>(null) }
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToChat = { sessionId, modelPath ->
                    navController.navigate("chat/$sessionId")
                },
                onNavigateToModelManager = {
                    navController.navigate("model_manager")
                }
            )
        }
        
        composable("model_manager") {
            ModelManagerScreen(
                onNavigateToChat = { sessionId ->
                    navController.navigate("chat/$sessionId") {
                        popUpTo("home")
                    }
                }
            )
        }
        
        composable(
            route = "chat/{sessionId}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            ChatScreen(
                sessionId = sessionId,
                modelPath = currentModelPath,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
