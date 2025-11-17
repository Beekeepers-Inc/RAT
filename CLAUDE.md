# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

**RAT** is a desktop data analysis application built with Kotlin Compose Desktop. It provides an Excel-like interface for importing, viewing, and analyzing CSV and Excel files. The application uses DuckDB Java client as the analytical database engine for high-performance data processing.

## Core Value Propositions

- **Handle Massive Datasets**: Efficiently process millions of rows using DuckDB's analytical engine
- **Instant Results**: Sub-second query performance with DuckDB's OLAP optimization
- **No Cloud Upload**: All processing happens locally for complete data privacy
- **Familiar Interface**: Excel-like spreadsheet UX for data analysis
- **Cross-Platform**: Runs on macOS, Windows, and Linux via Compose Desktop

## Tech Stack

- **UI Framework**: Kotlin Compose Desktop (JetBrains Compose Multiplatform)
- **Language**: Kotlin/JVM
- **Database Engine**: DuckDB (via JDBC client)
- **File Parsing**: Apache POI (Excel), OpenCSV (CSV preview), DuckDB native (CSV import)
- **Build System**: Gradle with Kotlin DSL
- **Target Platforms**: macOS, Windows, Linux

## Development Commands

### Build and Run

```bash
# Build the project
./gradlew build

# Run the application
./gradlew run

# Clean build artifacts
./gradlew clean
```

### Create Distributions

```bash
# macOS DMG
./gradlew packageDmg

# Windows MSI
./gradlew packageMsi

# Linux DEB
./gradlew packageDeb

# All formats
./gradlew package
```

### Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.rats.core.DuckDBCoreTest"
```

## Architecture

### Module Structure

**1. Core Module** (`src/main/kotlin/com/rats/core/`)
- DuckDB connection management
- Query execution with type conversion
- Table metadata retrieval
- Configuration (memory limits, threading)

**2. Import Module** (`src/main/kotlin/com/rats/import/`)
- File format detection (CSV, Excel)
- File preview generation
- CSV import using DuckDB native `read_csv_auto`
- Excel import with Apache POI and transaction batching
- Progress reporting callbacks

**3. Editor Module** (`src/main/kotlin/com/rats/editor/`)
- Data sorting (single and multi-column)
- Row filtering with SQL WHERE clauses
- Column operations (add, delete)
- **Inline cell editing** - Click any cell to edit, changes persist to DuckDB
- **Add empty rows** - Insert new rows with type-appropriate default values
- Row deletion

**4. Statistics Module** (`src/main/kotlin/com/rats/statistics/`)
- Column statistics (count, nulls, distinct, min, max)
- Statistical measures (mean, median, stddev, variance)
- Quartiles (Q25, Q75)
- Correlation between columns
- Uses DuckDB's optimized aggregate functions

**5. Export Module** (`src/main/kotlin/com/rats/export/`)
- CSV export using DuckDB's native COPY command
- Excel export with Apache POI
- Query result export
- **Filtered data export** - When filters are applied, exports only filtered rows

**6. UI Module** (`src/main/kotlin/com/rats/ui/`)
- Compose Desktop components
- State management with `mutableStateOf`
- **Editable data grid** with virtual scrolling and inline cell editing
- Dialog components (preview, sort, filter, statistics, export)
- Toolbar with Save/Save As, Import, Sort, Filter, Stats, Add Row, Export buttons
- Status bar with operation feedback

### Data Flow

1. **Import**: User selects file → Preview generated → User confirms → Previous table cleaned up → DuckDB imports data
2. **Query**: UI requests data → DuckDBCore executes query → Results converted to Kotlin objects
3. **Sort**: User selects column → Editor creates sorted table → UI refreshes
4. **Filter**: User sets conditions → AppState creates SQL view → Grid shows filtered data
5. **Edit Cell**: User clicks cell → EditableCell enters edit mode → User types → Focus lost → DuckDB UPDATE executed → Grid reloads
6. **Add Row**: User clicks Add Row → Editor inserts row with defaults → Grid reloads with new row
7. **Save**: User clicks Save → Current table (filtered or not) exported to original file path
8. **Save Filtered Data**: When filters applied, save/export operations automatically use the filtered view
9. **State Update**: UI state changes → Compose recomposes affected components

### Key Design Decisions

**Why DuckDB**: Analytical database optimized for OLAP queries, perfect for Excel-like operations. Uses JDBC driver for Kotlin/JVM integration.

**Why Compose Desktop**: Modern declarative UI framework with reactive state management. Cross-platform without needing browser/WebView.

**State Management**: Uses Compose's `mutableStateOf` for reactive UI updates. AppState class holds all application state.

**Coroutines**: Long-running operations (import, sort) run on `Dispatchers.IO` to keep UI responsive.

## Important Considerations

**DuckDB Configuration**
- 4GB memory limit configured for desktop use
- 4 threads for parallel processing
- In-memory database (no persistence between sessions)

**File Format Support**
- CSV: Native DuckDB `read_csv_auto` with parallel processing
- Excel: Apache POI with transaction batching (1000 rows/batch)
- Automatic schema inference

**Inline Cell Editing**
- Click any cell to enter edit mode (BasicTextField replaces Text)
- Focus change triggers save (onFocusChanged event)
- Uses ROW_NUMBER() window function to identify rows without primary key
- Immediate persistence to DuckDB via UPDATE query
- Grid reloads after each edit to ensure consistency

**Adding Rows**
- Inserts row with type-appropriate default values:
  - INTEGER/BIGINT → 0
  - DOUBLE/FLOAT → 0.0
  - VARCHAR/TEXT → '' (empty string)
- Row appears at bottom of table
- Cells can be edited immediately after insertion

**Filtered Data Operations**
- Filters create SQL views (not new tables) for memory efficiency
- When filtered, `currentTable` points to the view name
- Save/Export operations use `currentTable`, so filtered data is automatically exported
- Original table preserved in `originalTable` for reset functionality

**Error Handling**
- All operations wrapped in try-catch
- User-friendly error dialogs
- Proper resource cleanup with `use` blocks
- Status messages show operation results

**Performance Optimizations**
- Virtual scrolling in LazyColumn for large datasets
- Transaction batching for Excel imports
- Parallel CSV processing in DuckDB
- Lazy loading of table data
- SQL views for filtering (no data duplication)

## Code Style

- Kotlin official style guide
- Compose best practices for state management
- Immutable data classes for domain models
- Extension functions for utility operations
- Coroutines for async operations

## Dependencies

Key dependencies managed in `build.gradle.kts`:
- `org.duckdb:duckdb_jdbc:1.1.3` - DuckDB JDBC driver
- `org.apache.poi:poi-ooxml:5.2.5` - Excel file support
- `com.opencsv:opencsv:5.9` - CSV parsing for preview
- `compose.desktop.currentOs` - Compose Desktop UI
- `compose.material3` - Material Design 3 components
