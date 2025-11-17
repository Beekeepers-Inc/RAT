package com.rats.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rats.core.DuckDBCore
import com.rats.core.QueryResult
import com.rats.core.TableInfo
import com.rats.editor.DataEditor
import com.rats.export.DataExporter
import com.rats.import.FileImporter
import com.rats.import.ImportProgress
import com.rats.import.PreviewData
import com.rats.statistics.StatisticsCalculator
import com.rats.statistics.TableStatistics

data class FilterCondition(
    val column: String,
    val operator: String,
    val value: String
)

class AppState {
    val db = DuckDBCore()
    val importer = FileImporter(db)
    val editor = DataEditor(db)
    val statistics = StatisticsCalculator(db)
    val exporter = DataExporter(db)

    var currentTable by mutableStateOf<String?>(null)
    var originalTable by mutableStateOf<String?>(null)
    var currentData by mutableStateOf<QueryResult?>(null)
    var currentTableInfo by mutableStateOf<TableInfo?>(null)

    var isLoading by mutableStateOf(false)
    var loadingMessage by mutableStateOf("Loading...")
    var importProgress by mutableStateOf<ImportProgress?>(null)

    var showImportDialog by mutableStateOf(false)
    var showSortDialog by mutableStateOf(false)
    var showPreviewDialog by mutableStateOf(false)
    var showStatsDialog by mutableStateOf(false)
    var showExportDialog by mutableStateOf(false)
    var showFilterDialog by mutableStateOf(false)

    var previewData by mutableStateOf<PreviewData?>(null)
    var selectedFilePath by mutableStateOf<String?>(null)

    var tableStatistics by mutableStateOf<TableStatistics?>(null)
    var filterConditions by mutableStateOf<List<FilterCondition>>(emptyList())
    var isFiltered by mutableStateOf(false)

    var statusMessage by mutableStateOf("Ready")
    var errorMessage by mutableStateOf<String?>(null)

    fun cleanupCurrentTable() {
        try {
            // Drop current table if exists
            if (currentTable != null) {
                try {
                    db.execute("DROP TABLE IF EXISTS $currentTable")
                } catch (_: Exception) {
                    // Might be a view, try dropping view
                    try {
                        db.execute("DROP VIEW IF EXISTS $currentTable")
                    } catch (_: Exception) {
                    }
                }
            }

            // Drop original table if exists (for filtered views)
            if (originalTable != null) {
                try {
                    db.execute("DROP TABLE IF EXISTS $originalTable")
                } catch (_: Exception) {
                }
            }

            // Reset state
            currentTable = null
            originalTable = null
            currentData = null
            currentTableInfo = null
            tableStatistics = null
            isFiltered = false
            filterConditions = emptyList()
        } catch (e: Exception) {
            // Log but don't fail
            println("Cleanup warning: ${e.message}")
        }
    }

    fun loadTableData(tableName: String, limit: Int = Int.MAX_VALUE, offset: Int = 0) {
        try {
            currentTableInfo = db.getTableInfo(tableName)
            val actualLimit = if (limit == Int.MAX_VALUE) currentTableInfo?.rowCount?.toInt() ?: 100000 else limit
            currentData = db.queryData(tableName, actualLimit, offset)
            currentTable = tableName
            statusMessage = "Loaded ${currentData?.rows?.size ?: 0} of ${currentTableInfo?.rowCount ?: 0} rows from $tableName"
        } catch (e: Exception) {
            errorMessage = "Failed to load table: ${e.message}"
        }
    }

    fun applyFilter(conditions: List<FilterCondition>) {
        if (conditions.isEmpty()) return

        try {
            // Store original table if not already stored
            if (originalTable == null) {
                originalTable = currentTable
            }

            val sourceTable = originalTable ?: currentTable ?: return

            // Build WHERE clause
            val whereClauses = conditions.map { condition ->
                val valueStr = when {
                    condition.value.toDoubleOrNull() != null -> condition.value
                    condition.value.equals("true", ignoreCase = true) -> "true"
                    condition.value.equals("false", ignoreCase = true) -> "false"
                    else -> "'${condition.value.replace("'", "''")}'"
                }

                when (condition.operator.uppercase()) {
                    "IN" -> "\"${condition.column}\" IN ($valueStr)"
                    "LIKE" -> "\"${condition.column}\" LIKE $valueStr"
                    else -> "\"${condition.column}\" ${condition.operator} $valueStr"
                }
            }

            val whereClause = whereClauses.joinToString(" AND ")
            val filteredViewName = "${sourceTable}_filtered"

            // Drop existing view if exists
            try {
                db.execute("DROP VIEW IF EXISTS $filteredViewName")
            } catch (_: Exception) {
            }

            // Create filtered view
            val createQuery = "CREATE VIEW $filteredViewName AS SELECT * FROM $sourceTable WHERE $whereClause"
            db.execute(createQuery)

            currentTable = filteredViewName
            filterConditions = conditions
            isFiltered = true

            loadTableData(filteredViewName)
        } catch (e: Exception) {
            errorMessage = "Filter failed: ${e.message}"
        }
    }

    fun resetFilter() {
        if (!isFiltered || originalTable == null) return

        try {
            currentTable = originalTable
            loadTableData(originalTable!!)

            isFiltered = false
            filterConditions = emptyList()
            originalTable = null

            statusMessage = "Filter cleared - showing all ${currentTableInfo?.rowCount ?: 0} rows"
        } catch (e: Exception) {
            errorMessage = "Reset failed: ${e.message}"
        }
    }

    fun clearError() {
        errorMessage = null
    }

    fun close() {
        db.close()
    }
}
