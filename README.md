# SpendWise 💰

> A modern, privacy-focused native Android daily expense tracking app built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Room Database**.

SpendWise is 100% offline with **zero network permissions**, keeping all your financial records private on your device while providing interactive data visualizations, local JSON backups via Android's Storage Access Framework (SAF), and spreadsheet exports (CSV & Excel `.xlsx`).

---

## ✨ Features

### 1. 📝 Daily Expense Tracking & Validation
- **Rich Entry Details**: Track amount (with customizable currency symbol), category, date & time (defaults to current moment), title with quick-suggestion chips, payment method (`Cash`, `UPI`, `Card`), and optional remarks.
- **Instant Validation**: Strict input validation prevents zero or negative amounts and empty titles.
- **Grouped Transaction Feed**: Displays expenses grouped chronologically by day (`Today`, `Yesterday`, or formatted date) with sticky headers showing daily subtotals.
- **Swipe-to-Delete with Undo**: Swipe left on any expense to delete it with an immediate confirmation dialog and an interactive 4-second **UNDO** `Snackbar`.
- **Tap to Edit**: Tap any transaction to view and update its details in a bottom sheet.

### 2. 🎛️ Interactive Budget Controller & Daily Pace Simulator
- **Multi-Point Access**: Open the budget controller directly from the Home header card, Analysis screen, or Settings.
- **Dual Scope Support**: Set a default monthly budget or customize targets on a per-month basis.
- **Tuning Controls**:
  - Quick Steppers: `-$5,000`, `-$1,000`, `+$1,000`, `+$5,000`.
  - Percentage Scaling: `-20%`, `-10%`, `+10%`, `+20%`.
  - Fluid Interactive Slider: Smoothly drag between spending limits to observe real-time budget impacts.
  - Preset Benchmarks: Quick shortcuts (`₹15k`, `₹25k`, `₹35k`, `₹50k`, `₹75k`, `₹100k`).
- **Safe Daily Pace Calculator**: Automatically calculates your remaining days in the month and reveals how much you can safely spend per day (`Remaining / DaysLeft`) to stay within budget.
- **Category-Level Budget Limits**: Allocate specific budget targets per category (e.g., Food: ₹8,000, Shopping: ₹4,000) with visual progress bars.

### 3. 📊 Monthly Analytics & Data Visualization
- **Month-over-Month Navigation**: Browse previous and future months with `←` and `→` controls.
- **Spending Metrics**: Total spent, percentage change (`%`) vs. the previous month (with dynamic indicator badges), daily average spend, and peak spend day.
- **Category Donut Chart**: Compose Canvas donut chart with animated sweep angles, central total display, slice tap selection, and legend breakdown.
- **Daily Spending Bar Chart**: Visualizes day-by-day expenditure across the month with weekend indicators and peak day highlight.
- **6-Month Trend Line Chart**: Smooth cubic Bézier curve displaying month-over-month trajectory with gradient underfill.
- **Top Categories Breakdown**: Ranked list with proportional progress indicators and transaction counts.

### 4. 🏷️ Category Management
- **Predefined Categories**: Seeded defaults (*Food*, *Transport*, *Rent*, *Bills*, *Shopping*, *Health*, *Entertainment*, *Education*, and *Other*), each with distinct icons and colors.
- **Custom Categories**: Create, edit, and color-code custom categories from a curated icon set and color palette.
- **Safe Reassignment**: Deleting any category automatically reassigns all past and future expenses to *"Other"*, keeping historical balances intact.

### 5. 💾 Storage Access Framework (SAF) Backup & Restore
- **Privacy-First**: No storage permissions required. Uses Android's Storage Access Framework (`CreateDocument` / `OpenDocument`).
- **Versioned JSON Backup**: Exports complete expenses, categories, and settings to a structured, versioned JSON file (`spendwise_backup_*.json`).
- **Inspection Preview**: Validates schema and displays record counts, date span, and total amounts before restoring.
- **Flexible Restore Modes**: Choose between **"Replace All Data"** (clean restore) or **"Merge (Skip Duplicates)"** (preserves existing records).
- **30-Day Backup Overdue Alert**: Automatically flags an alert if no backup has occurred within 30 days.

### 6. 📑 Spreadsheet Reports (CSV & Native Excel .xlsx)
- **Flexible Scope**: Export for the *Current Month*, a *Custom Date Range*, or *All-Time Data*.
- **RFC 4180-Compliant CSV**: Escapes commas, quotes, and line breaks properly and prepends a `UTF-8 BOM` (`\uFEFF`) so Microsoft Excel and Google Sheets open special characters without encoding errors.
- **Native Dual-Sheet Excel (`.xlsx`)**: Zero-dependency OpenXML ZIP generator producing genuine Excel workbooks:
  - *Sheet 1 (All Expenses)*: Formatted transactions table with auto-fit columns and currency formatting.
  - *Sheet 2 (Category Summary)*: Aggregated breakdown with totals, counts, and percentages.
- **Direct Save & System Share**: Save files directly via SAF or share immediately using Android's native share sheet (`FileProvider`).

### 7. 🎨 Emerald & Mint Wealth Theme
- Built strictly around Material Design 3 (M3).
- Supports dynamic System Theme, Forced Light, and Forced Dark modes.
- Emerald and Mint palette (`#0F5132`, `#10B981`, `#047857`, `#34D399`, `#064E3B`, `#D1FAE5`) designed specifically for financial clarity.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (Material 3, Material You tokens)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Local Database**: Android Room 2.7.0 (SQLite) via Kotlin Symbol Processing (KSP)
- **Reactive Streams**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Settings Store**: Jetpack DataStore Preferences
- **Visuals**: Bespoke Jetpack Compose Canvas charts (Donut, Bar, Line)
- **Excel Generator**: OpenXML SpreadsheetML ZIP generator (`java.util.zip.ZipOutputStream`)
- **Testing**: JUnit 4 + Robolectric local JVM testing

---

## 📁 Project Structure

```
app/src/main/java/com/example/
├── SpendWiseApplication.kt          # Application entrypoint & AppContainer DI
├── MainActivity.kt                  # Edge-to-edge container & 4-tab NavigationBar
├── data/
│   ├── model/
│   │   ├── CategoryEntity.kt        # Room entity for categories
│   │   ├── ExpenseEntity.kt         # Room entity for transactions
│   │   ├── ExpenseWithCategory.kt   # Composite Room relation model
│   │   ├── PaymentMethod.kt         # Cash, UPI, Card enum
│   │   └── AnalysisModels.kt        # Aggregation models (MonthSummary, DailySpending, etc.)
│   ├── dao/
│   │   ├── CategoryDao.kt           # Category queries and reassignment logic
│   │   └── ExpenseDao.kt            # Transaction queries and date range filters
│   ├── database/
│   │   └── SpendWiseDatabase.kt     # Room database definition
│   ├── preferences/
│   │   └── UserPreferencesRepository.kt # DataStore for currency, budget, and theme
│   ├── seeder/
│   │   └── SampleDataSeeder.kt      # Predefined categories & realistic demo seeding
│   ├── repository/
│   │   └── ExpenseRepository.kt     # Central repository for metrics and data operations
│   ├── backup/
│   │   └── BackupRestoreManager.kt  # JSON serialization, schema validation, and restore
│   └── export/
│       ├── ReportExporter.kt        # CSV exporter with BOM & FileProvider sharing
│       └── XlsxGenerator.kt         # Lightweight OpenXML .xlsx generator
└── ui/
    ├── common/
    │   └── CategoryVisuals.kt       # Material symbols mapping & category badge composables
    ├── charts/
    │   └── Charts.kt                # Canvas DonutChart, DailyBarChart, and TrendLineChart
    ├── components/
    │   ├── AddExpenseBottomSheet.kt # Expense entry modal with live validation
    │   └── BudgetManipulationSheet.kt # Interactive budget controller & daily pace simulator
    ├── screens/
    │   ├── HomeScreen.kt            # Summary card, daily grouped transactions, swipe-to-delete
    │   ├── AnalysisScreen.kt        # Month picker, charts, metrics, and budget progress
    │   ├── CategoriesScreen.kt      # Category manager with add/edit/delete dialogs
    │   └── SettingsScreen.kt        # Currency, budget, SAF backup/restore, export reports
    ├── theme/
    │   ├── Color.kt                 # Emerald & Mint color tokens
    │   ├── Theme.kt                 # Material 3 light/dark color schemes
    │   └── Type.kt                  # Typography definitions
    └── viewmodel/
        ├── ExpenseViewModel.kt      # Transactions, home summary, and undo state
        ├── AnalysisViewModel.kt     # Reactive monthly summary and navigation
        └── SettingsViewModel.kt     # Preferences, backup streams, and report generation
```

---

## 🧪 Testing

The test suite covers data persistence, monthly aggregation math, backup serialization round-trips, and CSV RFC 4180 escaping:

```bash
# Run all unit and Robolectric tests via Gradle
gradle :app:testDebugUnitTest
```

### Key Test Suites:
- `BudgetManipulationTest`: Tests stepper bounds, percentage scaling, and daily safe allowance calculations.
- `CsvEscapingTest`: Validates RFC 4180 quoting, double-quote escaping, multiline notes, and UTF-8 BOM prepending.
- `BackupSerializationTest`: Validates JSON schema compliance, preview metadata extraction, and corrupt file detection.
- `AggregationTest`: Validates month-over-month percentage changes, daily averages, and chronological grouping.
- `ExampleRobolectricTest`: Verifies resource resolution and application context configuration.

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Ladybug / Jellyfish or newer recommended).
- **Android SDK** API 26 (Android 8.0) or higher (recommended API 34+).
- **JDK 17 or JDK 21**.

### Running the App
1. Clone or open this repository in **Android Studio**.
2. Allow Gradle to sync dependencies.
3. Connect an Android device or start an Android Emulator.
4. Click **Run** (`Shift + F10`) to build and install SpendWise.

---

## 🔒 Privacy & Permissions

SpendWise requires **NO runtime permissions** and **NO network permissions**:
- `<uses-permission android:name="android.permission.INTERNET" />` is **NOT** declared.
- File exports and backups use the Android **Storage Access Framework** and **FileProvider**, ensuring zero access to private external storage.
