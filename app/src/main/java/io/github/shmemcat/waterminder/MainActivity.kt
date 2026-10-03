package io.github.shmemcat.waterminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import io.github.shmemcat.waterminder.ui.WaterminderApp

class MainActivity : ComponentActivity() {
    private val model: WaterminderViewModel by viewModels()
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        model.refresh()
        if (granted) model.update(model.settings.copy(enabled = true))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent.action == DRANK_WATER) { model.drink(); intent.action = Intent.ACTION_MAIN }
        setContent {
            WaterminderApp(model, onAllowNotifications = {
                val needsPermission = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                if (needsPermission && (!getPreferences(MODE_PRIVATE).getBoolean("asked", false) || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))) {
                    getPreferences(MODE_PRIVATE).edit().putBoolean("asked", true).apply()
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else openNotificationSettings()
            }, onNotificationSettings = { openNotificationSettings() })
        }
    }
    override fun onResume() { super.onResume(); model.refresh() }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == DRANK_WATER) { model.drink(); intent.action = Intent.ACTION_MAIN }
    }
    private fun openNotificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }
    companion object { const val DRANK_WATER = "io.github.shmemcat.waterminder.DRANK_WATER" }
}
