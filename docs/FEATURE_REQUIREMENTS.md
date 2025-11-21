# RATS Feature Requirements - Excel-Inspired Data Analysis

**Goal**: Make data analysis as intuitive as Excel while leveraging DuckDB's analytical power for millions of rows.

**Philosophy**: Focus on READING data and GETTING INSIGHTS. No data editing, pure analytical workflows.

---

## TIER 1: Critical Foundation (Must Have)

### 1. Visual Pivot Table Builder ⭐ PRIORITY #1

**Why**: Pivot Tables are THE #1 most critical Excel feature, mentioned universally. They transform raw data into insights in minutes.

**Requirements**:

**UI Components**:
- Drag-and-drop interface with 4 drop zones:
  - **Row Fields** - Group by these dimensions (e.g., Product, Region)
  - **Column Fields** - Spread these across columns (e.g., Year, Quarter)
  - **Value Fields** - What to calculate (e.g., Sum of Sales)
  - **Filters** - Pre-filter the data before pivoting

- Available aggregations:
  - SUM, COUNT, AVERAGE, MIN, MAX, MEDIAN, STDEV
  - DISTINCT COUNT
  - PERCENTILE (25th, 50th, 75th)

- Grouping capabilities:
  - **Date grouping**: Year, Quarter, Month, Week, Day
  - **Number bucketing**: Custom ranges (0-100, 100-200, etc.)
  - **Text grouping**: First letter, prefix patterns

**Technical Implementation**:
```sql
-- DuckDB PIVOT example
SELECT * FROM (
  SELECT region, quarter, SUM(sales) as total_sales
  FROM sales_data
  WHERE year = 2024
  GROUP BY region, quarter
) PIVOT (
  SUM(total_sales) FOR quarter IN ('Q1', 'Q2', 'Q3', 'Q4')
);
```

**User Experience**:
- Preview updates in real-time as user drags fields
- Show row/column totals and grand totals
- Export pivot results to new view or CSV
- Save pivot configurations for reuse
- "Suggested pivots" based on data types (auto-detect dates, categories, numerics)

**Success Metrics**: User can create a pivot table in <30 seconds without any SQL knowledge

---

### 2. Multi-Criteria Filtering System

**Why**: SUMIFS is the "#1 must-know formula" - analysts need multi-criteria filtering daily for answering "How much did region A sell of product B in month C?"

**Requirements**:

**Visual Filter Builder**:
- Point-and-click interface to build complex WHERE clauses
- Filter types per data type:
  - **Text**: equals, contains, starts with, ends with, regex
  - **Numbers**: =, >, <, >=, <=, between, top N, bottom N
  - **Dates**: specific date, date range, relative (last 7 days, this month, this year)
  - **Null handling**: is null, is not null

- **Combine filters with AND/OR logic**:
  ```
  [Region] equals "West"
  AND [Sales] greater than 10000
  AND [Date] in last 30 days
  ```

**Quick Filters Panel**:
- Show all unique values for text columns (checkbox list)
- Sliders for numeric ranges
- Calendar picker for dates
- Search box to filter the filter list itself

**Technical Implementation**:
```sql
-- Multi-criteria example
SELECT * FROM sales_data
WHERE region = 'West'
  AND sales > 10000
  AND date >= CURRENT_DATE - INTERVAL 30 DAYS
  AND product IN ('Widget A', 'Widget B')
```

**User Experience**:
- Show count of matching rows as filters are applied
- "Clear all filters" button
- Save filter combinations as named views
- Show active filters as removable chips/tags
- Filter memory - remember last used filters per table

---

### 3. Conditional Formatting (Visual Insights)

**Why**: Used by 90% of analysts - condenses complex datasets into instantly understandable visuals

**Requirements**:

**Formatting Types**:

1. **Color Scales (Heat Maps)**:
   - 2-color gradient (low = red, high = green)
   - 3-color gradient (low = red, mid = yellow, high = green)
   - Custom color selection
   - Apply to entire column or selected range

2. **Data Bars (In-Cell Bar Charts)**:
   - Horizontal bars where length = value
   - Positive/negative bars (positive right, negative left)
   - Show value label on bar
   - Gradient or solid fill

3. **Icon Sets**:
   - Traffic lights (🔴 🟡 🟢)
   - Arrows (🔺 ➡ 🔻)
   - Stars (⭐⭐⭐)
   - Flags/Priority indicators
   - Custom thresholds (e.g., >90% = green, 50-90% = yellow, <50% = red)

4. **Text Color/Background**:
   - Highlight cells meeting criteria
   - Entire row highlighting based on column value
   - Alternating row colors for readability

**Rule Builder**:
- Visual rule creator: "If [Column] is [greater than] [1000] then [green background]"
- Multiple rules per column with priority ordering
- Formula-based rules: `=[Sales] > [Target]`
- Copy formatting rules between columns

**Technical Implementation**:
- Client-side rendering (no database changes)
- Fast recalculation on filter/sort changes
- Export view with formatting preserved (HTML/PDF)

**Business Use Cases**:
- Sales performance (flag underperforming regions)
- Budget tracking (highlight over-budget items)
- Inventory alerts (low stock warnings)
- KPI dashboards (at-a-glance status)

---

### 4. Statistical Summary Panel

**Why**: Analysts need quick stats for selected data - Excel shows SUM/COUNT/AVG in status bar, we should do better

**Requirements**:

**Always-Visible Stats Panel**:
When user selects column(s) or range, show:
- Count (total rows, non-null)
- Sum, Average, Median
- Min, Max, Range
- Standard Deviation, Variance
- 25th, 50th, 75th Percentiles
- Distinct Count
- Null count and percentage

**Column Profiling**:
- Auto-detect data type
- Show distribution histogram
- Top 10 most frequent values
- Identify outliers (values beyond 1.5 * IQR)
- Data quality score (null %, outlier %, duplicates)

**Technical Implementation**:
```sql
-- Quick stats query
SELECT
  COUNT(*) as count,
  COUNT(DISTINCT column_name) as distinct_count,
  SUM(column_name) as sum,
  AVG(column_name) as avg,
  MEDIAN(column_name) as median,
  STDDEV(column_name) as stdev,
  MIN(column_name) as min,
  MAX(column_name) as max,
  PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY column_name) as p25,
  PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY column_name) as p75
FROM current_table
WHERE column_name IS NOT NULL
```

**User Experience**:
- Instant calculation (DuckDB aggregations are fast)
- Copy stats to clipboard
- Export stats to CSV
- Compare multiple columns side-by-side

---

## TIER 2: High-Value Professional Features

### 5. Chart & Visualization Builder

**Why**: 80%+ of analysts use charts daily - column/line charts are workhorses of business reporting

**Requirements**:

**Chart Types**:
1. **Column/Bar Charts** - Compare values across categories
   - Grouped (side-by-side comparison)
   - Stacked (part-to-whole)
   - 100% Stacked (proportional)

2. **Line Charts** - Trending over time
   - Single or multi-line
   - Limit to 4-5 lines for clarity
   - Area charts (filled lines)

3. **Combo Charts** - Compare different scales
   - Column + Line (e.g., Sales $ + Profit %)
   - Dual Y-axis

4. **Scatter Plots** - Correlation analysis
   - X-Y plotting
   - Trend line option
   - Outlier identification

5. **Pie/Donut Charts** - Proportional breakdown
   - Limit to <7 slices for readability
   - Show percentages

6. **Heat Maps** - Matrix visualization
   - Color-coded cells
   - Great for correlation matrices

**Chart Builder UI**:
- Drag columns to X-axis, Y-axis, Series
- Auto-detect appropriate chart type based on data types
- Live preview as options change
- Interactive legends (click to toggle series)
- Export chart as PNG/SVG
- Embed charts in dashboard

**Technical Implementation**:
- Use existing statistics module for aggregations
- Client-side rendering with Chart.js or similar
- DuckDB materialized views for large datasets
- Responsive sizing

**Business Value**:
- Instant visual insights without Excel export
- Interactive dashboards for executives
- Trend analysis and forecasting

---

### 6. Lookup/Join Operations (Multi-Table Analysis)

**Why**: VLOOKUP appears in 70% of job descriptions - analysts constantly merge data from different sources

**Requirements**:

**Visual Join Builder**:
- Import multiple files (CSV, Excel) into DuckDB
- Visual interface showing tables as boxes with columns
- Drag between columns to create join relationships
- Join types: INNER, LEFT, RIGHT, FULL OUTER
- Preview join results before applying

**Join Configuration**:
- Select which columns to keep from each table
- Rename columns to avoid conflicts
- Handle duplicate column names intelligently
- Show match statistics (matched rows, unmatched rows)

**Technical Implementation**:
```sql
-- Example join
SELECT
  s.order_id,
  s.product,
  s.quantity,
  p.price,
  s.quantity * p.price as total
FROM sales s
LEFT JOIN prices p ON s.product = p.product_name
```

**Common Use Cases**:
- Match customer IDs to names
- Add pricing data to sales records
- Combine regional reports
- Enrich data with lookup tables

**User Experience**:
- "Add table" button to import additional data
- Visual relationship diagram
- Validation warnings (many-to-many joins, unmatched records)
- Save join as reusable view

---

### 7. Interactive Slicers (Dashboard Filtering)

**Why**: Visual filtering with buttons instead of dropdowns - clearly shows what's selected, enables multi-select

**Requirements**:

**Slicer UI**:
- Grid of clickable buttons (one per unique value)
- Multi-select with Ctrl+Click
- "Select All" / "Clear All" buttons
- Show count of items selected
- Visual indication of filtered state

**Slicer Types**:
- **List Slicer** - For categories (Region, Product, Status)
- **Range Slicer** - For numbers (Sales 0-10k, 10k-50k, 50k+)
- **Timeline Slicer** - For dates (Year, Quarter, Month selector)

**Cross-Filtering**:
- One slicer affects multiple views/charts
- Cascade filtering (selecting Region filters City options)
- Show disabled options (grayed out unavailable combinations)

**Technical Implementation**:
- Build WHERE clauses from slicer selections
- Apply to all connected views
- Efficient query with proper indexing

**Business Value**:
- Self-service analytics for non-technical users
- Executive dashboards
- Regional/department/product filtering
- No SQL knowledge required

---

### 8. Date Grouping & Time Intelligence

**Why**: Analysts constantly group dates by year/quarter/month - Excel does this automatically in pivots

**Requirements**:

**Automatic Date Hierarchies**:
When date column detected, auto-create:
- Year
- Quarter (Q1, Q2, Q3, Q4)
- Month (Jan, Feb, Mar, ...)
- Week Number
- Day of Week (Mon, Tue, ...)
- Date (specific day)

**Technical Implementation**:
```sql
-- Date extraction
SELECT
  YEAR(date_col) as year,
  QUARTER(date_col) as quarter,
  MONTH(date_col) as month,
  MONTHNAME(date_col) as month_name,
  WEEK(date_col) as week,
  DAYNAME(date_col) as day_of_week
FROM data
```

**Time Intelligence Calculations**:
- Year-over-Year (YoY) comparison
- Month-over-Month (MoM) comparison
- Rolling averages (30-day, 90-day)
- Year-to-Date (YTD) totals
- Same Period Last Year

**User Experience**:
- Drill-down: Click Year → see Quarters → see Months → see Days
- Drill-up: Aggregate back to higher levels
- Custom fiscal year start (not just Jan 1)
- Relative date filters (Last 7 days, This Month, Last Quarter)

---

### 9. Calculated Fields & Formula Builder

**Why**: Analysts need custom calculations - Excel provides calculated fields in pivots

**Requirements**:

**Formula Builder UI**:
- Visual formula creator (no SQL needed)
- Drag columns into formula
- Common functions library:
  - Math: +, -, *, /, %, ROUND, ABS, POWER
  - Aggregations: SUM, AVG, COUNT, MIN, MAX
  - Text: CONCAT, UPPER, LOWER, TRIM, SUBSTRING
  - Date: DATE_DIFF, DATE_ADD, CURRENT_DATE
  - Conditional: IF, CASE

**Named Calculations**:
- Create calculated column: `Profit = Revenue - Cost`
- Percentage calculations: `Margin% = (Revenue - Cost) / Revenue * 100`
- Ratios: `Conversion Rate = Orders / Visits`
- Conditional logic: `Status = IF(Sales > 10000, 'High', 'Low')`

**Technical Implementation**:
```sql
-- Calculated field examples
SELECT
  *,
  revenue - cost as profit,
  (revenue - cost) / revenue * 100 as profit_margin_pct,
  CASE
    WHEN sales > 10000 THEN 'High'
    WHEN sales > 5000 THEN 'Medium'
    ELSE 'Low'
  END as sales_tier
FROM data
```

**User Experience**:
- Formula preview with sample results
- Error detection and suggestions
- Reusable formula library
- Column naming and formatting
- Use in pivots, filters, and charts

---

## TIER 3: Advanced Professional Features

### 10. Sparklines (Mini Charts in Cells)

**Why**: Compact visual trends within cells - essential for dashboard reports and KPI monitoring

**Requirements**:

**Sparkline Types**:
- **Line** - Show continuous trends (monthly sales)
- **Column** - Show periodic comparisons
- **Win/Loss** - Binary outcomes (positive/negative)

**Configuration**:
- Point to data range for sparkline
- Show high/low points
- Show trend markers
- Custom colors

**Technical Implementation**:
- SVG rendering in table cells
- Auto-scale to cell size
- Update with data changes

---

### 11. Text Functions for Data Cleaning

**Why**: Analysts spend 60-80% of time on data cleaning - provide tools for viewing cleaned data without modifying source

**Requirements**:

**Text Transformations** (read-only views):
- **TRIM** - Remove leading/trailing spaces
- **UPPER/LOWER/PROPER** - Case conversion
- **LEFT/RIGHT/SUBSTRING** - Extract text portions
- **SPLIT** - Parse delimited text (Text to Columns)
- **CONCAT** - Combine multiple columns
- **REPLACE** - Find and replace patterns
- **REGEXP_EXTRACT** - Pattern extraction (emails, phones)

**Technical Implementation**:
```sql
-- Text cleaning examples
SELECT
  TRIM(name) as cleaned_name,
  UPPER(region) as region_upper,
  SUBSTRING(product_code, 1, 3) as category,
  STRING_SPLIT(full_name, ' ')[1] as first_name,
  REGEXP_EXTRACT(email, '@(.+)$') as email_domain
FROM data
```

**User Experience**:
- Preview transformations before applying
- Apply to create virtual view (no data modification)
- Chain multiple transformations
- Common patterns library (extract area code, parse addresses)

---

### 12. Advanced Statistics & Correlation

**Why**: Professional analysts need statistical measures beyond simple averages

**Requirements**:

**Statistical Functions**:
- Correlation matrix (already implemented!)
- Percentiles (25th, 50th, 75th, 90th, 95th, 99th)
- Standard deviation and variance
- Outlier detection (IQR method, Z-score)
- Distribution analysis (histogram, box plot)
- Moving averages (SMA, EMA)

**Technical Implementation**:
```sql
-- Advanced stats
SELECT
  PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY value) as q1,
  PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY value) as median,
  PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY value) as q3,
  STDDEV(value) as std_dev,
  VARIANCE(value) as variance
FROM data
```

**Correlation Matrix**:
- Already have this feature!
- Enhance with heatmap visualization
- Click cell to see scatter plot

---

## TIER 4: Power User Features

### 13. Saved Views & Templates

**Requirements**:
- Save current state (filters, pivots, charts) as named view
- Quick access to frequently used analyses
- Share view configurations with team
- Import/export view definitions

### 14. Data Quality Dashboard

**Requirements**:
- Automatic data profiling on import
- Show null percentages per column
- Identify duplicate rows
- Detect outliers
- Inconsistent formatting warnings
- Suggest data type corrections

### 15. Export & Reporting

**Requirements**:
- Export filtered view to CSV/Excel
- Export pivot table results
- Export charts as PNG/SVG
- Generate PDF report with multiple charts
- Scheduled exports (if running as server)

### 16. Smart Suggestions

**Requirements**:
- Auto-suggest useful pivots based on data
- Recommend charts based on selected columns
- Identify interesting correlations
- Suggest groupings for high-cardinality columns
- Detect time series and suggest trend analysis

---

## Non-Goals (Out of Scope)

These Excel features don't fit our read-only analysis model:

❌ **Data Validation** - We're not editing data
❌ **Flash Fill** - Pattern-based data entry
❌ **Power Query ETL** - We import once, analyze many times
❌ **Cell Editing** - Read-only analysis only
❌ **Formulas in Grid** - Use calculated fields instead
❌ **Macros/VBA** - Not applicable
❌ **Power Pivot** - DuckDB is already our engine

---

## Implementation Priorities

**Phase 1 (MVP)** - 2-3 months:
1. Pivot Table Builder (visual, drag-and-drop)
2. Multi-criteria Filtering (visual filter builder)
3. Conditional Formatting (color scales, data bars)
4. Statistical Summary Panel
5. Basic Charts (column, line, scatter)

**Phase 2** - Next 3 months:
6. Join Operations (multi-table analysis)
7. Interactive Slicers
8. Date Grouping & Time Intelligence
9. Calculated Fields Builder
10. Enhanced Charts (combo, heat maps)

**Phase 3** - Future:
11. Sparklines
12. Text Functions
13. Advanced Statistics
14. Saved Views
15. Data Quality Dashboard
16. Smart Suggestions

---

## Success Metrics

**Usability**:
- User can create pivot table in <30 seconds
- User can apply multi-criteria filter in <15 seconds
- User can create chart in <20 seconds
- Zero SQL knowledge required for 80% of tasks

**Performance**:
- Pivot table updates in <1 second for 1M rows
- Filtering response time <500ms
- Chart rendering <1 second
- Statistics calculation <2 seconds

**Adoption**:
- Users prefer RATS over Excel export for analysis
- 80% of analyses done without exporting to Excel
- Users create reusable views and dashboards
- Positive feedback on intuitiveness

---

## Technical Architecture

**UI Layer**: Kotlin Compose Desktop
**Query Engine**: DuckDB (OLAP-optimized)
**Visualization**: Chart library (Compose or JFreeChart)
**State Management**: Compose mutableStateOf
**Performance**: Materialized views for large pivots

**Key Principles**:
- All operations are queries, not modifications
- DuckDB handles all aggregations and transformations
- UI is reactive - updates instantly
- Progressive disclosure - start simple, reveal advanced features
- Always show SQL (optional "Show Query" button for learning)
