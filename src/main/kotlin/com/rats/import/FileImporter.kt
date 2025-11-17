package com.rats.import

import com.opencsv.CSVReader
import com.rats.core.DuckDBCore
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileReader
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

enum class FileFormat {
    CSV,
    EXCEL,
    UNKNOWN
}

data class ImportProgress(
    val rowsImported: Int,
    val totalRows: Int?,
    val status: String
)

data class ImportResult(
    val tableName: String,
    val rowsImported: Int,
    val columnsCount: Int,
    val success: Boolean,
    val message: String
)

data class PreviewData(
    val columns: List<String>,
    val rows: List<List<String>>,
    val totalPreviewRows: Int
)

class FileImporter(private val db: DuckDBCore) {

    fun detectFileFormat(filePath: String): FileFormat {
        val extension = Path.of(filePath).extension.lowercase()
        return when (extension) {
            "csv", "tsv" -> FileFormat.CSV
            "xls", "xlsx", "xlsm" -> FileFormat.EXCEL
            else -> FileFormat.UNKNOWN
        }
    }

    fun previewFile(filePath: String, maxRows: Int = 10): PreviewData {
        return when (detectFileFormat(filePath)) {
            FileFormat.CSV -> previewCsv(filePath, maxRows)
            FileFormat.EXCEL -> previewExcel(filePath, maxRows)
            FileFormat.UNKNOWN -> throw IllegalArgumentException("Unsupported file format")
        }
    }

    fun importFile(
        filePath: String,
        tableName: String? = null,
        onProgress: ((ImportProgress) -> Unit)? = null
    ): ImportResult {
        val file = File(filePath)
        if (!file.exists()) {
            return ImportResult("", 0, 0, false, "File not found: $filePath")
        }

        val actualTableName = tableName ?: DuckDBCore.sanitizeTableName(
            Path.of(filePath).nameWithoutExtension
        )

        onProgress?.invoke(ImportProgress(0, null, "Starting import..."))

        return when (detectFileFormat(filePath)) {
            FileFormat.CSV -> importCsv(filePath, actualTableName, onProgress)
            FileFormat.EXCEL -> importExcel(filePath, actualTableName, onProgress)
            FileFormat.UNKNOWN -> ImportResult(
                actualTableName,
                0,
                0,
                false,
                "Unsupported file format"
            )
        }
    }

    private fun previewCsv(filePath: String, maxRows: Int): PreviewData {
        CSVReader(FileReader(filePath)).use { reader ->
            val allRows = mutableListOf<List<String>>()
            var rowCount = 0

            reader.forEach { row ->
                if (rowCount <= maxRows) {
                    allRows.add(row.toList())
                }
                rowCount++
            }

            if (allRows.isEmpty()) {
                return PreviewData(emptyList(), emptyList(), 0)
            }

            val columns = allRows.first()
            val dataRows = if (allRows.size > 1) {
                allRows.drop(1).take(maxRows)
            } else {
                emptyList()
            }

            return PreviewData(columns, dataRows, dataRows.size)
        }
    }

    private fun previewExcel(filePath: String, maxRows: Int): PreviewData {
        WorkbookFactory.create(File(filePath)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            val rows = mutableListOf<List<String>>()

            for (i in 0..minOf(maxRows, sheet.lastRowNum)) {
                val row = sheet.getRow(i) ?: continue
                val rowData = mutableListOf<String>()

                for (j in 0 until row.lastCellNum) {
                    val cell = row.getCell(j)
                    val cellValue = when (cell?.cellType) {
                        CellType.STRING -> cell.stringCellValue
                        CellType.NUMERIC -> {
                            if (DateUtil.isCellDateFormatted(cell)) {
                                cell.localDateTimeCellValue.toString()
                            } else {
                                cell.numericCellValue.toString()
                            }
                        }
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        CellType.FORMULA -> cell.cellFormula
                        else -> ""
                    }
                    rowData.add(cellValue)
                }
                rows.add(rowData)
            }

            if (rows.isEmpty()) {
                return PreviewData(emptyList(), emptyList(), 0)
            }

            val columns = rows.first()
            val dataRows = if (rows.size > 1) {
                rows.drop(1)
            } else {
                emptyList()
            }

            return PreviewData(columns, dataRows, dataRows.size)
        }
    }

    private fun importCsv(
        filePath: String,
        tableName: String,
        onProgress: ((ImportProgress) -> Unit)?
    ): ImportResult {
        return try {
            onProgress?.invoke(ImportProgress(0, null, "Reading CSV file..."))

            // Use DuckDB's native CSV reader for optimal performance
            val query = """
                CREATE TABLE $tableName AS
                SELECT * FROM read_csv_auto('$filePath', header=true, sample_size=-1, parallel=true)
            """.trimIndent()

            db.execute(query)

            // Get the row count
            val tableInfo = db.getTableInfo(tableName)

            onProgress?.invoke(
                ImportProgress(
                    tableInfo.rowCount.toInt(),
                    tableInfo.rowCount.toInt(),
                    "Import complete!"
                )
            )

            ImportResult(
                tableName = tableName,
                rowsImported = tableInfo.rowCount.toInt(),
                columnsCount = tableInfo.columns.size,
                success = true,
                message = "Successfully imported ${tableInfo.rowCount} rows"
            )
        } catch (e: Exception) {
            ImportResult(tableName, 0, 0, false, "CSV import failed: ${e.message}")
        }
    }

    private fun importExcel(
        filePath: String,
        tableName: String,
        onProgress: ((ImportProgress) -> Unit)?
    ): ImportResult {
        return try {
            WorkbookFactory.create(File(filePath)).use { workbook ->
                val sheet = workbook.getSheetAt(0)

                if (sheet.lastRowNum < 0) {
                    return ImportResult(tableName, 0, 0, false, "Empty Excel file")
                }

                // Get headers from first row
                val headerRow = sheet.getRow(0) ?: return ImportResult(
                    tableName, 0, 0, false, "No header row found"
                )

                val columns = mutableListOf<String>()
                for (i in 0 until headerRow.lastCellNum) {
                    val cell = headerRow.getCell(i)
                    val columnName = cell?.stringCellValue ?: "column_$i"
                    columns.add(DuckDBCore.sanitizeTableName(columnName))
                }

                // Create table with VARCHAR columns
                val createTableSql = """
                    CREATE TABLE $tableName (
                        ${columns.joinToString(", ") { "$it VARCHAR" }}
                    )
                """.trimIndent()
                db.execute(createTableSql)

                // Import data rows with transaction batching
                db.execute("BEGIN TRANSACTION")

                var totalRows = 0
                var batchCount = 0
                val batchSize = 1000

                for (rowIndex in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue

                    val values = columns.indices.map { colIndex ->
                        val cell = row.getCell(colIndex)
                        val value = when (cell?.cellType) {
                            CellType.STRING -> cell.stringCellValue
                            CellType.NUMERIC -> {
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    cell.localDateTimeCellValue.toString()
                                } else {
                                    cell.numericCellValue.toString()
                                }
                            }
                            CellType.BOOLEAN -> cell.booleanCellValue.toString()
                            CellType.FORMULA -> {
                                try {
                                    cell.numericCellValue.toString()
                                } catch (e: Exception) {
                                    cell.stringCellValue
                                }
                            }
                            else -> ""
                        }
                        "'${value.replace("'", "''")}'"
                    }

                    val insertSql = "INSERT INTO $tableName VALUES (${values.joinToString(", ")})"
                    db.execute(insertSql)

                    totalRows++
                    batchCount++

                    if (batchCount >= batchSize) {
                        onProgress?.invoke(
                            ImportProgress(
                                totalRows,
                                null,
                                "Importing... $totalRows rows"
                            )
                        )
                        batchCount = 0
                    }
                }

                db.execute("COMMIT")

                onProgress?.invoke(
                    ImportProgress(totalRows, totalRows, "Import complete!")
                )

                ImportResult(
                    tableName = tableName,
                    rowsImported = totalRows,
                    columnsCount = columns.size,
                    success = true,
                    message = "Successfully imported $totalRows rows"
                )
            }
        } catch (e: Exception) {
            try {
                db.execute("ROLLBACK")
            } catch (_: Exception) {
            }
            ImportResult(tableName, 0, 0, false, "Excel import failed: ${e.message}")
        }
    }
}
