package com.tabbatch.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.tabbatch.app.platform.sharing.ShareIntentParser
import com.tabbatch.app.ui.navigation.TabBatchNavHost
import com.tabbatch.app.ui.theme.TabBatchTheme

/** Single-activity Compose host. Also receives Android Sharesheet ACTION_SEND /
 * ACTION_SEND_MULTIPLE intents (see AndroidManifest.xml intent filters).
 *
 * The activity uses `launchMode="singleTask"`, so a share arriving while TabBatch is already
 * running (foreground or backgrounded) is delivered to [onNewIntent] rather than [onCreate].
 * Without handling that callback, a second share would silently do nothing — this is tracked
 * via a mutable state holder so new shares are picked up regardless of activity lifecycle state. */
class MainActivity : ComponentActivity() {

    private val sharedTextState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sharedTextState.value = ShareIntentParser.extractSharedText(intent)

        setContent {
            TabBatchTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TabBatchNavHost(initialSharedText = sharedTextState.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val sharedText = ShareIntentParser.extractSharedText(intent)
        if (!sharedText.isNullOrBlank()) {
            sharedTextState.value = sharedText
            recreate()
        }
    }
}
