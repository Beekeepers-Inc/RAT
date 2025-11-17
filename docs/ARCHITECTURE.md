# RAT Architecture

This document describes the high-level architecture of RAT, a Kotlin Desktop application for data analysis.

## System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Compose Desktop UI                       │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐  │
│  │  Import  │   Save   │   Sort   │  Filter  │  Export  │  │
│  └────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┘  │
│       │          │          │          │          │         │
│  ┌────▼──────────▼──────────▼──────────▼──────────▼─────┐  │
│  │                    AppState (Reactive)                │  │
│  │         Compose mutableStateOf for UI binding         │  │
│  └───────────────────────┬───────────────────────────────┘  │
└──────────────────────────┼──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                      Service Layer                           │
│  ┌─────────┐  ┌─────────┐  ┌───────────┐  ┌─────────────┐  │
│  │Importer │  │ Editor  │  │Statistics │  │  Exporter   │  │
│  └────┬────┘  └────┬────┘  └─────┬─────┘  └──────┬──────┘  │
└───────┼────────────┼─────────────┼───────────────┼──────────┘
        │            │             │               │
┌───────▼────────────▼─────────────▼───────────────▼──────────┐
│                       DuckDB Core                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                  JDBC Connection                        │ │
│  │                  In-Memory Database                     │ │
│  │                  SQL Query Engine                       │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Core Modules

### 1. UI Layer (`com.rats.ui`)

**Purpose**: User interface and interaction handling

**Components**:
- `Main.kt` - Application entry point and window configuration
- `AppState.kt` - Centralized reactive state management
- `MainWindow.kt` - Main composable with event handlers
- `components/` - Reusable UI components (DataGrid, Dialogs, Toolbar)

**Key Patterns**:
- Reactive state with Compose's `mutableStateOf`
- Coroutines for async operations
- SwingUtilities for native file dialogs

### 2. Import Module (`com.rats.import`)

**Purpose**: File ingestion and schema detection

**Capabilities**:
- File format detection (CSV, Excel)
- Data preview generation
- Progress reporting via callbacks
- CSV import using DuckDB native `read_csv_auto`
- Excel import with Apache POI and transaction batching

**Data Flow**:
```
User selects file → Format detection → Preview generation → User confirms
→ Cleanup previous table → Import with progress → Load into grid
```

### 3. Editor Module (`com.rats.editor`)

**Purpose**: Data manipulation operations

**Operations**:
- Row reordering (sorting by one or more columns)
- Data filtering with SQL WHERE clauses
- Column operations (add, delete)
- Cell updates
- Row deletion

**Implementation**: All operations use SQL queries on DuckDB tables, ensuring consistency and leveraging DuckDB's optimization.

### 4. Statistics Module (`com.rats.statistics`)

**Purpose**: Analytical computations

**Statistics Provided**:
- Count, null count, distinct count
- Min, max, mean, median
- Standard deviation, variance
- Quartiles (Q25, Q75)
- Correlation between columns

**Optimization**: Uses DuckDB's built-in aggregate functions for performance.

### 5. Export Module (`com.rats.export`)

**Purpose**: Data output to various formats

**Capabilities**:
- CSV export using DuckDB's COPY command
- Excel export with Apache POI
- Query result export
- Format-specific options (headers, sheet names)

### 6. DuckDB Core (`com.rats.core`)

**Purpose**: Database operations and connection management

**Features**:
- JDBC connection to in-memory DuckDB
- Query execution with type conversion
- Table metadata retrieval
- Memory and thread configuration
- SQL injection prevention via table name sanitization

## Design Principles

### Separation of Concerns
Each module has a single responsibility:
- UI handles presentation
- Services handle business logic
- DuckDB Core handles data persistence

### Reactive State Management
UI automatically updates when state changes:
```kotlin
var currentData by mutableStateOf<QueryResult?>(null)
// UI recomposes when currentData changes
```

### Async Operations
Long-running tasks execute on background threads:
```kotlin
withContext(Dispatchers.IO) {
    // Database operations
}
```

### Resource Management
Proper cleanup using Kotlin's `use` blocks:
```kotlin
connection.createStatement().use { stmt ->
    stmt.executeQuery(query).use { rs ->
        // Process results
    }
}
```

## Data Flow Patterns

### Import Flow
1. User clicks Import → File dialog opens
2. File selected → Preview generated (first 10 rows)
3. User confirms → Previous table dropped
4. Import executes with progress callbacks
5. Table info loaded → Grid populated

### Query Flow
1. UI requests data → AppState calls DuckDBCore
2. SQL executed → ResultSet converted to QueryResult
3. QueryResult stored in mutableStateOf
4. Compose recomposes affected UI components

### Edit Flow (Inline Cell Editing)
1. User clicks cell → EditableCell enters edit mode
2. User types new value → Local state updated
3. User clicks outside → onFocusChanged triggered
4. Editor.updateCell() called → DuckDB UPDATE executed
5. Table data reloaded → Grid recomposes with new value
6. Status message shows "Updated cell at row X, column Y"

### Add Row Flow
1. User clicks "Add Row" button
2. Editor.addEmptyRow() called → DuckDB INSERT with default values
3. Table data reloaded → New row appears at bottom
4. User can edit the new row cells inline

### Save Flow (Including Filtered Data)
1. User clicks Save/Save As → Current table exported (filtered or not)
2. If filtered view active → Only filtered rows are saved
3. DuckDB COPY command executes → File written
4. Status shows "Saved X rows"

### Export Flow
1. User chooses format/options
2. Save dialog opens for file path
3. DuckDB COPY command (CSV) or POI write (Excel)
4. Success message shown

## Performance Considerations

### Memory Management
- DuckDB configured with 4GB memory limit
- 4 threads for parallel processing
- Lazy evaluation of query results
- Virtual scrolling in UI (LazyColumn)

### Large Dataset Handling
- Streaming query results
- Transaction batching for Excel imports (1000 rows/batch)
- Native DuckDB CSV parser (parallel, SIMD-optimized)

### UI Responsiveness
- All database operations on Dispatchers.IO
- Progress callbacks for long operations
- Loading overlays during processing

## Extension Points

### Adding New File Formats
1. Extend `FileFormat` enum
2. Add detection logic in `detectFileFormat`
3. Implement preview and import methods
4. Handle in `importFile` switch statement

### Adding New Statistics
1. Add method to `StatisticsCalculator`
2. Create SQL query using DuckDB functions
3. Add UI dialog for displaying results

### Adding New Data Operations
1. Add method to `DataEditor`
2. Implement using SQL on DuckDB
3. Wire up to UI via AppState

## Security Considerations

- Table name sanitization prevents SQL injection
- File path validation in importers
- No network access (offline-only)
- Local file system access controlled by user selection
