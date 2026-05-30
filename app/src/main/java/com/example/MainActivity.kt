package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.WalletMainLayout
import com.example.ui.WalletViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val walletViewModel: WalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mandatory edge-to-edge setup for responsive notch calculations
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                WalletMainLayout(viewModel = walletViewModel)
            }
        }
    }
}
