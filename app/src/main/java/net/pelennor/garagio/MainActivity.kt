package net.pelennor.garagio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import net.pelennor.garagio.ui.GaragioApp
import net.pelennor.garagio.ui.GaragioViewModel
import net.pelennor.garagio.ui.theme.GaragioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaragioTheme {
                val viewModel: GaragioViewModel = viewModel(factory = GaragioViewModel.Factory)
                GaragioApp(viewModel)
            }
        }
    }
}
