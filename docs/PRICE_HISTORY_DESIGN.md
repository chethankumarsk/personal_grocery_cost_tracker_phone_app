# Price History — Design

Status: **implemented in `Unreleased` (1.4.0)** — see §13 for where the build differs from this design
Target: **1.4.0** (after recent-item chips, which are already in `Unreleased`)
Scope: read-only insight; no schema, CSV, or settings change

## 1. Problem

The app already knows the last price paid for every item (`item_catalog.last_price_paise`), and every purchase is stored in `expenses` with a normalized name. But it can only show *one* number: "last paid ₹64 on 12 Oct". It cannot answer the question a cost tracker exists to answer:

> Is this item getting more expensive, and by how much?

The data is already sufficient. `expenses.name_normalized` is indexed, amounts are integer paise, and dates are epoch days, so a history view needs **no migration and no CSV change**.

## 2. Goals / non-goals

**Goals**

- For a single item, show every recorded purchase with date and amount.
- Show the trend at a glance: latest price, previous price, absolute and percentage change, average, min/max.
- Work fully offline from Room; no new network use.
- Never invent or interpolate data; show only recorded purchases.

**Non-goals**

- No forecasting or inflation modelling beyond the recorded points.
- No per-store comparison, quantity/unit price, or receipt attachments (separate future items).
- No editing from inside the history view; tapping a row opens the existing editor.
- No changes to totals, the calendar, or the CSV format.
- No charts for categories or the month as a whole (category totals already exist).

## 3. User stories

1. As a user, I tap the history icon on a day-sheet row and see what I have paid for that item over time.
2. As a user, I can tell within a second whether the latest price went up or down versus the previous purchase.
3. As a user, I can open any historical purchase and correct it, and the history updates immediately.
4. As a user with only one purchase of an item, I get a clear "only one purchase so far" state rather than an empty chart.

## 4. Entry points

Three candidate surfaces; implement the first two, skip the third:

| Entry point | Behaviour | Decision |
| --- | --- | --- |
| Day-sheet row trailing icon | Opens history for that item's normalized name | **Yes** — discoverable, no change to the tap-to-edit contract (DAY-3) |
| Edit form (existing entry) | A "Price history" text button under the amount field | **Yes** — natural place when you are already looking at a price |
| Suggestion panel | Opening history from the autocomplete dropdown | **No** — the dropdown is for picking a name, and a nested sheet on top of the editor adds navigation complexity for little gain |

The icon must not steal the row's main tap target: the row keeps `clickable` → edit, and the icon is a separate 48 dp `IconButton` with `contentDescription = "Price history for <name>"`.

## 5. UX

A bottom sheet (or in-window sheet, matching the day-sheet pattern) titled with the item name.

```
┌──────────────────────────────────────────┐
│  Milk                              ✕     │
│                                          │
│  Last paid  ₹64        ▲ +₹4 (+6.7%)     │
│  on 12 Oct 2026        vs ₹60 on 2 Oct   │
│                                          │
│  Average ₹61 · Low ₹58 · High ₹64 · 4×   │
│                                          │
│   ₹                                      │
│   64 ┤           ●                       │
│   62 ┤     ●                             │
│   60 ┤  ●        ●                       │
│      └──┬────┬────┬────┬──               │
│        Sep  Sep  Oct  Oct                │
│                                          │
│  12 Oct 2026                      ₹64    │
│  2 Oct 2026                       ₹60    │
│  18 Sep 2026                      ₹62    │
│  4 Sep 2026                       ₹58    │
└──────────────────────────────────────────┘
```

Rules:

- **Trend colour is never the only signal.** Show the arrow glyph (▲/▼/–) and the signed text next to any colour, matching `CAL-9`'s accessibility rule.
- Amounts always via `Money.format`; dates via `DateRanges.formatDay` plus year when the purchase is not in the current year.
- The history list is the primary content; the chart is decorative (`contentDescription = null`, with the stats carrying the meaning for TalkBack).
- List rows are not buttons unless they open the editor; if they do, they use the same 48 dp target and an accessibility label of "Edit <name> on <date>, ₹<amount>".
- Scroll: entire sheet content scrolls; the header (name + last price) stays pinned only if the sheet exceeds the screen height.

States:

| State | Rendering |
| --- | --- |
| Loading | Sheet skeleton/spinner, same size as loaded (no layout jump) |
| No purchases | "No purchases recorded for this item." (only reachable by deleting the row that opened it) |
| One purchase | Header + "Only one purchase so far" + that single row; chart hidden |
| Two or more | Header + stats + chart + list |
| Very long history | Last 200 points charted; list shows all (or last 500, see §7) |

## 6. Data

### 6.1 Query

Existing index `expenses(name_normalized)` covers it. New DAO method:

```kotlin
@Query(
    """
    SELECT * FROM expenses
    WHERE name_normalized = :normalizedName
    ORDER BY expense_date DESC, created_at DESC, id DESC
    LIMIT :limit
    """,
)
fun observePriceHistory(normalizedName: String, limit: Int): Flow<List<ExpenseEntity>>
```

Ordering matches the day sheet: newest purchase first, ties broken by insertion order. `LIMIT` is a guard rail, not pagination.

### 6.2 Domain model

```kotlin
data class PricePoint(
    val expenseId: Long,
    val date: LocalDate,
    val amountPaise: Long,
)

data class PriceHistory(
    val name: String,
    val points: List<PricePoint>,   // newest first, as queried
    val stats: PriceStats,
)

data class PriceStats(
    val count: Int,
    val averagePaise: Long,
    val minPaise: Long,
    val maxPaise: Long,
    val latestPaise: Long,
    val previousPaise: Long?,       // null when count < 2
    val changePaise: Long?,         // latest - previous
    val changePercent: Long?,       // rounded to 1 dp when formatted
)
```

`PriceStats` is computed by a **pure Kotlin** `PriceHistoryCalculator` in `domain/history/`, so it is unit-testable with no Android or Room dependency (MNT-3).

### 6.3 Rounding

- `averagePaise` is an integer division of the total; the remainder is dropped. The UI shows `Money.format(averagePaise)`.
- `changePercent` is computed as `(change * 1000L) / previous` in tenths of a percent to avoid floats, then formatted as `+6.7%`. If `previous == 0` (not possible under the ₹1 minimum, but defensive), percent is `null` and only the absolute change is shown.

### 6.4 Repository

```kotlin
fun observePriceHistory(name: String, limit: Int = 500): Flow<PriceHistory?>
```

- Normalizes with `ItemNames.normalize` (the same function used on insert), so casing/whitespace variants resolve to one history.
- Returns `null`/empty when there is no match; the UI treats it as "no purchases".
- Reuses `expenses` rows; no new table, no catalog dependency.
- Deleting or editing an entry re-emits automatically (Room `Flow`), so the sheet stays live.

## 7. Performance

- Only one item's rows are read; typical histories are 3–40 rows.
- The `LIMIT 500` cap bounds pathological cases; if the cap is hit, the list header shows "Showing the latest 500 purchases".
- No totals are precomputed or stored (invariant 3).
- Chart draws at most 200 points; if more, sample evenly (keep first and last, take every *n*th) so the shape is preserved without a scrolling canvas.

## 8. UI state and navigation

- `MainViewModel` gains `priceHistory: StateFlow<PriceHistoryUiState?>` and `openPriceHistory(expense)` / `closePriceHistory()`.
- The open item is kept in `SavedStateHandle` as the normalized name plus a display name, so rotation and process death restore the sheet.
- Opening history from the edit form closes the editor first (or stacks the sheet above it — decide during implementation; stacking is simpler if both are full-screen, but the editor's discard logic must not fire).
- Tapping a row in history calls the existing `openEdit(expense)` with `returnToDaySheet` semantics preserved; history closes on edit.

## 9. Edge cases

| Case | Behaviour |
| --- | --- |
| Item renamed in an entry | History is per normalized name. Renaming one purchase splits it off; this matches how autocomplete treats names today. Document it in the empty/short states. |
| Same item bought twice on one day | Two separate points on the same date; chart offsets them horizontally by index, not by date. |
| Category changed | No effect; history ignores category. |
| All purchases deleted while open | Flow emits empty → show "No purchases recorded"; close on back. |
| Entry date edited | Point moves in the ordering automatically. |
| Price with paise (e.g. ₹64.50) | Shown exactly; chart axis uses paise internally. |
| 200% font | Header stacks vertically; stats wrap; chart keeps a minimum 120 dp height. |
| RTL / long names | Name ellipsizes after two lines; sheet title max two lines. |

## 10. Tests

**Unit (pure Kotlin) — `PriceHistoryCalculatorTest`**

- empty list → count 0, no stats
- single point → previous/change/percent null
- increasing and decreasing sequences → correct sign and magnitude
- average rounding (e.g. 60, 61, 61 → 60.66 → 6066)
- percent in tenths (60 → 64 = +6.7%)
- previous of 0 guard
- min/max with duplicates

**Repository (Robolectric + in-memory Room) — `OfflineExpenseRepositoryTest`**

- history returns newest first and ignores other items
- case/whitespace variants share one history
- editing a name moves the point to the new history
- deleting the last entry empties the history
- limit is respected

**Compose (Robolectric) — `PriceHistorySheetTest`**

- opening from a day-sheet row shows stats and rows
- single-purchase state renders the fallback message and no chart
- trend text includes both arrow and signed amount (accessibility, not colour-only)
- tapping a row opens the editor

**Manual**

- TalkBack reads the header, stats, and each row without announcing decorative chart content
- 200% font and small screens (360×640) — no clipped stats or overlapping chart
- 50+ purchases for one item scroll smoothly

## 11. Rollout

1. Ship as **1.4.0** together with the already-merged recent-item chips (or 1.5.0 if chips ship first).
2. No migration: verify the Room schema hash is unchanged.
3. No CSV change: verify export bytes are identical to 1.3.2 for the same data.
4. Update `REQUIREMENTS.md` with PRICE-1..PRICE-6 and add the status row, `CHANGELOG.md`, this doc, and the knowledge graph (`feature.price-history` replacing any future node).
5. Screenshot the sheet on the emulator for the release notes.

## 12. Open questions

1. **Sheet vs. screen.** A sheet matches the day sheet and keeps context; a full screen gives the chart more room. Recommendation: sheet with a scrollable body, revisit if the chart feels cramped at 200% font.
2. **Should the day-sheet icon be always visible or revealed on long-press?** Always visible costs horizontal space on small screens; an overflow menu is another tap. Recommendation: icon, because the row already truncates long names.
3. **Chart type.** Line chart with dots is easiest to read for sparse data; bars imply discrete comparisons. Recommendation: line + dots, one dot per purchase, x-axis by purchase order with date labels only at the first, last, and midpoint.
4. **Include category breakdown per item?** No — categories are already aggregated monthly; it would clutter a per-item view.
5. **Do we show the last 12 months only by default?** Recommendation: show everything, with a "Last 12 months / All" toggle only if users ask. Defaulting to a window silently hides data, which is worse than a long list.

## 13. Implementation notes (as built)

The shipped version follows the data, stats and test sections above. These points differ from the proposal:

- **Full-screen surface, not a bottom sheet.** `PriceHistorySheet` renders in the activity window like the entry editor. Stacking another `ModalBottomSheet` over the day sheet (itself a dialog-window sheet) was fragile on device, and this keeps the sheet above both the calendar and the editor.
- **Direction is written, not colour-only.** The header reads "Up ₹5 (8.3%) vs ₹60 on 2 Nov 2026" or "Down …"/"No change …", coloured error/primary/neutral. Words survive TalkBack and greyscale better than arrow glyphs.
- **Entry points:** the day-sheet row icon (`ic_price_history`) and a **Price history** button in the edit form, shown only while the form has no unsaved changes. Tapping a history row replaces a pristine editor with the selected entry.
- **Navigation:** opening history closes the day sheet and remembers to reopen it on close; opening it from the editor leaves the editor in the ViewModel so closing history returns to it. The open item is restored from `SavedStateHandle` after rotation.
- **Repository contract:** `observePriceHistory(name, limit)` always returns a `PriceHistory` (empty `points` for an unknown item) rather than null; the UI shows the "no purchases" state, and the sheet shows a short loading state until the first Room emission.
- **Dates in the list always include the year** (`DateRanges.formatDayWithYear`), so a long history is unambiguous without needing today's date.
- **Chart:** sampled to 200 points, drawn with Compose `Canvas`; no axes, labels or semantics. The stats grid carries the information for screen readers.
- **Tests:** 9 calculator tests, 4 repository tests, 3 Compose tests (open from day sheet, tap-to-edit, single purchase). No migration or CSV test changes because neither format changed.
