# RAT - Data Analysis Tool

**RAT** (Re-written Analysis Tool) is a desktop application built with Kotlin Compose Desktop that provides Excel-like functionality for working with tabular data. It uses DuckDB Java client as the computation core for high-performance data processing.

## Overview

RAT is designed to handle large datasets efficiently on desktop machines. It provides a familiar spreadsheet-like interface with powerful analytical capabilities, all running locally without cloud dependencies.

## Key Features

- **File Import**: Support for CSV, Excel (XLS, XLSX, XLSM) with automatic schema inference
- **Real-time Progress**: Live progress tracking during file imports
- **Data Viewing**: Excel-like grid interface with virtual scrolling for large datasets
- **Inline Editing**: Click any cell to edit its value directly in the grid
- **Add Rows**: Add new empty rows to the dataset with default values
- **Data Manipulation**: Sorting, filtering, cell operations
- **Statistical Analysis**: Comprehensive column statistics (count, mean, median, stddev, quartiles)
- **Export**: Save to CSV or Excel with format options
- **Save/Save As**: Overwrite original file or save to new location
- **Save Filtered Data**: When filters are applied, save exports only the filtered dataset
- **Memory Efficient**: DuckDB's columnar storage and lazy evaluation

## Target Users

- Data analysts working with large CSV/Excel files
- Researchers needing quick statistical summaries
- Business users requiring local data processing without cloud upload
- Anyone needing Excel-like functionality with better performance

## Supported Platforms

- macOS (Intel and Apple Silicon)
- Windows
- Linux

## Quick Start

```bash
# Prerequisites: JDK 17+
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"  # macOS

# Build and run
./gradlew run

# Or create distributable
./gradlew packageDmg     # macOS
./gradlew packageMsi     # Windows
./gradlew packageDeb     # Linux
```

## Documentation

- [Architecture Overview](ARCHITECTURE.md) - High-level system design
- [Technical Details](TECHNICAL.md) - Implementation specifics and APIs
- [CLAUDE.md](../CLAUDE.md) - AI assistant guidance for code contributions

## License

MIT License - See LICENSE file for details.
