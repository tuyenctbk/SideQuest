package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AuthScreen
import com.example.ui.MainFeatureScreen
import com.example.ui.SideQuestSplashScreen
import com.example.ui.theme.SideQuestTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        // STEP 2: Initialize UI
        setContent {
            SideQuestTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    Crossfade(
        targetState = showSplash,
        animationSpec = tween(durationMillis = 380),
        label = "splash_crossfade"
    ) { isSplash ->
        if (isSplash) {
            SideQuestSplashScreen(
                onSplashFinished = { showSplash = false }
            )
        } else {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            MainFeatureScreen(
                currentUserId = user.uid,
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Wanderer"
            )
        }
    }
}
