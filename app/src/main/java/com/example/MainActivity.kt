package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LinkSaathiApp
import com.example.ui.LinkSaathiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LinkSaathiViewModel = viewModel(
                factory = LinkSaathiViewModel.Factory(applicationContext)
            )
            LinkSaathiApp(viewModel = viewModel)
        }
    }
}
