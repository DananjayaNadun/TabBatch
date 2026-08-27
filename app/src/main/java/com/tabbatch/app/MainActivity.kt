package com.tabbatch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tabbatch.app.platform.sharing.ShareIntentParser
import com.tabbatch.app.ui.navigation.TabBatchNavHost
import com.tabbatch.app.ui.theme.TabBatchTheme

/** Single-activity Compose host. Also receives Android Sharesheet ACTION_SEND /
 * ACTION_SEND_MULTIPLE intents (see AndroidManifest.xml intent filters). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText = ShareIntentParser.extractSharedText(intent)

        setContent {
            TabBatchTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TabBatchNavHost(initialSharedText = sharedText)
                }
            }
        }
    }
}
