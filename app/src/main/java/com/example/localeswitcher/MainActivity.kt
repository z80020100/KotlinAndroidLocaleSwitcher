package com.example.localeswitcher

import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.localeswitcher.ui.theme.LocaleSwitcherTheme

class MainActivity : ComponentActivity() {
    private var busy by mutableStateOf(false)
    private var permissionGranted by mutableStateOf(false)
    private var currentLanguage by mutableStateOf("")
    private var message by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocaleSwitcherTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                            .verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.switch_description))
                        Text(stringResource(R.string.current_language, currentLanguage))
                        if (!permissionGranted) Text(stringResource(R.string.permission_required))
                        Button(
                            onClick = { switchLanguage("en-US") },
                            enabled = permissionGranted && !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("English (United States)") }
                        Button(
                            onClick = { switchLanguage("ja-JP") },
                            enabled = permissionGranted && !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("日本語") }
                        if (busy) Text(stringResource(R.string.switching))
                        message?.let { Text(stringResource(it)) }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
        Log.i("LocaleSwitcher", "Screen resumed. requiredPermissionsGranted=$permissionGranted")
    }

    override fun onDestroy() {
        Log.i("LocaleSwitcher", "Screen destroyed. changingConfigurations=$isChangingConfigurations")
        super.onDestroy()
    }

    private fun refreshState() {
        permissionGranted = SystemLocaleSwitcher.hasPermission(this)
        currentLanguage = LocaleList.getDefault().toLanguageTags()
    }

    private fun switchLanguage(tag: String) {
        if (busy) {
            Log.i("LocaleSwitcher", "Button request ignored. A switch is in progress.")
            return
        }
        Log.i("LocaleSwitcher", "Language button pressed.")
        busy = true
        message = null
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    SystemLocaleSwitcher.switchTo(applicationContext, tag)
                }
                Log.i("LocaleSwitcher", "Switch result received. result=$result")
                message = when (result) {
                    SystemLocaleSwitcher.Result.UPDATED -> R.string.updated
                    SystemLocaleSwitcher.Result.PERMISSION_REQUIRED -> R.string.permission_required
                    SystemLocaleSwitcher.Result.FAILED -> R.string.switch_failed
                }
                refreshState()
            } finally {
                busy = false
            }
        }
    }
}
