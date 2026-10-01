package ru.profikrol.operator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rabbitmes.mobile.ui.viewmodel.AppViewModel
import com.rabbitmes.mobile.RabbitMesApp
import com.rabbitmes.mobile.notifications.SystemNotifications
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    @Inject lateinit var sessionStore: SessionStore
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { requestBatteryOptimizationExemption() }
    private val connectivityManager: ConnectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateOnlineState()
        }

        override fun onLost(network: Network) {
            updateOnlineState()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateOnlineState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProfikrolTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    RabbitMesApp(vm)
                }
            }
        }
        openNotificationsIfRequested(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sessionStore.user.filterNotNull().first()
                requestBackgroundNotificationAccess()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openNotificationsIfRequested(intent)
    }

    private fun openNotificationsIfRequested(intent: Intent?) {
        if (intent?.getBooleanExtra(SystemNotifications.EXTRA_OPEN_NOTIFICATIONS, false) != true) return
        intent.removeExtra(SystemNotifications.EXTRA_OPEN_NOTIFICATIONS)
        vm.requestOpenNotifications()
    }

    /**
     * Уведомления приходят в фоне, только если их разрешено показывать
     * и система не душит соединение экономией батареи. Спрашиваем один раз.
     */
    private fun requestBackgroundNotificationAccess() {
        val preferences = getSharedPreferences(PERMISSIONS_PREFERENCES, MODE_PRIVATE)
        if (preferences.getBoolean(KEY_ASKED, false)) return
        preferences.edit().putBoolean(KEY_ASKED, true).apply()
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestBatteryOptimizationExemption()
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return
        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")),
            )
        }.onFailure { Log.w(TAG, "Battery optimization exemption request is unavailable", it) }
    }

    override fun onStart() {
        super.onStart()
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        updateOnlineState()
    }

    override fun onStop() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        super.onStop()
    }

    private fun updateOnlineState() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let(connectivityManager::getNetworkCapabilities)
        val isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        runOnUiThread { vm.setOnline(isOnline) }
    }

    private companion object {
        const val TAG = "MainActivity"
        const val PERMISSIONS_PREFERENCES = "background_notifications"
        const val KEY_ASKED = "asked"
    }
}
