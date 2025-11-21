# Pivot Table Implementation Plan

## Overview

This document tracks the implementation of the Pivot Table Builder feature, breaking down each phase into specific, actionable tasks with estimated effort and dependencies.

---

## Phase 1: Core Pivot Engine (Week 1-2)

### Task 1.1: Create Data Models
**Estimated Time**: 4 hours
**Status**: Not Started
**Files to Create**:
- `src/main/kotlin/com/rats/pivot/model/PivotConfiguration.kt`
- `src/main/kotlin/com/rats/pivot/model/PivotField.kt`
- `src/main/kotlin/com/rats/pivot/model/ValueField.kt`
- `src/main/kotlin/com/rats/pivot/model/PivotResult.kt`

**Implementation Details**:
```kotlin
// PivotConfiguration.kt
package com.rats.pivot.model

data class PivotConfiguration(
    val sourceTable: String,
    val rowFields: List<PivotField> = emptyList(),
    val columnFields: List<PivotField> = emptyList(),
    val valueFields: List<ValueField> = emptyList(),
    val filters: List<FilterCondition> = emptyList(),
    val showGrandTotals: Boolean = true,
    val showSubtotals: Boolean = true
) {
    fun isValid(): Boolean {
        // Must have at least one value field
        if (valueFields.isEmpty()) return false
        // Must have at least one dimension (row or column)
        if (rowFields.isEmpty() && columnFields.isEmpty()) return false
        return true
    }
}
```

**Acceptance Criteria**:
- ✅ All data classes compile without errors
- ✅ Validation logic works correctly
- ✅ Data classes are immutable (all val, no var)
- ✅ toString() and copy() work as expected

---

### Task 1.2: Implement Query Generator - Basic Template
**Estimated Time**: 8 hours
**Status**: Not Started
**Dependencies**: Task 1.1
**Files to Create**:
- `src/main/kotlin/com/rats/pivot/engine/PivotQueryGenerator.kt`

**Implementation Details**:
Start with simplest case: 1 row field + 1 value field (no PIVOT needed)

```kotlin
class PivotQueryGenerator(private val db: DuckDBCore) {

    fun generateQuery(config: PivotConfiguration): String {
        require(config.isValid()) { "Invalid pivot configuration" }

        return when {
            // Simple grouping (no columns to pivot)
            config.columnFields.isEmpty() -> generateGroupByQuery(config)

            // Cross-tab with pivot
            config.columnFields.isNotEmpty() -> generatePivotQuery(config)

            else -> throw IllegalArgumentException("Invalid configuration")
        }
    }

    private fun generateGroupByQuery(config: PivotConfiguration): String {
        val selectParts = mutableListOf<String>()

        // Add row fields
        config.rowFields.forEach { field ->
            selectParts.add(field.columnName)
        }

        // Add aggregations
        config.valueFields.forEach { valueField ->
            val aggExpr = buildAggregationExpression(valueField)
            selectParts.add("$aggExpr as ${valueField.displayName}")
        }

        val selectClause = selectParts.joinToString(", ")
        val fromClause = config.sourceTable
        val whereClause = buildWhereClause(config.filters)
        val groupByClause = config.rowFields.joinToString(", ") { it.columnName }
        val orderByClause = config.rowFields.joinToString(", ") { it.columnName }

        return """
            SELECT $selectClause
            FROM $fromClause
            WHERE $whereClause
            GROUP BY $groupByClause
            ORDER BY $orderByClause
            LIMIT 1000
        """.trimIndent()
    }

    private fun buildAggregationExpression(valueField: ValueField): String {
        return when (valueField.aggregation) {
            AggregationType.SUM -> "SUM(${valueField.columnName})"
            AggregationType.COUNT -> "COUNT(*)"
            AggregationType.COUNT_DISTINCT -> "COUNT(DISTINCT ${valueField.columnName})"
            AggregationType.AVERAGE -> "AVG(${valueField.columnName})"
            AggregationType.MIN -> "MIN(${valueField.columnName})"
            AggregationType.MAX -> "MAX(${valueField.columnName})"
            AggregationType.MEDIAN -> "MEDIAN(${valueField.columnName})"
            AggregationType.STDEV -> "STDDEV(${valueField.columnName})"
            AggregationType.VARIANCE -> "VARIANCE(${valueField.columnName})"
            else -> throw IllegalArgumentException("Unsupported aggregation: ${valueField.aggregation}")
        }
    }

    private fun buildWhereClause(filters: List<FilterCondition>): String {
        if (filters.isEmpty()) return "1=1"

        val conditions = filters.map { filter ->
            when (filter.operator) {
                "=" -> "${filter.column} = '${filter.value}'"
                ">" -> "${filter.column} > ${filter.value}"
                "<" -> "${filter.column} < ${filter.value}"
                "IN" -> "${filter.column} IN (${filter.value})"
                else -> "1=1"
            }
        }

        return conditions.joinToString(" AND ")
    }
}
```

**Test Cases**:
1. Simple grouping: Group by region, SUM(sales)
2. Multiple dimensions: Group by region, product, SUM(sales)
3. Multiple aggregations: SUM(sales), COUNT(*), AVG(price)
4. With filters: WHERE region = 'West'

**Acceptance Criteria**:
- ✅ Generates syntactically valid SQL
- ✅ Query executes without errors
- ✅ Results match expected aggregations
- ✅ Handles empty filters gracefully

---

### Task 1.3: Implement Query Generator - PIVOT Support
**Estimated Time**: 8 hours
**Status**: Not Started
**Dependencies**: Task 1.2
**Files to Modify**:
- `src/main/kotlin/com/rats/pivot/engine/PivotQueryGenerator.kt`

**Implementation Details**:
Add support for cross-tab pivots using DuckDB's PIVOT clause

```kotlin
private fun generatePivotQuery(config: PivotConfiguration): String {
    // Step 1: Get distinct values for column field
    val columnField = config.columnFields.first()
    val distinctValues = getDistinctColumnValues(config.sourceTable, columnField)

    // Limit to prevent explosion
    if (distinctValues.size > 100) {
        throw IllegalArgumentException("Too many column values (${distinctValues.size}). Maximum is 100.")
    }

    // Step 2: Build inner SELECT
    val selectParts = mutableListOf<String>()

    // Add row fields
    config.rowFields.forEach { field ->
        selectParts.add(field.columnName)
    }

    // Add column field (will be pivoted)
    selectParts.add(columnField.columnName)

    // Add aggregations
    config.valueFields.forEach { valueField ->
        val aggExpr = buildAggregationExpression(valueField)
        selectParts.add("$aggExpr as ${valueField.displayName}")
    }

    val innerSelect = selectParts.joinToString(", ")
    val groupByFields = (config.rowFields + config.columnFields).joinToString(", ") { it.columnName }

    // Step 3: Build PIVOT clause
    val pivotAggregations = config.valueFields.joinToString(", ") { valueField ->
        val aggFunc = when (valueField.aggregation) {
            AggregationType.SUM -> "SUM"
            AggregationType.AVERAGE -> "AVG"
            AggregationType.COUNT -> "SUM"
            else -> "SUM"
        }
        "$aggFunc(${valueField.displayName})"
    }

    val columnValues = distinctValues.joinToString(", ") { "'$it'" }

    // Step 4: Combine into final query
    return """
        SELECT * FROM (
            SELECT $innerSelect
            FROM ${config.sourceTable}
            WHERE ${buildWhereClause(config.filters)}
            GROUP BY $groupByFields
        )
        PIVOT (
            $pivotAggregations
            FOR ${columnField.columnName} IN ($columnValues)
        )
        ORDER BY ${config.rowFields.joinToString(", ") { it.columnName }}
        LIMIT 1000
    """.trimIndent()
}

private fun getDistinctColumnValues(table: String, field: PivotField): List<String> {
    val query = """
        SELECT DISTINCT ${field.columnName} as value
        FROM $table
        WHERE ${field.columnName} IS NOT NULL
        ORDER BY value
        LIMIT 100
    """.trimIndent()

    val result = db.executeQuery(query)
    return result.rows.map { it[0].toString() }
}
```

**Test Cases**:
1. Simple cross-tab: Rows=region, Columns=year, Values=SUM(sales)
2. With multiple rows: Rows=[region, product], Columns=year
3. With filters: WHERE date >= '2024-01-01'
4. Handle too many columns (>100)

**Acceptance Criteria**:
- ✅ Generates valid PIVOT query
- ✅ Executes without errors
- ✅ Results format as cross-tab
- ✅ Handles edge cases (0 values, 1 value, 100 values)

---

### Task 1.4: Implement Pivot Executor
**Estimated Time**: 4 hours
**Status**: Not Started
**Dependencies**: Task 1.3
**Files to Create**:
- `src/main/kotlin/com/rats/pivot/engine/PivotExecutor.kt`

**Implementation Details**:
```kotlin
class PivotExecutor(private val db: DuckDBCore) {

    fun execute(config: PivotConfiguration, previewOnly: Boolean = false): PivotResult {
        // Generate SQL
        val queryGenerator = PivotQueryGenerator(db)
        val sql = queryGenerator.generateQuery(config)

        // Execute query
        val queryResult = db.executeQuery(sql)

        // Transform to PivotResult
        return transformToPivotResult(queryResult, config)
    }

    private fun transformToPivotResult(
        queryResult: QueryResult,
        config: PivotConfiguration
    ): PivotResult {
        // Extract headers
        val rowFieldCount = config.rowFields.size
        val columnHeaders = queryResult.columns.drop(rowFieldCount)
        val rowHeaders = queryResult.rows.map { row ->
            row.take(rowFieldCount).map { it?.toString() ?: "" }
        }

        // Extract values (skip row dimension columns)
        val values = queryResult.rows.map { row ->
            row.drop(rowFieldCount)
        }

        // Calculate grand totals
        val grandTotal = calculateGrandTotal(values, config)

        return PivotResult(
            columnHeaders = listOf(columnHeaders),
            rowHeaders = rowHeaders,
            values = values,
            rowTotals = emptyList(),  // TODO: Calculate in phase 2
            columnTotals = emptyList(), // TODO: Calculate in phase 2
            grandTotal = grandTotal,
            totalRows = queryResult.totalRows,
            limitedToTopN = if (queryResult.rows.size == 1000) 1000 else null
        )
    }

    private fun calculateGrandTotal(values: List<List<Any?>>, config: PivotConfiguration): Any? {
        // For SUM aggregation, sum all values
        // For COUNT, count all
        // For AVG, calculate overall average
        // Simplified for now
        return null  // TODO: Implement
    }
}
```

**Test Cases**:
1. Execute simple pivot
2. Execute cross-tab pivot
3. Handle query errors gracefully
4. Validate result structure

**Acceptance Criteria**:
- ✅ Executes queries successfully
- ✅ Transforms results correctly
- ✅ Handles errors with meaningful messages
- ✅ Performance: <1 second for 1M rows

---

### Task 1.5: Unit Tests for Query Generator
**Estimated Time**: 6 hours
**Status**: Not Started
**Dependencies**: Task 1.2, 1.3
**Files to Create**:
- `src/test/kotlin/com/rats/pivot/engine/PivotQueryGeneratorTest.kt`

**Test Coverage**:
- Basic GROUP BY query
- PIVOT query with one column
- Multiple row fields
- Multiple value fields
- Date grouping
- Filters
- Edge cases

**Acceptance Criteria**:
- ✅ 80%+ code coverage
- ✅ All tests pass
- ✅ Tests run in <5 seconds

---

## Phase 2: Basic UI (Week 2-3)

### Task 2.1: Create Pivot Dialog Layout
**Estimated Time**: 6 hours
**Status**: Not Started
**Dependencies**: Phase 1 complete
**Files to Create**:
- `src/main/kotlin/com/rats/pivot/ui/PivotTableDialog.kt`

**Implementation Details**:
```kotlin
@Composable
fun PivotTableDialog(
    tableName: String,
    onDismiss: () -> Unit,
    onApply: (PivotConfiguration) -> Unit
) {
    val state = remember { PivotState(tableName) }

    Dialog(onCloseRequest = onDismiss) {
        Window(
            onCloseRequest = onDismiss,
            title = "Pivot Table Builder - $tableName",
            state = rememberWindowState(width = 1200.dp, height = 800.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top toolbar
                PivotToolbar(
                    onApply = { onApply(state.configuration) },
                    onClear = { state.clearAll() },
                    onSave = { /* TODO */ }
                )

                // Main content area
                Row(modifier = Modifier.weight(1f)) {
                    // Left: Field list
                    FieldListPanel(
                        fields = state.availableFields,
                        onFieldDragged = { state.startDrag(it) },
                        modifier = Modifier.width(250.dp)
                    )

                    // Right: Drop zones
                    DropZonePanel(
                        configuration = state.configuration,
                        onFieldDropped = { field, zone -> state.dropField(field, zone) },
                        onFieldRemoved = { field, zone -> state.removeField(field, zone) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Bottom: Preview
                PivotPreviewGrid(
                    result = state.previewResult,
                    isLoading = state.isLoading,
                    error = state.error,
                    modifier = Modifier.height(300.dp)
                )
            }
        }
    }
}
```

**Acceptance Criteria**:
- ✅ Dialog opens with proper layout
- ✅ All panels visible and sized correctly
- ✅ Responsive to window resizing
- ✅ Clean, professional appearance

---

### Task 2.2: Implement Field List Panel
**Estimated Time**: 6 hours
**Status**: Not Started
**Dependencies**: Task 2.1
**Files to Create**:
- `src/main/kotlin/com/rats/pivot/ui/FieldListPanel.kt`

**Implementation Details**:
- Show all columns from table
- Group by data type
- Search/filter box
- Icons for data types
- Drag handles
- Checkbox for quick add to Rows

**Acceptance Criteria**:
- ✅ Shows all table columns
- ✅ Can filter/search
- ✅ Visual grouping by type
- ✅ Draggable fields

---

### Task 2.3: Implement Drop Zones
**Estimated Time**: 8 hours
**Status**: Not Started
**Dependencies**: Task 2.1
**Files to Create**:
- `src/main/kotlin/com/rats/pivot/ui/DropZonePanel.kt`
- `src/main/kotlin/com/rats/pivot/ui/DropZone.kt`
- `src/main/kotlin/com/rats/pivot/ui/FieldChip.kt`

**Implementation Details**:
- 4 drop zones: Filters, Rows, Columns, Values
- Visual feedback on drag-over
- Remove button on each field chip
- Reorder within zone
- Context menu for field configuration

**Acceptance Criteria**:
- ✅ Can drop fields into zones
- ✅ Visual feedback works
- ✅ Can remove fields
- ✅ Can reorder fields within zone

---

### Task 2.4: Implement Drag-and-Drop
**Estimated Time**: 8 hours
**Status**: Not Started
**Dependencies**: Task 2.2, 2.3
**Files to Modify**:
- Multiple UI files

**Implementation Details**:
Use Compose's drag-and-drop modifiers
- Modifier.draggable()
- Modifier.dropTarget()
- Visual drag preview
- Highlight valid drop zones

**Acceptance Criteria**:
- ✅ Smooth drag experience
- ✅ Visual preview during drag
- ✅ Valid drop zones highlighted
- ✅ Invalid drops rejected

---

### Task 2.5: Implement Preview Grid
**Estimated Time**: 8 hours
**Status**: Not Started
**Dependencies**: Task 2.1, Phase 1
**Files to Create**:
- `src/main/kotlin/com/rats/pivot/ui/PivotPreviewGrid.kt`

**Implementation Details**:
- LazyColumn for rows
- LazyRow for horizontal scroll
- Format numbers appropriately
- Show loading spinner
- Show error messages
- Grand totals row

**Acceptance Criteria**:
- ✅ Displays pivot results correctly
- ✅ Handles large results efficiently
- ✅ Shows loading state
- ✅ Shows error state
- ✅ Professional formatting

---

### Task 2.6: Implement State Management
**Estimated Time**: 6 hours
**Status**: Not Started
**Dependencies**: Task 2.1
**Files to Create**:
- `src/main/kotlin/com/rats/pivot/state/PivotState.kt`

**Implementation Details**:
```kotlin
class PivotState(private val tableName: String) {
    private val db = DuckDBCore()
    private val scope = CoroutineScope(Dispatchers.Main)

    var configuration by mutableStateOf(
        PivotConfiguration(sourceTable = tableName)
    )

    var availableFields by mutableStateOf<List<PivotField>>(emptyList())
    var previewResult by mutableStateOf<PivotResult?>(null)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    init {
        loadAvailableFields()
    }

    private fun loadAvailableFields() {
        scope.launch(Dispatchers.IO) {
            try {
                val tableInfo = db.getTableInfo(tableName)
                val fields = tableInfo.columns.map { col ->
                    PivotField(
                        columnName = col.name,
                        displayName = col.name,
                        dataType = inferDataType(col.type)
                    )
                }
                withContext(Dispatchers.Main) {
                    availableFields = fields
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = "Failed to load fields: ${e.message}"
                }
            }
        }
    }

    fun dropField(field: PivotField, zone: DropZone) {
        when (zone) {
            DropZone.ROWS -> addToRows(field)
            DropZone.COLUMNS -> addToColumns(field)
            DropZone.VALUES -> addToValues(field)
            DropZone.FILTERS -> addFilter(field)
        }
        refreshPreview()
    }

    private fun refreshPreview() {
        if (!configuration.isValid()) {
            previewResult = null
            return
        }

        isLoading = true
        error = null

        scope.launch(Dispatchers.IO) {
            try {
                val executor = PivotExecutor(db)
                val result = executor.execute(configuration, previewOnly = true)

                withContext(Dispatchers.Main) {
                    previewResult = result
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = e.message
                    isLoading = false
                }
            }
        }
    }
}
```

**Acceptance Criteria**:
- ✅ State updates trigger UI refresh
- ✅ Async operations don't block UI
- ✅ Error handling works
- ✅ Preview updates automatically

---

## Progress Tracking

### Week 1
- [ ] Task 1.1: Data Models
- [ ] Task 1.2: Query Generator - Basic
- [ ] Task 1.3: Query Generator - PIVOT
- [ ] Task 1.4: Pivot Executor
- [ ] Task 1.5: Unit Tests

### Week 2
- [ ] Task 2.1: Dialog Layout
- [ ] Task 2.2: Field List Panel
- [ ] Task 2.3: Drop Zones
- [ ] Task 2.4: Drag-and-Drop

### Week 3
- [ ] Task 2.5: Preview Grid
- [ ] Task 2.6: State Management
- [ ] Integration testing
- [ ] Bug fixes

---

## Daily Standup Template

### What was completed yesterday?
- Task X.Y: [Description]
- Fixed bug in [component]

### What will be worked on today?
- Task X.Y: [Description]
- Expected completion: [time]

### Any blockers?
- [Issue description]
- Help needed with [specific problem]

---

## Risk Register

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| DuckDB PIVOT syntax issues | High | Medium | Test early, have fallback to GROUP BY |
| Drag-and-drop complexity | Medium | High | Use proven Compose patterns, simplify if needed |
| Performance with large pivots | High | Medium | Implement preview mode, pagination |
| UI complexity overwhelming users | Medium | Medium | Progressive disclosure, start simple |

---

## Definition of Done

A task is considered DONE when:
- ✅ Code is written and compiles
- ✅ Unit tests pass (80%+ coverage)
- ✅ Integration tests pass
- ✅ Code is reviewed (self-review minimum)
- ✅ Documentation is updated
- ✅ Manual testing completed
- ✅ No known critical bugs
- ✅ Performance acceptable (<1 second for typical operations)

---

## Next Actions

1. **Create feature branch**: `git checkout -b feature/pivot-table-builder`
2. **Start with Task 1.1**: Data models (4 hours)
3. **Commit frequently**: After each subtask
4. **Test as you go**: Write tests alongside implementation
5. **Document decisions**: Update this file with learnings

**Let's start building!** 🚀
