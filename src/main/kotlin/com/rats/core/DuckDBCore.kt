package com.rats.core

import org.duckdb.DuckDBConnection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types

data class ColumnInfo(
    val name: String,
    val type: String
)

data class TableInfo(
    val tableName: String,
    val columns: List<ColumnInfo>,
    val rowCount: Long
)

data class QueryResult(
    val columns: List<String>,
    val rows: List<List<Any?>>,
    val totalRows: Long
)

class DuckDBCore : AutoCloseable {
    private val connection: DuckDBConnection

    init {
        try {
            // Load DuckDB driver
            Class.forName("org.duckdb.DuckDBDriver")

            // Create in-memory database connection
            connection = DriverManager.getConnection("jdbc:duckdb:") as DuckDBConnection

            // Configure DuckDB for performance
            connection.createStatement().use { stmt ->
                stmt.execute("SET memory_limit='4GB'")
                stmt.execute("SET threads=4")
            }
        } catch (e: UnsatisfiedLinkError) {
            // Native library loading failed
            System.err.println("Failed to load DuckDB native library: ${e.message}")
            System.err.println("This usually means the DLL/SO file is missing or incompatible")
            e.printStackTrace()
            throw RuntimeException("Failed to load DuckDB native library. The application cannot start.", e)
        } catch (e: Exception) {
            System.err.println("Failed to initialize DuckDB: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Failed to initialize database engine. Error: ${e.message}", e)
        }
    }

    fun executeQuery(query: String): QueryResult {
        connection.createStatement().use { stmt ->
            stmt.executeQuery(query).use { rs ->
                val metaData = rs.metaData
                val columnCount = metaData.columnCount

                val columns = (1..columnCount).map { i ->
                    metaData.getColumnName(i)
                }

                val rows = mutableListOf<List<Any?>>()
                while (rs.next()) {
                    val row = (1..columnCount).map { i ->
                        convertValue(rs, i)
                    }
                    rows.add(row)
                }

                return QueryResult(
                    columns = columns,
                    rows = rows,
                    totalRows = rows.size.toLong()
                )
            }
        }
    }

    fun queryData(tableName: String, limit: Int = 1000, offset: Int = 0): QueryResult {
        val sanitizedTable = sanitizeTableName(tableName)
        val query = "SELECT * FROM $sanitizedTable LIMIT $limit OFFSET $offset"
        return executeQuery(query)
    }

    fun getTableInfo(tableName: String): TableInfo {
        val sanitizedTable = sanitizeTableName(tableName)

        // Get column information
        val columns = mutableListOf<ColumnInfo>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("DESCRIBE $sanitizedTable").use { rs ->
                while (rs.next()) {
                    columns.add(
                        ColumnInfo(
                            name = rs.getString("column_name"),
                            type = rs.getString("column_type")
                        )
                    )
                }
            }
        }

        // Get row count
        val rowCount = connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) FROM $sanitizedTable").use { rs ->
                if (rs.next()) rs.getLong(1) else 0L
            }
        }

        return TableInfo(
            tableName = sanitizedTable,
            columns = columns,
            rowCount = rowCount
        )
    }

    fun execute(sql: String) {
        connection.createStatement().use { stmt ->
            stmt.execute(sql)
        }
    }

    fun executeBatch(sqlStatements: List<String>) {
        connection.createStatement().use { stmt ->
            sqlStatements.forEach { sql ->
                stmt.addBatch(sql)
            }
            stmt.executeBatch()
        }
    }

    fun getConnection(): DuckDBConnection = connection

    private fun convertValue(rs: ResultSet, columnIndex: Int): Any? {
        val value = rs.getObject(columnIndex) ?: return null

        return when (rs.metaData.getColumnType(columnIndex)) {
            Types.NULL -> null
            Types.BOOLEAN, Types.BIT -> rs.getBoolean(columnIndex)
            Types.TINYINT -> rs.getByte(columnIndex)
            Types.SMALLINT -> rs.getShort(columnIndex)
            Types.INTEGER -> rs.getInt(columnIndex)
            Types.BIGINT -> rs.getLong(columnIndex)
            Types.FLOAT, Types.REAL -> rs.getFloat(columnIndex)
            Types.DOUBLE -> rs.getDouble(columnIndex)
            Types.DECIMAL, Types.NUMERIC -> rs.getBigDecimal(columnIndex)
            Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR -> rs.getString(columnIndex)
            Types.DATE -> rs.getDate(columnIndex)
            Types.TIME -> rs.getTime(columnIndex)
            Types.TIMESTAMP -> rs.getTimestamp(columnIndex)
            else -> value.toString()
        }
    }

    companion object {
        fun sanitizeTableName(name: String): String {
            return name.map { c ->
                if (c.isLetterOrDigit() || c == '_') c else '_'
            }.joinToString("")
                .trim('_')
                .ifEmpty { "table" }
        }
    }

    override fun close() {
        connection.close()
    }
}
