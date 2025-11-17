package com.rats.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppToolbar(
    onImportClick: () -> Unit,
    onSortClick: () -> Unit,
    onFilterClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveAsClick: () -> Unit,
    onExportClick: () -> Unit,
    onAddRowClick: () -> Unit,
    onResetClick: () -> Unit,
    hasData: Boolean,
    hasFilePath: Boolean,
    isFiltered: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App title
            Text(
                text = "RATS",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(24.dp))

            // Import button
            Button(
                onClick = onImportClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.FileOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Save button
            OutlinedButton(
                onClick = onSaveClick,
                enabled = hasData && hasFilePath
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Save As button
            OutlinedButton(
                onClick = onSaveAsClick,
                enabled = hasData
            ) {
                Icon(
                    Icons.Default.SaveAs,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save As")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sort button
            OutlinedButton(
                onClick = onSortClick,
                enabled = hasData
            ) {
                Icon(
                    Icons.Default.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sort")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Filter button
            OutlinedButton(
                onClick = onFilterClick,
                enabled = hasData
            ) {
                Icon(
                    Icons.Default.FilterAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Filter")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Stats button
            OutlinedButton(
                onClick = onStatsClick,
                enabled = hasData
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stats")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Add Row button
            OutlinedButton(
                onClick = onAddRowClick,
                enabled = hasData
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Row")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Export button
            OutlinedButton(
                onClick = onExportClick,
                enabled = hasData
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export")
            }

            if (isFiltered) {
                Spacer(modifier = Modifier.width(8.dp))

                // Reset button
                Button(
                    onClick = onResetClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
fun StatusBar(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
