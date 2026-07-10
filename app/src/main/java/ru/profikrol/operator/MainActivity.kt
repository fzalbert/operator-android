package ru.profikrol.operator

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
}
