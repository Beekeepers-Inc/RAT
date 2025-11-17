package com.rats.editor

import com.rats.core.DuckDBCore

data class SortColumn(
    val columnName: String,
    val ascending: Boolean = true
)

data class ReorderResult(
    val success: Boolean,
    val message: String,
    val rowsAffected: Int
)

class DataEditor(private val db: DuckDBCore) {

    fun reorderRows(tableName: String, sortColumns: List<SortColumn>): ReorderResult {
        if (sortColumns.isEmpty()) {
            return ReorderResult(false, "No sort columns specified", 0)
        }

        return try {
            val sanitizedTable = DuckDBCore.sanitizeTableName(tableName)
            val tempTableName = "${sanitizedTable}_temp_sorted"

            // Build ORDER BY clause
            val orderByClause = sortColumns.joinToString(", ") { sortCol ->
                val direction = if (sortCol.ascending) "ASC" else "DESC"
                "${DuckDBCore.sanitizeTableName(sortCol.columnName)} $direction"
            }

            // Create temporary sorted table
            val createSortedSql = """
                CREATE TABLE $tempTableName AS
                SELECT * FROM $sanitizedTable
                ORDER BY $orderByClause
            """.trimIndent()

            db.execute(createSortedSql)

            // Drop original table
            db.execute("DROP TABLE $sanitizedTable")

            // Rename temporary table to original name
            db.execute("ALTER TABLE $tempTableName RENAME TO $sanitizedTable")

            // Get row count
            val tableInfo = db.getTableInfo(sanitizedTable)

            ReorderResult(
                success = true,
                message = "Successfully sorted ${tableInfo.rowCount} rows",
                rowsAffected = tableInfo.rowCount.toInt()
            )
        } catch (e: Exception) {
            ReorderResult(false, "Sort failed: ${e.message}", 0)
        }
    }

    fun filterData(
        tableName: String,
        filterExpression: String,
        newTableName: String? = null
    ): ReorderResult {
        return try {
            val sanitizedTable = DuckDBCore.sanitizeTableName(tableName)
            val targetTable = newTableName?.let { DuckDBCore.sanitizeTableName(it) }
                ?: "${sanitizedTable}_filtered"

            val query = """
                CREATE TABLE $targetTable AS
                SELECT * FROM $sanitizedTable
                WHERE $filterExpression
            """.trimIndent()

            db.execute(query)

            val tableInfo = db.getTableInfo(targetTable)

            ReorderResult(
                success = true,
                message = "Filtered ${tableInfo.rowCount} rows into $targetTable",
                rowsAffected = tableInfo.rowCount.toInt()
            )
        } catch (e: Exception) {
            ReorderResult(false, "Filter failed: ${e.message}", 0)
        }
    }

    fun deleteColumn(tableName: String, columnName: String): ReorderResult {
        return try {
            val sanitizedTable = DuckDBCore.sanitizeTableName(tableName)
            val sanitizedColumn = DuckDBCore.sanitizeTableName(columnName)

            // Get current columns
            val tableInfo = db.getTableInfo(sanitizedTable)
            val remainingColumns = tableInfo.columns
                .filter { it.name != sanitizedColumn }
                .map { it.name }

            if (remainingColumns.size == tableInfo.columns.size) {
                return ReorderResult(false, "Column $columnName not found", 0)
            }

            if (remainingColumns.isEmpty()) {
                return ReorderResult(false, "Cannot delete the only column", 0)
            }

            // Create new table without the column
            val tempTable = "${sanitizedTable}_temp"
            val selectColumns = remainingColumns.joinToString(", ")

            db.execute("CREATE TABLE $tempTable AS SELECT $selectColumns FROM $sanitizedTable")
            db.execute("DROP TABLE $sanitizedTable")
            db.execute("ALTER TABLE $tempTable RENAME TO $sanitizedTable")

            ReorderResult(
                success = true,
                message = "Column $columnName deleted successfully",
                rowsAffected = tableInfo.rowCount.toInt()
            )
        } catch (e: Exception) {
            ReorderResult(false, "Delete column failed: ${e.message}", 0)
        }
    }

    fun addColumn(
        tableName: String,
        columnName: String,
        columnType: String = "VARCHAR",
        defaultValue: String? = null
    ): ReorderResult {
        return try {
            val sanitizedTable = DuckDBCore.sanitizeTableName(tableName)
            val sanitizedColumn = DuckDBCore.sanitizeTableName(columnName)

            val alterSql = if (defaultValue != null) {
                "ALTER TABLE $sanitizedTable ADD COLUMN $sanitizedColumn $columnType DEFAULT $defaultValue"
            } else {
                "ALTER TABLE $sanitizedTable ADD COLUMN $sanitizedColumn $columnType"
            }

            db.execute(alterSql)

            val tableInfo = db.getTableInfo(sanitizedTable)

            ReorderResult(
                success = true,
                message = "Column $columnName added successfully",
                rowsAffected = tableInfo.rowCount.toInt()
            )
        } catch (e: Exception) {
            ReorderResult(false, "Add column failed: ${e.message}", 0)
        }
    }

    fun updateCell(
        tableName: String,
        rowIndex: Int,
        columnName: String,
        newValue: String
    ): ReorderResult {
        return try {
            val sanitizedTable = DuckDBCore.sanitizeTableName(tableName)
            // Quote the column name with double quotes for DuckDB
            val quotedColumn = "\"${columnName.replace("\"", "\"\"")}\""

            // Add a row number column to identify the specific row
            val tempTable = "${sanitizedTable}_with_rownum"
            db.execute("""
                CREATE TABLE $tempTable AS
                SELECT *, ROW_NUMBER() OVER () as _row_num_
                FROM $sanitizedTable
            """.trimIndent())

            // Update the specific row - try to cast to the appropriate type
            val escapedValue = newValue.replace("'", "''")
            db.execute("""
                UPDATE $tempTable
                SET $quotedColumn = '$escapedValue'
                WHERE _row_num_ = ${rowIndex + 1}
            """.trimIndent())

            // Get column names without the row number (properly quoted)
            val tableInfo = db.getTableInfo(tempTable)
            val originalColumns = tableInfo.columns
                .filter { it.name != "_row_num_" }
                .joinToString(", ") { "\"${it.name.replace("\"", "\"\"")}\"" }

            // Recreate original table without row number
            db.execute("DROP TABLE $sanitizedTable")
            db.execute("""
                CREATE TABLE $sanitizedTable AS
                SELECT $originalColumns
                FROM $tempTable
            """.trimIndent())
            db.execute("DROP TABLE $tempTable")

            ReorderResult(
                success = true,
                message = "Cell updated successfully",
                rowsAffected = 1
            )
        } catch (e: Exception) {
            ReorderResult(false, "Update cell failed: ${e.message}", 0)
        }
    }

    fun deleteRows(tableName: String, rowIndices: List<Int>): ReorderResult {
        return try {
            val sanitizedTable = DuckDBCore.sanitizeTableName(tableName)

            // Add row numbers
            val tempTable = "${sanitizedTable}_with_rownum"
            db.execute("""
                CREATE TABLE $tempTable AS
                SELECT *, ROW_NUMBER() OVER () as _row_num_
                FROM $sanitizedTable
            """.trimIndent())

            // Delete specified rows (convert to 1-based indices)
            val indicesToDelete = rowIndices.map { it + 1 }.joinToString(", ")
            db.execute("DELETE FROM $tempTable WHERE _row_num_ IN ($indicesToDelete)")

            // Get original columns
            val tableInfo = db.getTableInfo(tempTable)
            val originalColumns = tableInfo.columns
                .filter { it.name != "_row_num_" }
                .joinToString(", ") { it.name }

            // Recreate original table
            db.execute("DROP TABLE $sanitizedTable")
            db.execute("""
                CREATE TABLE $sanitizedTable AS
                SELECT $originalColumns
                FROM $tempTable
            """.trimIndent())
            db.execute("DROP TABLE $tempTable")

            ReorderResult(
                success = true,
                message = "Deleted ${rowIndices.size} rows",
                rowsAffected = rowIndices.size
            )
        } catch (e: Exception) {
            ReorderResult(false, "Delete rows failed: ${e.message}", 0)
        }
    }

    fun addEmptyRow(tableName: String): ReorderResult {
        return try {
            val sanitizedTable = DuckDBCore.sanitizeTableName(tableName)

            // Get column info to create default values
            val tableInfo = db.getTableInfo(sanitizedTable)

            val values = tableInfo.columns.map { col ->
                when {
                    col.type.contains("INT", ignoreCase = true) -> "0"
                    col.type.contains("DOUBLE", ignoreCase = true) ||
                    col.type.contains("FLOAT", ignoreCase = true) ||
                    col.type.contains("DECIMAL", ignoreCase = true) -> "0.0"
                    col.type.contains("BOOL", ignoreCase = true) -> "false"
                    col.type.contains("DATE", ignoreCase = true) -> "'1970-01-01'"
                    col.type.contains("TIME", ignoreCase = true) -> "'1970-01-01 00:00:00'"
                    col.type.contains("TIMESTAMP", ignoreCase = true) -> "'1970-01-01 00:00:00'"
                    else -> "''"  // Empty string for VARCHAR and others
                }
            }

            val insertSql = "INSERT INTO $sanitizedTable VALUES (${values.joinToString(", ")})"
            db.execute(insertSql)

            val newTableInfo = db.getTableInfo(sanitizedTable)

            ReorderResult(
                success = true,
                message = "Added new row",
                rowsAffected = newTableInfo.rowCount.toInt()
            )
        } catch (e: Exception) {
            ReorderResult(false, "Add row failed: ${e.message}", 0)
        }
    }
}
