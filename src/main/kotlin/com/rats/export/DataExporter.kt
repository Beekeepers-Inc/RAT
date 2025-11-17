package com.rats.export

import com.rats.core.DuckDBCore
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

data class ExportResult(
    val success: Boolean,
    val message: String,
    val filePath: String,
    val rowsExported: Int
)

class DataExporter(private val db: DuckDBCore) {

    fun exportToCsv(
        tableName: String,
        filePath: String,
        includeHeader: Boolean = true
    ): ExportResult {
        return try {
            val headerOption = if (includeHeader) "HEADER" else ""
            val query = "COPY $tableName TO '$filePath' (FORMAT CSV, $headerOption)"
            db.execute(query)

            val tableInfo = db.getTableInfo(tableName)

            ExportResult(
                success = true,
                message = "Successfully exported ${tableInfo.rowCount} rows to CSV",
                filePath = filePath,
                rowsExported = tableInfo.rowCount.toInt()
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                message = "CSV export failed: ${e.message}",
                filePath = filePath,
                rowsExported = 0
            )
        }
    }

    fun exportToExcel(
        tableName: String,
        filePath: String,
        sheetName: String = "Data"
    ): ExportResult {
        return try {
            val data = db.queryData(tableName, limit = Int.MAX_VALUE, offset = 0)

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet(sheetName)

            // Write header
            val headerRow = sheet.createRow(0)
            data.columns.forEachIndexed { index, columnName ->
                headerRow.createCell(index).setCellValue(columnName)
            }

            // Write data rows
            data.rows.forEachIndexed { rowIndex, rowData ->
                val row = sheet.createRow(rowIndex + 1)
                rowData.forEachIndexed { colIndex, cellValue ->
                    val cell = row.createCell(colIndex)
                    when (cellValue) {
                        null -> cell.setBlank()
                        is Boolean -> cell.setCellValue(cellValue)
                        is Number -> cell.setCellValue(cellValue.toDouble())
                        is String -> cell.setCellValue(cellValue)
                        else -> cell.setCellValue(cellValue.toString())
                    }
                }
            }

            // Auto-size columns (optional, can be slow for large datasets)
            if (data.columns.size <= 50) {
                data.columns.indices.forEach { sheet.autoSizeColumn(it) }
            }

            // Write to file
            FileOutputStream(filePath).use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()

            ExportResult(
                success = true,
                message = "Successfully exported ${data.rows.size} rows to Excel",
                filePath = filePath,
                rowsExported = data.rows.size
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                message = "Excel export failed: ${e.message}",
                filePath = filePath,
                rowsExported = 0
            )
        }
    }

    fun exportQueryToCsv(
        query: String,
        filePath: String,
        includeHeader: Boolean = true
    ): ExportResult {
        return try {
            val headerOption = if (includeHeader) "HEADER" else ""
            val copyQuery = "COPY ($query) TO '$filePath' (FORMAT CSV, $headerOption)"
            db.execute(copyQuery)

            // Get count from the query
            val countQuery = "SELECT COUNT(*) FROM ($query)"
            val countResult = db.executeQuery(countQuery)
            val rowCount = (countResult.rows[0][0] as? Number)?.toInt() ?: 0

            ExportResult(
                success = true,
                message = "Successfully exported $rowCount rows to CSV",
                filePath = filePath,
                rowsExported = rowCount
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                message = "Query export failed: ${e.message}",
                filePath = filePath,
                rowsExported = 0
            )
        }
    }
}
