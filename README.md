# RAT - Data Analysis Tool (Kotlin Desktop)

A desktop data analysis application built with Kotlin Compose Desktop and DuckDB Java client, providing an Excel-like interface for importing, viewing, and analyzing CSV and Excel files.

## Features

- **File Import**: Support for CSV, XLS, XLSX, XLSM formats
- **Real-time Progress**: Live progress tracking during file imports with row counts
- **High Performance**: Optimized with DuckDB's parallel processing capabilities
- **Data Viewing**: Excel-like grid interface with virtual scrolling
- **Data Sorting**: Multi-column sorting capabilities
- **Type Detection**: Automatic schema inference for imported data
- **Loading Screens**: User-friendly loading overlays with progress indicators

## Technology Stack

- **UI Framework**: Kotlin Compose Desktop
- **Backend**: Kotlin/JVM
- **Database**: DuckDB (in-memory analytical database via JDBC)
- **File Parsing**:
  - CSV: DuckDB native `read_csv_auto` + OpenCSV for preview
  - Excel: Apache POI
- **Build System**: Gradle with Kotlin DSL

## Prerequisites

- JDK 17 or higher (install via SDKMAN, Homebrew, or download from [Adoptium](https://adoptium.net/))
- Gradle 8.4 or higher (included via wrapper, no manual installation needed)

### Installing Java on macOS

```bash
# Using Homebrew
brew install openjdk@17

# Or using SDKMAN
sdk install java 17.0.9-tem
```

### Installing Java on Windows

Download and install from [Adoptium](https://adoptium.net/temurin/releases/?version=17)

## Quick Start

### Installation

```bash
# Clone the repository
cd rat

# Build the project
./gradlew build

# Run the application
./gradlew run
```

### Building Distributions

```bash
# Create native distributions
./gradlew packageDmg     # macOS
./gradlew packageMsi     # Windows
./gradlew packageDeb     # Linux
```

## Project Structure

```
rat/
├── src/main/kotlin/com/rats/
│   ├── Main.kt                    # Application entry point
│   ├── core/
│   │   └── DuckDBCore.kt          # DuckDB connection and queries
│   ├── import/
│   │   └── FileImporter.kt        # CSV/Excel import logic
│   ├── editor/
│   │   └── DataEditor.kt          # Sorting and data manipulation
│   └── ui/
│       ├── AppState.kt            # Application state management
│       ├── MainWindow.kt          # Main window composition
│       └── components/
│           ├── DataGrid.kt        # Data grid component
│           ├── Dialogs.kt         # Import/Sort dialogs
│           └── Toolbar.kt         # Toolbar and status bar
├── build.gradle.kts               # Build configuration
├── settings.gradle.kts            # Project settings
└── gradle.properties              # Gradle properties
```

## Architecture

The application follows a clean architecture pattern:

- **Core Layer**: DuckDB integration for data storage and querying
- **Import Layer**: File parsing and data import logic
- **Editor Layer**: Data manipulation operations
- **UI Layer**: Compose Desktop UI components

All data is stored in DuckDB in-memory database with 4GB memory limit and 4 threads configured for optimal performance.

## Usage

1. **Import Data**: Click "Import" to open a file dialog and select a CSV or Excel file
2. **Preview**: Review the first 10 rows of your data before importing
3. **Import**: Confirm to import the entire file with real-time progress tracking
4. **View**: Explore your data in the Excel-like grid with scrolling
5. **Sort**: Click "Sort" to order your data by any column

## Performance

- **CSV Import**: Uses DuckDB's native `read_csv_auto` with parallel processing for fast imports
- **Excel Import**: Transaction batching (1000 rows/batch) for optimal performance
- **Query Execution**: DuckDB's OLAP-optimized queries for fast data retrieval
- **Virtual Scrolling**: Efficient rendering for large datasets

## License

See LICENSE file for details.
