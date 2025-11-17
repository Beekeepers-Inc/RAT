package com.rats.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rats.editor.SortColumn
import com.rats.import.ImportProgress
import com.rats.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun MainWindow(appState: AppState) {
    val coroutineScope = rememberCoroutineScope()

    MaterialTheme(
        colorScheme = lightColorScheme()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                AppToolbar(
                    onImportClick = {
                        coroutineScope.launch {
                            handleImport(appState)
                        }
                    },
                    onSortClick = {
                        appState.showSortDialog = true
                    },
                    onFilterClick = {
                        appState.showFilterDialog = true
                    },
                    onStatsClick = {
                        coroutineScope.launch {
                            calculateStatistics(appState)
                        }
                    },
                    onSaveClick = {
                        coroutineScope.launch {
                            performSave(appState)
                        }
                    },
                    onSaveAsClick = {
                        coroutineScope.launch {
                            performSaveAs(appState)
                        }
                    },
                    onExportClick = {
                        appState.showExportDialog = true
                    },
                    onAddRowClick = {
                        coroutineScope.launch {
                            performAddRow(appState)
                        }
                    },
                    onResetClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                appState.resetFilter()
                            }
                        }
                    },
                    hasData = appState.currentData != null,
                    hasFilePath = appState.selectedFilePath != null,
                    isFiltered = appState.isFiltered
                )

                // Main content area
                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    if (appState.currentData != null) {
                        DataGrid(
                            data = appState.currentData!!,
                            onCellEdit = { rowIndex, colIndex, newValue ->
                                coroutineScope.launch {
                                    performCellEdit(appState, rowIndex, colIndex, newValue)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        EmptyDataGrid(modifier = Modifier.fillMaxSize())
                    }
                }

                // Status bar
                StatusBar(message = appState.statusMessage)
            }

            // Loading overlay
            if (appState.isLoading) {
                LoadingOverlay(
                    message = appState.loadingMessage,
                    progress = appState.importProgress?.rowsImported,
                    totalRows = appState.importProgress?.totalRows
                )
            }

            // Dialogs
            if (appState.showPreviewDialog && appState.previewData != null && appState.selectedFilePath != null) {
                PreviewDialog(
                    previewData = appState.previewData!!,
                    fileName = File(appState.selectedFilePath!!).name,
                    onConfirm = {
                        appState.showPreviewDialog = false
                        coroutineScope.launch {
                            performImport(appState)
                        }
                    },
                    onDismiss = {
                        appState.showPreviewDialog = false
                        appState.previewData = null
                        appState.selectedFilePath = null
                    }
                )
            }

            if (appState.showSortDialog && appState.currentData != null) {
                SortDialog(
                    columns = appState.currentData!!.columns,
                    onConfirm = { columnName, ascending ->
                        appState.showSortDialog = false
                        coroutineScope.launch {
                            performSort(appState, columnName, ascending)
                        }
                    },
                    onDismiss = {
                        appState.showSortDialog = false
                    }
                )
            }

            if (appState.showFilterDialog && appState.currentData != null) {
                FilterDialog(
                    columns = appState.currentData!!.columns,
                    currentFilters = appState.filterConditions,
                    onConfirm = { filters ->
                        appState.showFilterDialog = false
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                appState.applyFilter(filters)
                            }
                        }
                    },
                    onDismiss = {
                        appState.showFilterDialog = false
                    }
                )
            }

            if (appState.showStatsDialog && appState.tableStatistics != null) {
                StatisticsDialog(
                    stats = appState.tableStatistics!!,
                    onDismiss = {
                        appState.showStatsDialog = false
                    }
                )
            }

            if (appState.showExportDialog && appState.currentTable != null) {
                ExportDialog(
                    onConfirm = { format, includeHeader, sheetName ->
                        appState.showExportDialog = false
                        coroutineScope.launch {
                            performExport(appState, format, includeHeader, sheetName)
                        }
                    },
                    onDismiss = {
                        appState.showExportDialog = false
                    }
                )
            }

            // Error dialog
            appState.errorMessage?.let { error ->
                ErrorDialog(
                    message = error,
                    onDismiss = { appState.clearError() }
                )
            }
        }
    }
}

private suspend fun handleImport(appState: AppState) {
    // Run file chooser on Swing EDT
    val filePath = suspendCoroutine<String?> { continuation ->
        SwingUtilities.invokeLater {
            val fileChooser = JFileChooser().apply {
                dialogTitle = "Select File to Import"
                fileSelectionMode = JFileChooser.FILES_ONLY
                isAcceptAllFileFilterUsed = false
                addChoosableFileFilter(
                    FileNameExtensionFilter(
                        "Data Files (*.csv, *.xlsx, *.xls)",
                        "csv", "xlsx", "xls", "xlsm"
                    )
                )
            }

            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                continuation.resume(fileChooser.selectedFile.absolutePath)
            } else {
                continuation.resume(null)
            }
        }
    }

    if (filePath != null) {
        appState.selectedFilePath = filePath

        withContext(Dispatchers.IO) {
            try {
                appState.statusMessage = "Loading preview..."
                val preview = appState.importer.previewFile(filePath)
                appState.previewData = preview
                appState.showPreviewDialog = true
            } catch (e: Exception) {
                appState.errorMessage = "Failed to preview file: ${e.message}"
            }
        }
    }
}

private suspend fun performImport(appState: AppState) {
    val filePath = appState.selectedFilePath ?: return

    appState.isLoading = true
    appState.loadingMessage = "Cleaning up previous data..."
    appState.importProgress = ImportProgress(0, null, "Cleaning up...")

    withContext(Dispatchers.IO) {
        try {
            // Clean up existing table before importing
            appState.cleanupCurrentTable()

            appState.loadingMessage = "Importing file..."
            appState.importProgress = ImportProgress(0, null, "Starting import...")

            val result = appState.importer.importFile(filePath) { progress ->
                appState.importProgress = progress
                appState.loadingMessage = progress.status
            }

            if (result.success) {
                appState.loadTableData(result.tableName)
                appState.statusMessage = result.message
            } else {
                appState.errorMessage = result.message
            }
        } catch (e: Exception) {
            appState.errorMessage = "Import failed: ${e.message}"
        } finally {
            appState.isLoading = false
            appState.importProgress = null
            appState.previewData = null
            // Keep selectedFilePath for Save functionality
        }
    }
}

private suspend fun performSort(appState: AppState, columnName: String, ascending: Boolean) {
    val tableName = appState.currentTable ?: return

    appState.isLoading = true
    appState.loadingMessage = "Sorting data..."

    withContext(Dispatchers.IO) {
        try {
            val sortColumn = SortColumn(columnName, ascending)
            val result = appState.editor.reorderRows(tableName, listOf(sortColumn))

            if (result.success) {
                appState.loadTableData(tableName)
                appState.statusMessage = result.message
            } else {
                appState.errorMessage = result.message
            }
        } catch (e: Exception) {
            appState.errorMessage = "Sort failed: ${e.message}"
        } finally {
            appState.isLoading = false
        }
    }
}

private suspend fun calculateStatistics(appState: AppState) {
    val tableName = appState.currentTable ?: return

    appState.isLoading = true
    appState.loadingMessage = "Calculating statistics..."

    withContext(Dispatchers.IO) {
        try {
            val stats = appState.statistics.getTableStatistics(tableName)
            appState.tableStatistics = stats
            appState.showStatsDialog = true
        } catch (e: Exception) {
            appState.errorMessage = "Statistics calculation failed: ${e.message}"
        } finally {
            appState.isLoading = false
        }
    }
}

private suspend fun performExport(
    appState: AppState,
    format: String,
    includeHeader: Boolean,
    sheetName: String
) {
    val tableName = appState.currentTable ?: return

    // Show save dialog on Swing EDT
    val filePath = suspendCoroutine<String?> { continuation ->
        SwingUtilities.invokeLater {
            val fileChooser = JFileChooser().apply {
                dialogTitle = "Export Data"
                fileSelectionMode = JFileChooser.FILES_ONLY
                val extension = if (format == "excel") "xlsx" else "csv"
                selectedFile = File("$tableName.$extension")
            }

            val result = fileChooser.showSaveDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                continuation.resume(fileChooser.selectedFile.absolutePath)
            } else {
                continuation.resume(null)
            }
        }
    }

    if (filePath == null) return

    appState.isLoading = true
    appState.loadingMessage = "Exporting data..."

    withContext(Dispatchers.IO) {
        try {
            val result = if (format == "excel") {
                appState.exporter.exportToExcel(tableName, filePath, sheetName)
            } else {
                appState.exporter.exportToCsv(tableName, filePath, includeHeader)
            }

            if (result.success) {
                appState.statusMessage = result.message
            } else {
                appState.errorMessage = result.message
            }
        } catch (e: Exception) {
            appState.errorMessage = "Export failed: ${e.message}"
        } finally {
            appState.isLoading = false
        }
    }
}

private suspend fun performSave(appState: AppState) {
    val tableName = appState.currentTable ?: return
    val filePath = appState.selectedFilePath ?: return

    appState.isLoading = true
    appState.loadingMessage = "Saving file..."

    withContext(Dispatchers.IO) {
        try {
            val extension = filePath.substringAfterLast('.', "csv").lowercase()
            val result = if (extension in listOf("xlsx", "xls", "xlsm")) {
                appState.exporter.exportToExcel(tableName, filePath, "Data")
            } else {
                appState.exporter.exportToCsv(tableName, filePath, true)
            }

            if (result.success) {
                appState.statusMessage = "Saved ${result.rowsExported} rows to $filePath"
            } else {
                appState.errorMessage = result.message
            }
        } catch (e: Exception) {
            appState.errorMessage = "Save failed: ${e.message}"
        } finally {
            appState.isLoading = false
        }
    }
}

private suspend fun performSaveAs(appState: AppState) {
    val tableName = appState.currentTable ?: return

    // Show save dialog on Swing EDT
    val filePath = suspendCoroutine<String?> { continuation ->
        SwingUtilities.invokeLater {
            val fileChooser = JFileChooser().apply {
                dialogTitle = "Save As"
                fileSelectionMode = JFileChooser.FILES_ONLY
                selectedFile = File("$tableName.csv")
            }

            val result = fileChooser.showSaveDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                continuation.resume(fileChooser.selectedFile.absolutePath)
            } else {
                continuation.resume(null)
            }
        }
    }

    if (filePath == null) return

    appState.isLoading = true
    appState.loadingMessage = "Saving file..."

    withContext(Dispatchers.IO) {
        try {
            val extension = filePath.substringAfterLast('.', "csv").lowercase()
            val result = if (extension in listOf("xlsx", "xls", "xlsm")) {
                appState.exporter.exportToExcel(tableName, filePath, "Data")
            } else {
                appState.exporter.exportToCsv(tableName, filePath, true)
            }

            if (result.success) {
                // Update the selected file path to the new location
                appState.selectedFilePath = filePath
                appState.statusMessage = "Saved ${result.rowsExported} rows to $filePath"
            } else {
                appState.errorMessage = result.message
            }
        } catch (e: Exception) {
            appState.errorMessage = "Save As failed: ${e.message}"
        } finally {
            appState.isLoading = false
        }
    }
}

private suspend fun performCellEdit(
    appState: AppState,
    rowIndex: Int,
    colIndex: Int,
    newValue: String
) {
    val tableName = appState.currentTable ?: return
    val columns = appState.currentData?.columns ?: return

    if (colIndex >= columns.size) return
    val columnName = columns[colIndex]

    withContext(Dispatchers.IO) {
        try {
            val result = appState.editor.updateCell(tableName, rowIndex, columnName, newValue)

            if (result.success) {
                // Reload data to reflect the change
                appState.loadTableData(tableName)
                appState.statusMessage = "Updated cell at row ${rowIndex + 1}, column '$columnName'"
            } else {
                appState.errorMessage = result.message
            }
        } catch (e: Exception) {
            appState.errorMessage = "Cell update failed: ${e.message}"
        }
    }
}

private suspend fun performAddRow(appState: AppState) {
    val tableName = appState.currentTable ?: return

    appState.isLoading = true
    appState.loadingMessage = "Adding new row..."

    withContext(Dispatchers.IO) {
        try {
            val result = appState.editor.addEmptyRow(tableName)

            if (result.success) {
                appState.loadTableData(tableName)
                appState.statusMessage = "Added new row (total: ${result.rowsAffected} rows)"
            } else {
                appState.errorMessage = result.message
            }
        } catch (e: Exception) {
            appState.errorMessage = "Add row failed: ${e.message}"
        } finally {
            appState.isLoading = false
        }
    }
}
