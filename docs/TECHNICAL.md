# RAT Technical Documentation

This document provides detailed technical information about RAT's implementation, APIs, and development practices.

## Technology Stack

| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| UI Framework | Compose Desktop | 1.5.11 | Declarative UI |
| Language | Kotlin | 1.9.21 | JVM-based language |
| Database | DuckDB | 1.1.3 | Analytical queries |
| Excel | Apache POI | 5.2.5 | XLSX read/write |
| CSV Preview | OpenCSV | 5.9 | CSV parsing |
| Async | Coroutines | 1.7.3 | Async operations |
| Build | Gradle | 8.4 | Build system |

## Project Structure

```
rat/
├── src/main/kotlin/com/rats/
│   ├── Main.kt                              # Entry point
│   ├── core/
│   │   └── DuckDBCore.kt                    # Database layer
│   ├── import/
│   │   └── FileImporter.kt                  # File import logic
│   ├── editor/
│   │   └── DataEditor.kt                    # Data manipulation
│   ├── statistics/
│   │   └── Statistics.kt                    # Statistical calculations
│   ├── export/
│   │   └── DataExporter.kt                  # Data export
│   └── ui/
│       ├── AppState.kt                      # State management
│       ├── MainWindow.kt                    # Main window
│       └── components/
│           ├── DataGrid.kt                  # Grid component
│           ├── Dialogs.kt                   # Dialog components
│           └── Toolbar.kt                   # Toolbar component
├── build.gradle.kts                         # Build configuration
├── settings.gradle.kts                      # Project settings
├── gradle.properties                        # Gradle properties
└── docs/
    ├── README.md                            # Overview
    ├── ARCHITECTURE.md                      # Architecture
    └── TECHNICAL.md                         # This file
```

## Core APIs

### DuckDBCore

```kotlin
class DuckDBCore : AutoCloseable {
    // Execute any SQL query
    fun executeQuery(query: String): QueryResult

    // Query table data with pagination
    fun queryData(tableName: String, limit: Int, offset: Int): QueryResult

    // Get table metadata
    fun getTableInfo(tableName: String): TableInfo

    // Execute non-query SQL (CREATE, DROP, INSERT, etc.)
    fun execute(sql: String)

    // Execute multiple statements in batch
    fun executeBatch(sqlStatements: List<String>)

    // Get raw JDBC connection
    fun getConnection(): DuckDBConnection

    // Sanitize table/column names
    companion object {
        fun sanitizeTableName(name: String): String
    }
}
```

### FileImporter

```kotlin
class FileImporter(db: DuckDBCore) {
    // Detect file format from extension
    fun detectFileFormat(filePath: String): FileFormat

    // Generate preview (first N rows)
    fun previewFile(filePath: String, maxRows: Int = 10): PreviewData

    // Import file with progress callbacks
    fun importFile(
        filePath: String,
        tableName: String? = null,
        onProgress: ((ImportProgress) -> Unit)? = null
    ): ImportResult
}

// File formats supported
enum class FileFormat { CSV, EXCEL, UNKNOWN }

// Progress tracking
data class ImportProgress(
    val rowsImported: Int,
    val totalRows: Int?,
    val status: String
)

// Import result
data class ImportResult(
    val tableName: String,
    val rowsImported: Int,
    val columnsCount: Int,
    val success: Boolean,
    val message: String
)
```

### DataEditor

```kotlin
class DataEditor(db: DuckDBCore) {
    // Sort table by columns
    fun reorderRows(tableName: String, sortColumns: List<SortColumn>): ReorderResult

    // Filter data with SQL expression
    fun filterData(tableName: String, filterExpression: String, newTableName: String?): ReorderResult

    // Delete a column
    fun deleteColumn(tableName: String, columnName: String): ReorderResult

    // Add a new column
    fun addColumn(tableName: String, columnName: String, columnType: String, defaultValue: String?): ReorderResult

    // Update a specific cell (inline editing)
    fun updateCell(tableName: String, rowIndex: Int, columnName: String, newValue: String): ReorderResult

    // Delete multiple rows
    fun deleteRows(tableName: String, rowIndices: List<Int>): ReorderResult

    // Add a new empty row with default values
    fun addEmptyRow(tableName: String): ReorderResult
}

data class SortColumn(val columnName: String, val ascending: Boolean = true)

data class ReorderResult(
    val success: Boolean,
    val message: String,
    val rowsAffected: Int
)
```

### DataGrid (Editable)

```kotlin
@Composable
fun DataGrid(
    data: QueryResult,
    onCellEdit: ((rowIndex: Int, columnIndex: Int, newValue: String) -> Unit)? = null,
    modifier: Modifier = Modifier
)

// Inline editing behavior:
// - Click cell to enter edit mode
// - Type new value
// - Click outside or press Tab to save changes
// - Changes are immediately persisted to DuckDB
```

### StatisticsCalculator

```kotlin
class StatisticsCalculator(db: DuckDBCore) {
    // Get comprehensive table statistics
    fun getTableStatistics(tableName: String): TableStatistics

    // Aggregate single column
    fun aggregateColumn(tableName: String, columnName: String, function: String): AggregationResult

    // Calculate correlation between two columns
    fun calculateCorrelation(tableName: String, columnX: String, columnY: String): Double
}

data class TableStatistics(
    val tableName: String,
    val totalRows: Long,
    val totalColumns: Int,
    val columnStats: List<ColumnStatistics>
)

data class ColumnStatistics(
    val columnName: String,
    val dataType: String,
    val count: Long,
    val nullCount: Long,
    val distinctCount: Long,
    val min: Any?,
    val max: Any?,
    val mean: Double?,
    val median: Double?,
    val stdDev: Double?,
    val variance: Double?,
    val q25: Double?,
    val q75: Double?
)
```

### DataExporter

```kotlin
class DataExporter(db: DuckDBCore) {
    // Export to CSV using DuckDB COPY
    fun exportToCsv(tableName: String, filePath: String, includeHeader: Boolean = true): ExportResult

    // Export to Excel using Apache POI
    fun exportToExcel(tableName: String, filePath: String, sheetName: String = "Data"): ExportResult

    // Export query results to CSV
    fun exportQueryToCsv(query: String, filePath: String, includeHeader: Boolean = true): ExportResult
}

data class ExportResult(
    val success: Boolean,
    val message: String,
    val filePath: String,
    val rowsExported: Int
)
```

### AppState

```kotlin
class AppState {
    // Services
    val db: DuckDBCore
    val importer: FileImporter
    val editor: DataEditor
    val statistics: StatisticsCalculator
    val exporter: DataExporter

    // Reactive state (UI automatically updates when these change)
    var currentTable: String?
    var currentData: QueryResult?
    var currentTableInfo: TableInfo?
    var isLoading: Boolean
    var loadingMessage: String
    var importProgress: ImportProgress?
    var showImportDialog: Boolean
    var showSortDialog: Boolean
    var showPreviewDialog: Boolean
    var showStatsDialog: Boolean
    var showExportDialog: Boolean
    var showFilterDialog: Boolean
    var previewData: PreviewData?
    var selectedFilePath: String?
    var tableStatistics: TableStatistics?
    var filterConditions: List<FilterCondition>
    var isFiltered: Boolean
    var statusMessage: String
    var errorMessage: String?

    // Operations
    fun cleanupCurrentTable()
    fun loadTableData(tableName: String, limit: Int = Int.MAX_VALUE, offset: Int = 0)
    fun applyFilter(conditions: List<FilterCondition>)
    fun resetFilter()
    fun clearError()
    fun close()
}

data class FilterCondition(
    val column: String,
    val operator: String,  // =, !=, >, <, >=, <=, LIKE
    val value: String
)
```

## Build Commands

```bash
# Development
./gradlew run                    # Run application
./gradlew compileKotlin          # Compile only
./gradlew clean                  # Clean build artifacts

# Testing
./gradlew test                   # Run all tests
./gradlew test --tests "TestClass"  # Run specific test

# Distribution
./gradlew packageDmg             # macOS DMG
./gradlew packageMsi             # Windows MSI
./gradlew packageDeb             # Linux DEB
./gradlew package                # All formats

# Code quality
./gradlew ktlintCheck            # Check code style (if configured)
```

## Configuration

### DuckDB Settings (DuckDBCore.kt:33-35)
```kotlin
stmt.execute("SET memory_limit='4GB'")  // Max memory usage
stmt.execute("SET threads=4")           // Parallel threads
```

### Gradle Properties (gradle.properties)
```properties
org.gradle.jvmargs=-Xmx4096m    # Gradle JVM memory
kotlin.code.style=official       # Kotlin code style
```

### Compose Desktop (build.gradle.kts)
```kotlin
compose.desktop {
    application {
        mainClass = "com.rats.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "RATS"
            packageVersion = "1.0.0"
        }
    }
}
```

## Performance Optimizations

### CSV Import
Uses DuckDB's native CSV reader which is SIMD-optimized and parallel:
```kotlin
CREATE TABLE $tableName AS
SELECT * FROM read_csv_auto('$filePath', header=true, sample_size=-1, parallel=true)
```

### Excel Import
Transaction batching for better performance:
```kotlin
db.execute("BEGIN TRANSACTION")
// Insert 1000 rows at a time
db.execute("COMMIT")
```

### Virtual Scrolling
LazyColumn renders only visible rows:
```kotlin
LazyColumn(state = verticalListState) {
    itemsIndexed(data.rows) { index, row ->
        // Only visible rows are composed
    }
}
```

### Statistics
Uses DuckDB's optimized aggregate functions:
```sql
SELECT
    COUNT("column"),
    AVG("column"),
    MEDIAN("column"),
    STDDEV_POP("column"),
    PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY "column")
FROM table
```

## Error Handling

### Pattern
```kotlin
try {
    // Operation
    return SuccessResult(...)
} catch (e: Exception) {
    return FailureResult(success = false, message = "Error: ${e.message}")
}
```

### UI Error Display
```kotlin
appState.errorMessage?.let { error ->
    ErrorDialog(message = error, onDismiss = { appState.clearError() })
}
```

## Threading Model

### Main Thread (Compose)
- UI rendering
- State updates
- Event handling

### Dispatchers.IO
- Database queries
- File I/O
- Statistical calculations

### Swing EDT
- Native file dialogs (JFileChooser)

```kotlin
// File dialog on Swing EDT
val filePath = suspendCoroutine<String?> { continuation ->
    SwingUtilities.invokeLater {
        val result = JFileChooser().showOpenDialog(null)
        continuation.resume(...)
    }
}

// Database operation on IO dispatcher
withContext(Dispatchers.IO) {
    appState.importer.importFile(filePath)
}
```

## Type Conversion

DuckDB JDBC types to Kotlin:
```kotlin
when (rs.metaData.getColumnType(columnIndex)) {
    Types.BOOLEAN -> rs.getBoolean(columnIndex)
    Types.INTEGER -> rs.getInt(columnIndex)
    Types.BIGINT -> rs.getLong(columnIndex)
    Types.DOUBLE -> rs.getDouble(columnIndex)
    Types.VARCHAR -> rs.getString(columnIndex)
    Types.DATE -> rs.getDate(columnIndex)
    Types.TIMESTAMP -> rs.getTimestamp(columnIndex)
    else -> value.toString()
}
```

## Future Enhancements

1. **Parquet Support** - DuckDB has native Parquet support
2. **JSON Import** - Using DuckDB's `read_json_auto`
3. **Undo/Redo** - State history management
4. **Charts** - Integration with Compose charting libraries
5. **Custom Formulas** - SQL expression builder
6. **Pivot Tables** - DuckDB's PIVOT functionality
7. **Database Persistence** - Optional file-based DuckDB storage
8. **Multi-Table Support** - Multiple tables in memory
