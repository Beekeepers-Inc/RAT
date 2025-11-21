# Pivot Table - Design Decisions Record

## ✅ Confirmed Decisions (From Review Session)

### 1. Column Limit Strategy
**Decision**: Allow >100 columns with warning
- Show warning at 100 columns: "This pivot has 250 columns. Large pivots may be slow. Continue?"
- Max recommended: 100 columns
- Max allowed: 500 columns (hard technical limit)
- **Rationale**: Financial datasets can be wide when denormalized

### 2. Date Grouping UI
**Decision**: Auto-group to Year by default (Option C)
- Automatically detect date fields and group by Year
- Display as: `📅 Year(order_date)` with dropdown icon
- Click dropdown to change grouping: Quarter, Month, Week, Day
- **Rationale**: Best balance of immediate usability and flexibility

### 3. Preview Row Limit
**Decision**: 200 rows in preview mode
- Shows top 200 rows with LIMIT 200
- Full results available via "View Full Table" button
- Always show total row count: "Showing 200 of 1,234,567 rows"

### 4. Hierarchical Row Grouping
**Decision**: Flat display for MVP
- Show all rows with proper indentation/grouping
- Display subtotals inline
- Expand/collapse feature deferred to Phase 2
- **Rationale**: Simpler implementation, covers 80% of use cases

### 5. Filter UI Complexity
**Decision**: Simple checkbox filters for MVP
- Multi-select checkboxes for categorical data
- All filters use AND logic
- Advanced filters (number ranges, date pickers, OR logic) deferred to Phase 2
- **Rationale**: Covers most common filtering needs with minimal complexity

---

## ✅ All Decisions Confirmed

### 6. Number Formatting Strategy
**Decision**: Auto-detect from field name + user can override
- Fields containing "sales", "revenue", "price", "amount", "total" → Currency ($1,234.56)
- Fields containing "rate", "percent", "pct", "ratio" → Percentage (45.6%)
- Fields containing "count", "quantity", "qty", "num" → Whole numbers (1,234)
- Default → 2 decimal places (1,234.56)
- Right-click on value field allows format override
- **Rationale**: Smart defaults reduce manual work, override provides flexibility

### 7. Grand Totals Position
**Decision**: Sticky totals (always visible)
- Grand total row stays visible at bottom while scrolling data
- Implemented with fixed footer in grid component
- **Rationale**: Better UX - users always see totals without scrolling

### 8. NULL Value Handling
**Decision**: Show as "(null)"
- NULL values display as "(null)" in pivot results
- Treated as distinct group in aggregations
- **Rationale**: Clear indication that data is missing vs empty string

### 9. Performance & Progress Indicators
**Decision**: 60 second max wait with progressive UI
- 0-3s: Simple spinner
- 3-10s: Spinner + "Generating pivot..."
- 10-30s: Spinner + "This is taking longer than expected..."
- 30-60s: Show cancel button
- >60s: Timeout with error message
- **Rationale**: 60s accommodates complex financial datasets while providing escape hatch

### 10. Subtotals Display
**Decision**: Always show subtotals
- Automatically show subtotals for multi-level row grouping
- Visual indentation to show hierarchy
- **Rationale**: Provides immediate insights into grouped data

---

## ⏳ Pending Decisions (Need Your Input)

---

## 📋 Decision Summary

All critical design decisions finalized. Ready for implementation.

| # | Decision Area | Choice | Rationale |
|---|---------------|--------|-----------|
| 1 | Column Limit | >100 allowed, warn at 100, max 500 | Financial datasets can be wide |
| 2 | Date Grouping | Auto-group to Year, dropdown to change | Balance of usability and flexibility |
| 3 | Preview Rows | 200 rows | Good sample size without performance hit |
| 4 | Hierarchical Rows | Flat display for MVP | Simpler implementation, 80% coverage |
| 5 | Filter UI | Simple checkboxes | Covers common cases, minimal complexity |
| 6 | Number Formatting | Auto-detect + user override | Smart defaults, manual control available |
| 7 | Grand Totals | Sticky (always visible) | Better UX, no scrolling to see totals |
| 8 | NULL Handling | Show as "(null)" | Clear indication of missing data |
| 9 | Performance | 60s max, progressive warnings | Accommodates complex pivots, provides escape |
| 10 | Subtotals | Always show | Immediate insights into grouped data |

---

## ARCHIVED: Original Decision Questions

### 6. Number Formatting Strategy

**Question**: How should we format numbers in pivot results?

**Options**:
- **A) Auto-detect from field name**
  - Fields containing "sales", "revenue", "price" → Currency ($1,234.56)
  - Fields containing "rate", "percent", "pct" → Percentage (45.6%)
  - Fields containing "count", "quantity" → Whole numbers (1,234)
  - Default → 2 decimal places (1,234.56)

- **B) Always show raw numbers**
  - No formatting in preview
  - User can apply formatting in full view

- **C) User chooses format per value field**
  - Right-click on value field → "Format as Currency/Percentage/Number"
  - More control but requires extra clicks

- **D) Use DuckDB's default formatting**
  - Whatever DuckDB returns, we display as-is
  - Simplest but least user-friendly

**My recommendation**: **Option A** (auto-detect with smart defaults) + **Option C** (user can override)

**Your input needed**: Which option do you prefer?

---

### 7. Grand Totals Position

**Question**: Where should grand totals be displayed?

**Options**:
- **A) Bottom only** (Excel default)
  ```
  West   | $100 | $200 | $300
  East   | $150 | $250 | $400
  Total  | $250 | $450 | $700  ← Bottom
  ```
  Problem: User must scroll to see totals in large pivots

- **B) Sticky totals** (always visible)
  - Total row stays visible at bottom even when scrolling
  - Requires CSS/UI trickery

- **C) Top AND bottom**
  ```
  Total  | $250 | $450 | $700  ← Top
  West   | $100 | $200 | $300
  East   | $150 | $250 | $400
  Total  | $250 | $450 | $700  ← Bottom
  ```

- **D) Configurable**
  - Checkbox: "Show totals at top"
  - Checkbox: "Show totals at bottom"

**My recommendation**: **Option B** (sticky totals) for better UX

**Your input needed**: Which option do you prefer?

---

### 8. NULL Value Handling

**Question**: How should NULL values be displayed in pivot results?

**Scenario**:
```sql
-- Some rows have NULL region
SELECT region, SUM(sales)
FROM data
GROUP BY region
```

**Options**:
- **A) Show as "(Blank)" row**
  ```
  West     | $100
  East     | $200
  (Blank)  | $50   ← NULL region
  ```

- **B) Show as "(null)"**
  ```
  West   | $100
  East   | $200
  (null) | $50
  ```

- **C) Show as "Unspecified"**
  ```
  West        | $100
  East        | $200
  Unspecified | $50
  ```

- **D) Exclude NULL rows entirely**
  - Add `WHERE region IS NOT NULL` to query
  - User never sees NULL values

- **E) Configurable**
  - Default: Show as "(Blank)"
  - Checkbox: "Exclude blank values"

**My recommendation**: **Option A** (show as "(Blank)") with **Option E** (configurable exclude)

**Your input needed**: Which option do you prefer?

---

### 9. Performance & Progress Indicators

**Question**: How should we handle long-running pivots?

**Scenarios**:
- Simple pivot: <500ms → No indicator needed
- Moderate pivot: 500ms - 3s → Show spinner
- Complex pivot: 3s - 10s → Show progress bar?
- Very complex: >10s → ?

**Options**:
- **A) Spinner only**
  - Simple loading spinner
  - No time estimate
  - No cancel option

- **B) Spinner + time estimate**
  - "Generating pivot... (estimated 5 seconds)"
  - Based on row count heuristic

- **C) Progress bar**
  - Show percentage complete
  - Requires query to report progress (may not be possible with DuckDB)

- **D) Spinner + Cancel button**
  - User can cancel long-running query
  - Requires query cancellation support

- **E) Progressive timeout warnings**
  - 0-3s: Spinner only
  - 3-10s: Spinner + "This is taking longer than expected..."
  - >10s: Show cancel option

**My recommendation**: **Option E** (progressive warnings) - best UX without complex implementation

**Your input needed**:
- What's acceptable max wait time? 5s? 10s? 30s?
- Should we allow query cancellation?

---

### 10. Subtotals Display

**Question**: How should subtotals be shown in multi-level row grouping?

**Scenario**: Rows = [Region, Product]
```
West
  Widget A    | $100
  Widget B    | $200
  [West Total]| $300  ← Subtotal
East
  Widget A    | $150
  Widget B    | $250
  [East Total]| $400  ← Subtotal
[Grand Total] | $700  ← Grand total
```

**Options**:
- **A) Always show subtotals**
  - Automatic for any multi-level grouping
  - May be cluttered for many levels

- **B) Show only for 2+ levels**
  - Single level: No subtotals (just grand total)
  - Multiple levels: Show subtotals

- **C) Configurable**
  - Checkbox: "Show subtotals"
  - Default: ON

- **D) Smart detection**
  - If result set < 50 rows: Show subtotals
  - If result set > 50 rows: Hide subtotals (too cluttered)

**My recommendation**: **Option C** (configurable, default ON)

**Your input needed**: Should subtotals always show, or be optional?

---

## Quick Response Template

Just reply with the numbers and your choice:

```
6. Number formatting: A (auto-detect + user override)
7. Grand totals: B (sticky)
8. NULL handling: A (show as Blank)
9. Performance: E (progressive warnings), max wait 10s, yes to cancel
10. Subtotals: C (configurable, default ON)
```

Or let me know if you want to discuss any of these in detail first.

---

## Implementation Impact

These decisions will affect:
- **Task 1.2**: Query generation (NULL handling, subtotals)
- **Task 2.5**: Preview grid UI (number formatting, grand totals position)
- **Task 2.6**: State management (progress indicators)

**Once confirmed, I can finalize the design and start implementation.**
