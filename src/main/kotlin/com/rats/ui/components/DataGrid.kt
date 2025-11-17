@file:OptIn(ExperimentalFoundationApi::class)

package com.rats.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rats.core.QueryResult

data class CellPosition(val row: Int, val col: Int)

@Composable
fun DataGrid(
    data: QueryResult,
    onCellEdit: ((rowIndex: Int, columnIndex: Int, newValue: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val verticalListState = rememberLazyListState()

    // Selection state
    var selectedCells by remember { mutableStateOf(setOf<CellPosition>()) }
    var editingCell by remember { mutableStateOf<CellPosition?>(null) }
    var editText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Column(modifier = modifier) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            // Row number header
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Column headers
            data.columns.forEach { column ->
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline)
                        .padding(8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = column,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Divider()

        // Data rows with virtual scrolling and vertical scrollbar
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = verticalListState,
                modifier = Modifier.fillMaxSize().padding(end = 12.dp)
            ) {
                itemsIndexed(data.rows) { rowIndex, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState)
                            .background(
                                if (rowIndex % 2 == 0)
                                    MaterialTheme.colorScheme.surface
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                    ) {
                        // Row number
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (rowIndex + 1).toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Data cells
                        row.forEachIndexed { colIndex, cellValue ->
                            val cellPos = CellPosition(rowIndex, colIndex)
                            val isSelected = cellPos in selectedCells
                            val isEditing = editingCell == cellPos
                            val displayValue = cellValue?.toString() ?: ""

                            Box(
                                modifier = Modifier
                                    .width(150.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.5.dp,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outlineVariant
                                    )
                                    .background(
                                        if (isSelected && !isEditing)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else
                                            Color.Transparent
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            // Single click to select (Ctrl+click for multi-select)
                                            selectedCells = setOf(cellPos)
                                            editingCell = null
                                        },
                                        onDoubleClick = {
                                            // Double click to edit directly
                                            selectedCells = setOf(cellPos)
                                            editText = displayValue
                                            editingCell = cellPos
                                        }
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (isEditing) {
                                    BasicTextField(
                                        value = editText,
                                        onValueChange = { editText = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                            .onKeyEvent { keyEvent ->
                                                when {
                                                    keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyUp -> {
                                                        // Save on Enter
                                                        if (editText != displayValue) {
                                                            onCellEdit?.invoke(rowIndex, colIndex, editText)
                                                        }
                                                        editingCell = null
                                                        true
                                                    }
                                                    keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyUp -> {
                                                        // Cancel on Escape
                                                        editingCell = null
                                                        editText = displayValue
                                                        true
                                                    }
                                                    keyEvent.key == Key.Tab && keyEvent.type == KeyEventType.KeyUp -> {
                                                        // Save and move to next cell
                                                        if (editText != displayValue) {
                                                            onCellEdit?.invoke(rowIndex, colIndex, editText)
                                                        }
                                                        editingCell = null
                                                        // Move to next cell
                                                        val nextCol = if (colIndex + 1 < data.columns.size) colIndex + 1 else 0
                                                        val nextRow = if (nextCol == 0 && rowIndex + 1 < data.rows.size) rowIndex + 1 else rowIndex
                                                        selectedCells = setOf(CellPosition(nextRow, nextCol))
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            }
                                    )

                                    LaunchedEffect(Unit) {
                                        focusRequester.requestFocus()
                                    }
                                } else {
                                    Text(
                                        text = displayValue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Vertical scrollbar
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(
                    scrollState = verticalListState
                )
            )
        }

        // Horizontal scrollbar indicator
        HorizontalScrollbar(
            modifier = Modifier.fillMaxWidth().height(12.dp),
            adapter = rememberScrollbarAdapter(horizontalScrollState)
        )

        // Edit button for selected cells
        if (selectedCells.isNotEmpty() && editingCell == null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val firstCell = selectedCells.first()
                        val currentValue = data.rows.getOrNull(firstCell.row)?.getOrNull(firstCell.col)?.toString() ?: ""
                        editText = currentValue
                        editingCell = firstCell
                    }
                ) {
                    Text("Edit Cell (F2)")
                }

                if (selectedCells.size > 1) {
                    var bulkValue by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = bulkValue,
                        onValueChange = { bulkValue = it },
                        label = { Text("Set value for ${selectedCells.size} cells") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            selectedCells.forEach { cell ->
                                onCellEdit?.invoke(cell.row, cell.col, bulkValue)
                            }
                            selectedCells = emptySet()
                            bulkValue = ""
                        },
                        enabled = bulkValue.isNotEmpty()
                    ) {
                        Text("Apply to All")
                    }
                }

                Text(
                    text = "Selected: ${selectedCells.size} cell(s)",
                    modifier = Modifier.align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // Global key handler for F2 to edit
    LaunchedEffect(selectedCells, editingCell) {
        if (selectedCells.size == 1 && editingCell == null) {
            // F2 key handling would go here if we add global key listener
        }
    }
}

@Composable
fun EmptyDataGrid(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No Data Loaded",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Import a CSV or Excel file to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
