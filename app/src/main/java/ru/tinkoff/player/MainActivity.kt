@file:OptIn(ExperimentalPermissionsApi::class, ExperimentalTime::class)

package ru.tinkoff.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import ru.tinkoff.player.screen.PlayerScreen
import kotlin.time.ExperimentalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dependencies = Dependencies(this)
        setContent {
            PlayerScreen(dependencies)
        }
    }
}