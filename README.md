# SpendWise 💰

> A modern, privacy-first native Android personal finance & expense manager built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Room Database**.

SpendWise operates 100% offline with **zero network permissions**, keeping all financial transactions, fund inflows, and friend debt settlements securely on your device. It provides interactive data visualizations, group expense splitting, flexible budget controller with fund tracking, calendar-based data management, local JSON backups via Android's Storage Access Framework (SAF), and spreadsheet exports (CSV & Excel `.xlsx`).

---

## 👨‍💻 Author & Attribution

**Developed by Bishal Mandal**  
- **Email**: [bishalkumandal777@gmail.com](mailto:bishalkumandal777@gmail.com)  
- **Version**: SpendWise v1.0 (Offline-first Jetpack Compose Room Database)

---

## ✨ Key Features

### 1. 📝 Daily Expense Tracking & Grouped Feed
- **Rich Transaction Entry**: Log amount (with customizable currency symbol), category, timestamp (defaults to now, with custom date/time pickers), merchant/title with quick autocomplete chips, payment method (`Cash`, `UPI`, `Card`), and optional remarks.
- **Grouped Transaction Dashboard**: Chronologically organized daily groups (`Today`, `Yesterday`, or formatted date) displaying daily net spending and fund inflows side by side.
- **Swipe-to-Delete with Undo**: Swipe left on any expense to delete it with an immediate confirmation dialog and an interactive 4-second **UNDO** `Snackbar`.
- **Tap to Edit**: Modify any logged expense directly in a dedicated bottom sheet.

### 2. 👥 Split Expenses & Friends Hub
- **Group Splitting**: Split any expense among friends with one tap directly inside the expense creation sheet or settlement hub.
- **Three Division Modes**:
  - **Equal Division**: Splits the total equally among all selected participants.
  - **Percentage Split**: Custom percentage allocations per friend.
  - **Manual Entry**: Specify exact monetary amounts per participant.
- **Saved Friends Directory**: Add or remove frequent friends (e.g. Rahul, Priya, Alex) with instant auto-suggest chips.
- **Settlement Tracking & Checkbox System**:
  - Quick checkbox toggles to mark when someone pays you back.
  - Dedicated **Who Owes Me & Split Hub** on the home screen showing total pending receivables and individual breakdowns.
  - Visual status chips distinguishing **Pending** from **Settled**.
- **Visual Split Graph**: Interactive graphic chart illustrating per-person breakdown and collected vs. pending debts.

### 3. 🎛️ Flexible Budget Controller & Inflow Tracking
- **"My Funds" Flexible Tracker**:
  - Ditch rigid arbitrary spending quotas: log the exact money you receive from parents, freelance earnings, pocket money, or gifts.
  - Live **Available to Spend** balance calculation (`Total Funds Added - Month Spent`).
  - History of received funds with timestamps, sources, and deletion support.
- **Optional Monthly Spending Limit**:
  - Toggle the monthly spending limit **ON** or **OFF** at will.
  - When enabled, features real-time **Safe Daily Pace Calculator** (`Remaining Budget / Days Left`) and visual pace indicators.
  - Quick steppers (`-$5k`, `-$1k`, `+$1k`, `+$5k`), percentage scalers (`-20%`, `-10%`, `+10%`, `+20%`), and a fluid drag slider.
  - Preset benchmarks (`₹15k`, `₹25k`, `₹35k`, `₹50k`, `₹75k`, `₹100k`).
- **Category-Level Budget Limits**: Allocate spending caps per category (e.g., Food: ₹8,000, Shopping: ₹4,000) with visual progress meters.

### 4. 📊 Monthly Analytics & Data Visualization
- **Month-over-Month Navigation**: Effortlessly step through past and upcoming months with `←` and `→` controls.
- **Key Financial Metrics**: Monthly total spend, month-over-month percentage change (`%`) badge, daily average expenditure, and peak spend day.
- **Category Donut Chart**: Compose Canvas donut chart with animated sweep angles, central total display, slice tap selection, and legend breakdown.
- **Daily Spending Bar Chart**: Visualizes day-by-day expenditure across the month with weekend indicators and peak day highlight.
- **6-Month Trend Line Chart**: Smooth cubic Bézier curve displaying month-over-month trajectory with gradient underfill.
- **Top Categories Breakdown**: Ranked list with proportional progress indicators and transaction counts.

### 5. 🏷️ Category Management
- **Predefined Categories**: Seeded defaults (*Food*, *Transport*, *Rent*, *Bills*, *Shopping*, *Health*, *Entertainment*, *Education*, and *Other*), each with distinct icons and colors.
- **Custom Categories**: Create, edit, and color-code custom categories from a curated icon set and color palette.
- **Safe Reassignment**: Deleting any category automatically reassigns all past and future expenses to *"Other"*, keeping historical balances intact.

### 6. 📅 Calendar Range Data Management
- **Clear Specific Date Range**: Select "From" and "To" dates via calendar dialogs to permanently delete expenses within that range without wiping categories or custom funds.
- **Clear All Expenses**: One-tap full wipe for expense records while cleanly preserving user categories and preferences.

### 7. 💾 Storage Access Framework (SAF) Backup & Restore
- **Privacy-First**: No storage permissions required. Uses Android's Storage Access Framework (`CreateDocument` / `OpenDocument`).
- **Versioned JSON Backup**: Exports complete expenses, splits, friends, categories, and settings to a structured, versioned JSON file (`spendwise_backup_*.json`).
- **Inspection Preview**: Validates schema and displays record counts, date span, and total amounts before restoring.
- **Flexible Restore Modes**: Choose between **"Replace All Data"** (clean restore) or **"Merge (Skip Duplicates)"** (preserves existing records).
- **30-Day Backup Overdue Alert**: Automatically flags an alert if no backup has occurred within 30 days.

### 8. 📑 Spreadsheet Reports (CSV & Native Excel .xlsx)
- **Flexible Scope**: Export for the *Current Month*, a *Custom Date Range*, or *All-Time Data*.
- **RFC 4180-Compliant CSV**: Escapes commas, quotes, and line breaks properly and prepends a `UTF-8 BOM` (`\uFEFF`) so Microsoft Excel and Google Sheets open special characters without encoding errors.
- **Native Dual-Sheet Excel (`.xlsx`)**: Zero-dependency OpenXML ZIP generator producing genuine Excel workbooks:
  - *Sheet 1 (All Expenses)*: Formatted transactions table with auto-fit columns and currency formatting.
  - *Sheet 2 (Category Summary)*: Aggregated breakdown with totals, counts, and percentages.
- **Direct Save & System Share**: Save files directly via SAF or share immediately using Android's native share sheet (`FileProvider`).

### 9. 🎨 Modern Executive Material 3 Design System
- Built strictly adhering to **Material Design 3 (M3)** with an **8dp spacing grid** (`SpendWiseSpacing`).
- **Strict Brand Color Palette**:
  - **Primary (`#0171DF`)**: Royal Blue for primary actions, active tabs, and navigation accents (with crisp white text/icons).
  - **Secondary (`#3CAE8B`)**: Mint Green for fund inflows, healthy budget meters, and friend settlement indicators (strictly paired with `#121660` text for WCAG AA contrast).
  - **Tertiary (`#FFD140`)**: Warm Yellow for Floating Action Buttons (FAB), "Add Money" CTA pills, and warning thresholds (strictly paired with `#121660` text).
  - **Deep Navy (`#121660`)**: Anchors primary text on light backgrounds and top app bars, and serves as the dark surface baseline.
- **Theming Modes**: Supports System Default, Forced Light (`#F4F8FC` surface), and Forced Dark (`#080A1F` / `#0E1235` surfaces) with dynamic color disabled for brand consistency.
- **Accessibility**: All interactive targets are at least 48dp with semantic content descriptions and smooth transitions under 300ms.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (Material 3, Material You tokens)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Local Database**: Android Room 2.7.0 (SQLite) via Kotlin Symbol Processing (KSP)
- **Reactive Streams**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Settings Store**: Jetpack DataStore Preferences
- **Visuals**: Bespoke Jetpack Compose Canvas charts (Donut, Bar, Line, Split Graphs)
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
│   │   ├── ExpenseSplitEntity.kt    # Room entity for friend split debts
│   │   ├── FriendEntity.kt          # Room entity for saved friends directory
│   │   ├── FundEntity.kt            # Room entity for received funds / inflows
│   │   ├── ExpenseWithCategory.kt   # Composite Room relation model (expenses + splits)
│   │   ├── PaymentMethod.kt         # Cash, UPI, Card enum
│   │   ├── SplitAllocationMode.kt   # Equal, Percentage, Manual split modes
│   │   └── AnalysisModels.kt        # Aggregation models (MonthSummary, DailySpending, etc.)
│   ├── dao/
│   │   ├── CategoryDao.kt           # Category queries and reassignment logic
│   │   ├── ExpenseDao.kt            # Transaction queries and date range filters
│   │   ├── FriendDao.kt             # Saved friend contacts
│   │   ├── FundDao.kt               # Received fund inflow records
│   │   └── SplitDao.kt              # Split expense debt entries & settlement queries
│   ├── database/
│   │   └── SpendWiseDatabase.kt     # Room database definition with schema migrations
│   ├── preferences/
│   │   └── UserPreferencesRepository.kt # DataStore for currency, budget, theme, & limits
│   ├── seeder/
│   │   └── SampleDataSeeder.kt      # Predefined categories seeding
│   ├── repository/
│   │   └── ExpenseRepository.kt     # Central repository for metrics, funds, and splits
│   ├── backup/
│   │   └── BackupRestoreManager.kt  # JSON serialization, schema validation, and restore
│   └── export/
│       ├── ReportExporter.kt        # CSV exporter with BOM & FileProvider sharing
│       └── XlsxGenerator.kt         # Lightweight OpenXML .xlsx generator
└── ui/
    ├── common/
    │   └── CategoryVisuals.kt       # Material symbols mapping & category badge composables
    ├── charts/
    │   ├── Charts.kt                # Canvas DonutChart, DailyBarChart, and TrendLineChart
    │   └── SplitExpenseGraph.kt     # Visual chart for split debts and settlements
    ├── components/
    │   ├── AddExpenseBottomSheet.kt # Expense entry modal with friend split allocation
    │   └── BudgetManipulationSheet.kt # Inflow fund tracker, monthly limit, & pace simulator
    ├── screens/
    │   ├── HomeScreen.kt            # Summary card, who owes me card, grouped transactions feed
    │   ├── AnalysisScreen.kt        # Month picker, charts, metrics, and budget progress
    │   ├── CategoriesScreen.kt      # Category manager with add/edit/delete dialogs
    │   └── SettingsScreen.kt        # Currency, monthly limit switch, SAF backup, date-range wipe
    ├── theme/
    │   ├── Color.kt                 # Brand Blue, Green, Yellow, and Navy color tokens
    │   ├── Theme.kt                 # Material 3 light/dark color schemes & spacing tokens
    │   └── Type.kt                  # Modern typography definitions
    └── viewmodel/
        ├── ExpenseViewModel.kt      # Transactions, funds, friend splits, and undo state
        ├── AnalysisViewModel.kt     # Reactive monthly summary and navigation
        └── SettingsViewModel.kt     # Preferences, backup streams, and report generation
```

---

## 🧪 Testing

The test suite validates Room database persistence, mathematical aggregations, split allocations, fund balances, backup serialization round-trips, and CSV RFC 4180 escaping:

```bash
# Run all unit and Robolectric tests via Gradle
gradle :app:testDebugUnitTest
```

### Key Test Suites:
- `FundsAndFriendsTest`: Tests inflow fund additions, available balance math, friend management, and split expense allocation modes.
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

## 📦 Automated Release APK via GitHub Actions

SpendWise includes an automated CI workflow at `.github/workflows/release-apk.yml` that builds and packages a signed Release APK on every push to `main` or manually via **Run workflow** (`workflow_dispatch`).

### Required GitHub Repository Secrets

To sign the APK with your own production key:

1. Generate your release keystore with alias `upload`:
   ```bash
   keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Convert the keystore to a Base64 string:
   ```bash
   # Linux / macOS
   base64 -w 0 my-upload-key.jks
   ```
3. In your GitHub repository, go to **Settings > Secrets and variables > Actions > New repository secret** and configure:
   - `SIGNING_KEYSTORE_BASE64`: The Base64-encoded contents of `my-upload-key.jks`.
   - `STORE_PASSWORD`: Keystore password.
   - `KEY_PASSWORD`: Key password.

> **Note on CI Fallback**: If secrets are not configured, the workflow automatically generates a temporary validation keystore with alias `upload` so the release build always finishes cleanly and produces a downloadable artifact (`SpendWise-Release-APK`).

---

## 🔒 Privacy & Permissions

SpendWise requires **NO runtime permissions** and **NO network permissions**:
- `<uses-permission android:name="android.permission.INTERNET" />` is **NOT** declared.
- File exports and backups use the Android **Storage Access Framework** and **FileProvider**, ensuring zero access to private external storage.
