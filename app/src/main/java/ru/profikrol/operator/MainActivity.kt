package ru.profikrol.operator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import ru.profikrol.operator.navigation.AppNavGraph
import ru.profikrol.operator.uikit.theme.ProfikrolTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProfikrolTheme {
                AppNavGraph()
            }
        }
    }
}
