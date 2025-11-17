package com.rats.statistics

import com.rats.core.DuckDBCore

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

data class TableStatistics(
    val tableName: String,
    val totalRows: Long,
    val totalColumns: Int,
    val columnStats: List<ColumnStatistics>
)

data class AggregationResult(
    val columnName: String,
    val function: String,
    val result: Any?
)

class StatisticsCalculator(private val db: DuckDBCore) {

    fun getTableStatistics(tableName: String): TableStatistics {
        val tableInfo = db.getTableInfo(tableName)

        val columnStats = tableInfo.columns.map { columnInfo ->
            calculateColumnStatistics(tableName, columnInfo.name, columnInfo.type)
        }

        return TableStatistics(
            tableName = tableName,
            totalRows = tableInfo.rowCount,
            totalColumns = tableInfo.columns.size,
            columnStats = columnStats
        )
    }

    private fun calculateColumnStatistics(
        tableName: String,
        columnName: String,
        dataType: String
    ): ColumnStatistics {
        val isNumeric = dataType.contains("INT", ignoreCase = true)
                || dataType.contains("DOUBLE", ignoreCase = true)
                || dataType.contains("FLOAT", ignoreCase = true)
                || dataType.contains("DECIMAL", ignoreCase = true)
                || dataType.contains("NUMERIC", ignoreCase = true)

        val query = if (isNumeric) {
            """
                SELECT
                    COUNT("$columnName") as count,
                    COUNT(*) - COUNT("$columnName") as null_count,
                    COUNT(DISTINCT "$columnName") as distinct_count,
                    MIN("$columnName")::VARCHAR as min_val,
                    MAX("$columnName")::VARCHAR as max_val,
                    AVG("$columnName") as mean,
                    MEDIAN("$columnName") as median,
                    STDDEV_POP("$columnName") as std_dev,
                    VAR_POP("$columnName") as variance,
                    PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY "$columnName") as q25,
                    PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY "$columnName") as q75
                FROM $tableName
            """.trimIndent()
        } else {
            """
                SELECT
                    COUNT("$columnName") as count,
                    COUNT(*) - COUNT("$columnName") as null_count,
                    COUNT(DISTINCT "$columnName") as distinct_count,
                    MIN("$columnName")::VARCHAR as min_val,
                    MAX("$columnName")::VARCHAR as max_val,
                    NULL as mean,
                    NULL as median,
                    NULL as std_dev,
                    NULL as variance,
                    NULL as q25,
                    NULL as q75
                FROM $tableName
            """.trimIndent()
        }

        val result = db.executeQuery(query)

        if (result.rows.isNotEmpty()) {
            val row = result.rows[0]
            return ColumnStatistics(
                columnName = columnName,
                dataType = dataType,
                count = (row[0] as? Number)?.toLong() ?: 0L,
                nullCount = (row[1] as? Number)?.toLong() ?: 0L,
                distinctCount = (row[2] as? Number)?.toLong() ?: 0L,
                min = row[3],
                max = row[4],
                mean = (row[5] as? Number)?.toDouble(),
                median = (row[6] as? Number)?.toDouble(),
                stdDev = (row[7] as? Number)?.toDouble(),
                variance = (row[8] as? Number)?.toDouble(),
                q25 = (row[9] as? Number)?.toDouble(),
                q75 = (row[10] as? Number)?.toDouble()
            )
        }

        return ColumnStatistics(
            columnName = columnName,
            dataType = dataType,
            count = 0,
            nullCount = 0,
            distinctCount = 0,
            min = null,
            max = null,
            mean = null,
            median = null,
            stdDev = null,
            variance = null,
            q25 = null,
            q75 = null
        )
    }

    fun aggregateColumn(
        tableName: String,
        columnName: String,
        function: String
    ): AggregationResult {
        val funcUpper = function.uppercase()
        val query = "SELECT $funcUpper(\"$columnName\") FROM $tableName"
        val result = db.executeQuery(query)

        val value = if (result.rows.isNotEmpty()) {
            result.rows[0][0]
        } else {
            null
        }

        return AggregationResult(
            columnName = columnName,
            function = funcUpper,
            result = value
        )
    }

    fun calculateCorrelation(
        tableName: String,
        columnX: String,
        columnY: String
    ): Double {
        val query = """
            SELECT CORR("$columnX", "$columnY") FROM $tableName
        """.trimIndent()

        val result = db.executeQuery(query)

        return if (result.rows.isNotEmpty()) {
            (result.rows[0][0] as? Number)?.toDouble() ?: 0.0
        } else {
            0.0
        }
    }
}
