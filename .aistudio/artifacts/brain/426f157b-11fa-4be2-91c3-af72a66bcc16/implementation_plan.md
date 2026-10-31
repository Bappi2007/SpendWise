# SpendWise - Native Expense Tracker

A native, privacy-focused Android application for daily expense tracking built with Kotlin, Jetpack Compose, Material 3, and Room. It delivers an intuitive offline-first experience with rich data visualizations, custom category management, automated budget tracking, SAF-based JSON backups, and multi-format reporting.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions were confirmed during initial clarification and are locked into the system design:
> - **Initial Data State**: Realistic demo transactions across multiple months and categories will be pre-seeded on first install so that charts, summaries, and monthly analytics are immediately interactive.
> - **Default Currency**: Indian Rupee (`₹`) by default, customizable in Settings to USD (`$`), EUR (`€`), GBP (`£`), JPY (`¥`), or any custom currency symbol.
> - **Visual Identity**: Emerald & Mint wealth palette (`#0F5132` / `#00A86B` / `#10B981` / `#D1FAE5`) with dynamic dark/light surface gradients, modern curved cards, and distinct category tinting.

---

## 1. Overview & Core Concept

- **What It Does**: SpendWise allows users to log daily expenses in seconds, organize them into customizable categories, observe month-over-month trends, set spending budgets, back up their financial data locally via Android's Storage Access Framework, and export detailed CSV / Excel reports.
- **Target Audience / Persona**: Individuals, freelancers, and household budget managers who want an aesthetic, lightning-fast expense tracker without account registration, ads, tracking, or network permissions.
- **Key Value**: 100% offline security, zero cloud dependency, smooth 60fps Compose animations, and comprehensive financial awareness through visual analytics.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Daily Logging Flow**:
   - The user opens the app to the **Home** screen displaying today's summary card (Total Spent Today, Month-to-Date, and Budget Remaining).
   - Taps the floating action button (`+`) or quick-action chip.
   - A modal bottom sheet slides up with an amount input field (with currency prefix), category selector pills with icons and color accents, date/time pickers (defaulting to current moment), title, remarks, and payment method chips (Cash, UPI, Card).
   - Instant validation: clicking Save inserts into Room with a haptic ripple and smooth list insertion animation.

2. **Transaction Management Flow**:
   - Expenses are displayed in a `LazyColumn` grouped by date (Today, Yesterday, Specific Date) with sticky day headers and daily spend subtotals.
   - Tapping an item opens an edit sheet with pre-populated fields.
   - Swiping an item left triggers a swipe-to-delete action accompanied by an undo `Snackbar` that restores the record if tapped within 4 seconds.

3. **Monthly Analysis Flow**:
   - Users navigate to **Analysis** via the bottom bar.
   - A month-switcher bar (← October 2026 →) lets users browse historical months.
   - Metric pills show Total Spent, % Change vs previous month, Daily Average, and Peak Spend Day.
   - Interactive charts:
     - **Category Donut Chart**: Arc slices for each category with tap-to-inspect percentages, central total display, and detailed legend breakdown.
     - **Daily Spend Bar Chart**: Vertical bars representing each day in the month, highlighting weekends and the highest spend peak.
     - **6-Month Trend Line Chart**: Smooth cubic Bézier curve displaying month-by-month trajectory with gradient underfill.
     - **Budget Progress Indicator**: Visual progress bar indicating percentage of budget consumed, switching to warning amber at 80% and crimson over 100%.

4. **Category Customization Flow**:
   - Default seeded categories: Food, Transport, Rent, Bills, Shopping, Health, Entertainment, Education, Other.
   - Users can create new custom categories choosing from a curated icon set and color palette.
   - Deleting a category prompts a safe migration dialog: all associated expenses are automatically reassigned to "Other" before removal.

5. **Local Backup & Restore Flow**:
   - **Backup**: Exports complete expenses, categories, and preferences into a structured, versioned JSON payload (`spendwise_backup_YYYYMMDD.json`) using the Storage Access Framework (SAF `CreateDocument`).
   - **Restore**: Users pick a JSON file via SAF `OpenDocument`. The app validates schema and version, presents an inspection sheet (total records found, date span, category count), and lets the user choose between **"Replace All"** or **"Merge (Skip Duplicates)"**.
   - Displays last backup timestamp with an overdue badge if older than 30 days.

6. **Report Export Flow**:
   - Users choose scope (Current Month, Custom Date Range, or All-Time Data) and format (CSV or Excel `.xlsx`).
   - Generates RFC 4180-compliant CSV with UTF-8 BOM, or native dual-sheet OpenXML `.xlsx` (Sheet 1: Detailed Transactions; Sheet 2: Category Breakdown & Totals) with auto-sized columns and formatted headers.
   - Files can be saved directly via SAF or shared via Android's native share sheet.

### Visual Identity & Theme Tokens

- **Aesthetic Direction**: Fresh, trustworthy Emerald & Mint fintech aesthetic.
- **Color Tokens**:
  - `Primary`: Deep Emerald (`#0A5C36` light / `#4ADE80` dark)
  - `Secondary / Accent`: Mint Green (`#10B981` light / `#2DD4BF` dark)
  - `Tertiary`: Soft Sage & Gold Highlights (`#F59E0B` for warnings, `#EF4444` for overspend)
  - `Background`: Pristine off-white (`#F8FAFC` light / `#0B1311` dark surface)
  - `Surface & Card`: High-tonal elevation containers with subtle border strokes (`#E2E8F0` / `#1E2923`)
- **Typography & Rhythm**:
  - Prominent tabular numerical typography for amounts.
  - Consistent 16.dp horizontal screen gutters and 12.dp card padding.
  - Touch targets strictly enforcing minimum 48.dp interactive dimensions.

---

## 3. Key Product Decisions & Trade-Offs

- **Storage Strategy**:
  - *Chosen Approach*: Android Room 2.7.0 SQLite database with Kotlin Coroutines and reactive `Flow` queries, combined with Jetpack DataStore Preferences for user settings (currency, theme, budget, last backup time).
  - *Why*: Provides ACID guarantees, ultra-fast indexed grouping queries for dates and categories, zero background battery consumption, and complete offline privacy (no INTERNET permission in `AndroidManifest.xml`).
- **Dependency Injection**:
  - *Chosen Approach*: Lightweight, robust Application-level `AppContainer` and `ViewModelProvider.Factory`.
  - *Why*: Guarantees rock-solid build stability with Kotlin 2.2.10 and AGP 9.1.1, avoids KSP compiler plugin conflicts associated with Dagger-Hilt setups in isolated sandbox containers, while ensuring 100% testability with constructor-injected repositories and fakes.
- **Chart Rendering Architecture**:
  - *Chosen Approach*: Native Jetpack Compose Canvas implementations for Donut, Bar, and Line charts with smooth animated state transitions (`animateFloatAsState`).
  - *Why*: Eliminates heavy external chart library version mismatches, provides complete control over touch highlight interactions, dynamically supports dark/light themes, and guarantees zero layout jank.
- **Native Excel (.xlsx) Generation**:
  - *Chosen Approach*: Streamlined OpenXML ZIP generator utilizing Java's standard `ZipOutputStream` to package workbook XML and sheet XML structures.
  - *Why*: Completely sidesteps massive libraries like Apache POI (which cause 40MB+ APK bloat and dex limit overhead on Android), generating compliant `.xlsx` files readable by Microsoft Excel, Google Sheets, and LibreOffice.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Jetpack Compose UI                            │
│  ┌───────────────┐ ┌────────────────┐ ┌───────────────┐ ┌────────────┐ │
│  │  HomeScreen   │ │ AnalysisScreen │ │CategoriesView │ │SettingsView│ │
│  │ (Daily List)  │ │(Donut/Bar/Line)│ │ (Manage/Edit) │ │ (Backup/Exp│ │
│  └───────┬───────┘ └───────┬────────┘ └───────┬───────┘ └──────┬─────┘ │
└──────────┼─────────────────┼──────────────────┼────────────────┼───────┘
           │                 │                  │                │
┌──────────▼─────────────────▼──────────────────▼────────────────▼───────┐
│                              ViewModels                                │
│      ExpenseViewModel      AnalysisViewModel      SettingsViewModel    │
│            │                       │                      │            │
│            └───────────────┬───────┴──────────────────────┘            │
└────────────────────────────┼───────────────────────────────────────────┘
                             ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           Repository Layer                             │
│       ExpenseRepository                    CategoryRepository          │
│                │                                    │                  │
│                └─────────────────┬──────────────────┘                  │
└──────────────────────────────────┼─────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼─────────────────────────────────────┐
│                 Data Persistence & Services Engine                     │
│  ┌───────────────────────┐ ┌─────────────────┐ ┌────────────────────┐  │
│  │   Room AppDatabase    │ │Preferences Data-│ │  Backup & Report   │  │
│  │ ExpenseDao/CategoryDao│ │     Store       │ │  (Json/CSV/XLSX)   │  │
│  └───────────────────────┘ └─────────────────┘ └────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Entities & Schema

1. **`CategoryEntity`**:
   - `id: String` (UUID primary key)
   - `name: String` (Unique, e.g., "Food & Dining")
   - `iconName: String` (M3 icon identifier)
   - `colorHex: Long` (Color value)
   - `isDefault: Boolean`

2. **`ExpenseEntity`**:
   - `id: String` (UUID primary key)
   - `title: String`
   - `amount: Double`
   - `categoryId: String` (Foreign key referencing `CategoryEntity.id`)
   - `timestamp: Long` (Epoch millis for exact date & time)
   - `paymentMethod: String` ("CASH", "UPI", "CARD")
   - `notes: String?`

3. **`AppPreferences` (DataStore)**:
   - `currencySymbol: String` (Default "₹")
   - `themeMode: String` ("SYSTEM", "LIGHT", "DARK")
   - `monthlyBudget: Double`
   - `lastBackupTimestamp: Long`

### Test Suite Strategy

- **`AggregationTest`**: Validates month-over-month total computation, category grouping, percentage calculations, and daily averages.
- **`BackupSerializationTest`**: Tests full JSON export, JSON schema deserialization, and round-trip data integrity.
- **`CsvEscapingTest`**: Tests RFC 4180 compliance, special character escaping (commas, quotes, linebreaks), and UTF-8 BOM prepending.
- **`ExpenseViewModelTest`**: Unit testing state flows for adding, updating, and deleting expenses with undo functionality.
