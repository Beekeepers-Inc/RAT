package com.rats

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.rats.ui.AppState
import com.rats.ui.MainWindow

fun main() = application {
    val appState = remember { AppState() }

    val windowState = rememberWindowState(
        size = DpSize(1200.dp, 800.dp)
    )

    Window(
        onCloseRequest = {
            appState.close()
            exitApplication()
        },
        title = "RATS - Data Analysis Tool",
        state = windowState
    ) {
        MainWindow(appState)
    }
}
