package ru.profikrol.operator

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.rabbitmes.mobile.MobileMesViewModel
import com.rabbitmes.mobile.RabbitMesApp
import dagger.hilt.android.AndroidEntryPoint
import ru.profikrol.operator.uikit.theme.ProfikrolTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: MobileMesViewModel by viewModels()
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
        val isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        runOnUiThread { vm.setOnline(isOnline) }
    }
}
