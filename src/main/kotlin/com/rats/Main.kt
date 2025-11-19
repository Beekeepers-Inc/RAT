package com.rats

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.rats.ui.AppState
import com.rats.ui.MainWindow

fun main() = application {
    var initError by remember { mutableStateOf<String?>(null) }
    val appState = remember {
        try {
            AppState()
        } catch (e: Exception) {
            initError = "Failed to initialize application:\n\n${e.message}\n\nPlease ensure all required files are present and you have write permissions to the application directory."
            null
        }
    }

    val windowState = rememberWindowState(
        size = DpSize(1200.dp, 800.dp)
    )

    Window(
        onCloseRequest = {
            appState?.close()
            exitApplication()
        },
        title = "RATS - Data Analysis Tool",
        state = windowState
    ) {
        if (initError != null) {
            // Show error dialog if initialization failed
            AlertDialog(
                onDismissRequest = { exitApplication() },
                title = { Text("Initialization Error") },
                text = { Text(initError!!) },
                confirmButton = {
                    Button(onClick = { exitApplication() }) {
                        Text("Exit")
                    }
                }
            )
        } else if (appState != null) {
            MainWindow(appState)
        }
    }
}
