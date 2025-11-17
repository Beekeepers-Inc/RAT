@file:OptIn(ExperimentalMaterial3Api::class)

package com.rats.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rats.import.PreviewData

@Composable
fun PreviewDialog(
    previewData: PreviewData,
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(800.dp)
                .heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preview: $fileName",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preview info
                Text(
                    text = "${previewData.columns.size} columns, showing first ${previewData.totalPreviewRows} rows",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Preview table
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        // Header row
                        item {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                previewData.columns.forEach { col ->
                                    Text(
                                        text = col,
                                        modifier = Modifier.weight(1f).padding(4.dp),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                            Divider()
                        }

                        // Data rows
                        itemsIndexed(previewData.rows) { _, row ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                row.forEach { cell ->
                                    Text(
                                        text = cell,
                                        modifier = Modifier.weight(1f).padding(4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onConfirm) {
                        Text("Import")
                    }
                }
            }
        }
    }
}

@Composable
fun SortDialog(
    columns: List<String>,
    onConfirm: (columnName: String, ascending: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColumn by remember { mutableStateOf(columns.firstOrNull() ?: "") }
    var ascending by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(400.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort Data",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Column selection
                Text(
                    text = "Sort by column:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    TextField(
                        value = selectedColumn,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        columns.forEach { column ->
                            DropdownMenuItem(
                                text = { Text(column) },
                                onClick = {
                                    selectedColumn = column
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sort order
                Text(
                    text = "Sort order:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = ascending,
                        onClick = { ascending = true }
                    )
                    Text("Ascending", modifier = Modifier.padding(start = 4.dp))

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = !ascending,
                        onClick = { ascending = false }
                    )
                    Text("Descending", modifier = Modifier.padding(start = 4.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedColumn, ascending) },
                        enabled = selectedColumn.isNotEmpty()
                    ) {
                        Text("Sort")
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingOverlay(
    message: String,
    progress: Int? = null,
    totalRows: Int? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(300.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (progress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (totalRows != null) {
                            "${progress.formatNumber()} / ${totalRows.formatNumber()} rows"
                        } else {
                            "${progress.formatNumber()} rows imported"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

private fun Int.formatNumber(): String {
    return String.format("%,d", this)
}

@Composable
fun StatisticsDialog(
    stats: com.rats.statistics.TableStatistics,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(900.dp)
                .heightIn(max = 700.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Table Statistics",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Rows", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = stats.totalRows.formatNumber(),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Columns", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = stats.totalColumns.toString(),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Column Statistics", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(8.dp))

                // Column stats table
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(stats.columnStats) { _, colStat ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = colStat.columnName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Type: ${colStat.dataType}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Count: ${colStat.count.formatNumber()}", style = MaterialTheme.typography.bodySmall)
                                        Text("Nulls: ${colStat.nullCount.formatNumber()}", style = MaterialTheme.typography.bodySmall)
                                        Text("Distinct: ${colStat.distinctCount.formatNumber()}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column {
                                        Text("Min: ${colStat.min ?: "-"}", style = MaterialTheme.typography.bodySmall)
                                        Text("Max: ${colStat.max ?: "-"}", style = MaterialTheme.typography.bodySmall)
                                        Text("Mean: ${colStat.mean?.let { "%.2f".format(it) } ?: "-"}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column {
                                        Text("Median: ${colStat.median?.let { "%.2f".format(it) } ?: "-"}", style = MaterialTheme.typography.bodySmall)
                                        Text("StdDev: ${colStat.stdDev?.let { "%.2f".format(it) } ?: "-"}", style = MaterialTheme.typography.bodySmall)
                                        Text("Q25-Q75: ${colStat.q25?.let { "%.2f".format(it) } ?: "-"} - ${colStat.q75?.let { "%.2f".format(it) } ?: "-"}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun ExportDialog(
    onConfirm: (format: String, includeHeader: Boolean, sheetName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("csv") }
    var includeHeader by remember { mutableStateOf(true) }
    var sheetName by remember { mutableStateOf("Data") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(400.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Export Data",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Format selection
                Text("Format:", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedFormat == "csv",
                        onClick = { selectedFormat = "csv" }
                    )
                    Text("CSV", modifier = Modifier.padding(start = 4.dp))

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = selectedFormat == "excel",
                        onClick = { selectedFormat = "excel" }
                    )
                    Text("Excel", modifier = Modifier.padding(start = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Include header checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeHeader,
                        onCheckedChange = { includeHeader = it }
                    )
                    Text("Include header row", modifier = Modifier.padding(start = 8.dp))
                }

                // Excel-specific options
                if (selectedFormat == "excel") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sheet Name:", style = MaterialTheme.typography.labelLarge)
                    TextField(
                        value = sheetName,
                        onValueChange = { sheetName = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(selectedFormat, includeHeader, sheetName) }) {
                        Text("Export")
                    }
                }
            }
        }
    }
}

@Composable
fun FilterDialog(
    columns: List<String>,
    currentFilters: List<com.rats.ui.FilterCondition>,
    onConfirm: (List<com.rats.ui.FilterCondition>) -> Unit,
    onDismiss: () -> Unit
) {
    var filters by remember { mutableStateOf(currentFilters.toMutableList()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Data",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add filter button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = {
                        filters = (filters + com.rats.ui.FilterCondition(
                            column = columns.firstOrNull() ?: "",
                            operator = "=",
                            value = ""
                        )).toMutableList()
                    }) {
                        Text("Add Filter Rule")
                    }

                    if (filters.isNotEmpty()) {
                        OutlinedButton(onClick = { filters = mutableListOf() }) {
                            Text("Clear All")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filter rules
                if (filters.isEmpty()) {
                    Text(
                        text = "No filter rules. Click 'Add Filter Rule' to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(filters) { index, filter ->
                            FilterRuleRow(
                                filter = filter,
                                columns = columns,
                                onFilterChange = { newFilter ->
                                    filters = filters.toMutableList().apply { this[index] = newFilter }
                                },
                                onRemove = {
                                    filters = filters.toMutableList().apply { removeAt(index) }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(filters) },
                        enabled = filters.isNotEmpty() && filters.all { it.value.isNotBlank() }
                    ) {
                        Text("Apply Filter")
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRuleRow(
    filter: com.rats.ui.FilterCondition,
    columns: List<String>,
    onFilterChange: (com.rats.ui.FilterCondition) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column selector
        var columnExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = columnExpanded,
            onExpandedChange = { columnExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            TextField(
                value = filter.column,
                onValueChange = {},
                readOnly = true,
                label = { Text("Column") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = columnExpanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = columnExpanded,
                onDismissRequest = { columnExpanded = false }
            ) {
                columns.forEach { column ->
                    DropdownMenuItem(
                        text = { Text(column) },
                        onClick = {
                            onFilterChange(filter.copy(column = column))
                            columnExpanded = false
                        }
                    )
                }
            }
        }

        // Operator selector
        var operatorExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = operatorExpanded,
            onExpandedChange = { operatorExpanded = it },
            modifier = Modifier.width(100.dp)
        ) {
            TextField(
                value = filter.operator,
                onValueChange = {},
                readOnly = true,
                label = { Text("Op") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = operatorExpanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = operatorExpanded,
                onDismissRequest = { operatorExpanded = false }
            ) {
                listOf("=", "!=", ">", "<", ">=", "<=", "LIKE").forEach { op ->
                    DropdownMenuItem(
                        text = { Text(op) },
                        onClick = {
                            onFilterChange(filter.copy(operator = op))
                            operatorExpanded = false
                        }
                    )
                }
            }
        }

        // Value input
        TextField(
            value = filter.value,
            onValueChange = { onFilterChange(filter.copy(value = it)) },
            label = { Text("Value") },
            modifier = Modifier.weight(1f)
        )

        // Remove button
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove")
        }
    }
}

private fun Long.formatNumber(): String {
    return String.format("%,d", this)
}
