# SpendWise 💰

[![Download Latest APK](https://img.shields.io/badge/Download_Latest_APK-SpendWise_v1.0.0-0171DF?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Bappi2007/SpendWise/releases/latest)
[![GitHub Release](https://img.shields.io/github/v/release/Bappi2007/SpendWise?style=for-the-badge&logo=github&color=3CAE8B)](https://github.com/Bappi2007/SpendWise/releases/latest)
[![Platform](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-FFD140?style=for-the-badge&logo=android&logoColor=121660)](https://github.com/Bappi2007/SpendWise/releases)
[![Offline First](https://img.shields.io/badge/Privacy-100%25_Offline-121660?style=for-the-badge)](https://github.com/Bappi2007/SpendWise)

> A modern, privacy-first native Android personal finance & expense manager built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Room Database**.

SpendWise operates 100% offline with **zero network permissions**, keeping all financial transactions, fund inflows, and friend debt settlements securely on your device. It provides interactive data visualizations, group expense splitting, dedicated friend loan tracking with automated repayment fund restoration, a flexible budget controller with fund tracking, calendar-based data management, local JSON backups via Android's Storage Access Framework (SAF), and spreadsheet exports (CSV & native Excel `.xlsx`).

---

## 📥 Download SpendWise APK

Get the latest pre-built Android APK directly from **GitHub Releases**:

### 👉 [**Download Latest APK**](https://github.com/Bappi2007/SpendWise/releases/latest)
*(Direct link: `https://github.com/Bappi2007/SpendWise/releases/latest`)*

| Release Info | Details |
| :--- | :--- |
| **Latest Version** | `v1.0.0` (`versionCode = 1`, `versionName = "1.0"`) |
| **Application ID** | `com.aistudio.spendwise.qvrpnm` |
| **Minimum Android** | Android 8.0 Oreo (API 26) or newer (Target/Compile SDK 36) |
| **Available Assets** | `SpendWise-<tag>-release.apk` (Recommended), `SpendWise-<tag>-debug.apk`, `SHA256SUMS.txt` |
| **All Releases** | [https://github.com/Bappi2007/SpendWise/releases](https://github.com/Bappi2007/SpendWise/releases) |

### 📲 How to Install the APK on Your Android Device
1. Open the [**Latest Release page**](https://github.com/Bappi2007/SpendWise/releases/latest) on your Android phone.
2. Scroll down to the **Assets** section and tap **`SpendWise-v1.0.0-release.apk`** (or the latest version tag) to download.
3. Once downloaded, tap the file in your notification shade or open it via your **Files / Downloads** app.
4. If Android prompts you with *"For your security, your phone currently isn't allowed to install unknown apps from this source"*, tap **Settings** and enable **Allow from this source**.
5. Tap **Install**, then tap **Open** to start using SpendWise!

> **🔐 Note on APK Signing**:
> - When repository secrets (`SIGNING_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`) are configured in GitHub, the Release APK is signed with the persistent production upload key.
> - If those secrets are not configured, the GitHub Actions workflow automatically generates a temporary self-signed certificate during the build so the Release APK can still be installed immediately on any Android device (Android forbids installing completely unsigned APKs). Note that upgrading over a previous installation without uninstalling requires a consistent signing key across releases.

---

## 👨‍💻 Author & Attribution

**Developed by Bishal Mandal**  
- **GitHub**: [@Bappi2007](https://github.com/Bappi2007)  
- **Repository**: [Bappi2007/SpendWise](https://github.com/Bappi2007/SpendWise)  
- **Email**: [bishalkumandal777@gmail.com](mailto:bishalkumandal777@gmail.com)  
- **Version**: SpendWise v1.0 (Offline-first Jetpack Compose Room Database)

---

## 📱 Screenshots & Application Preview

| Home & Executive Summary | Split Hub & Friend Loans | Monthly Analytics & Charts | Flexible Budget Controller |
| :---: | :---: | :---: | :---: |
| *Deep Navy-to-Royal Blue hero card, Available Balance, Who Owes Me hub, and grouped daily feed* | *Equal / Percentage / Manual splits, Lend / Borrow mode, and automatic repayment fund restoration* | *Interactive Canvas Donut Chart, Daily Bar Chart, 6-Month Trend Line, and Income Budget meter* | *"My Funds" inflow log (parents, salary, freelance), ON/OFF monthly limit switch, and safe daily pace* |

*(To display live screenshots here, add images to a `screenshots/` folder in the repository such as `screenshots/home.png`, `screenshots/splits.png`, `screenshots/analytics.png`, and `screenshots/budget.png`.)*

---

## ✨ Key Features

### 1. 📝 Daily Expense Tracking & Grouped Feed
- **Rich Transaction Entry**: Log amount (with customizable currency symbol), category, timestamp (defaults to now, with custom date/time pickers), merchant/title with quick autocomplete chips, payment method (`Cash`, `UPI`, `Card`), and optional remarks.
- **Grouped Transaction Dashboard**: Chronologically organized daily groups (`Today`, `Yesterday`, or formatted date) displaying daily net spending and fund inflows side by side.
- **Swipe-to-Delete with Undo**: Swipe left on any expense to delete it with an immediate confirmation dialog and an interactive 4-second **UNDO** `Snackbar`.
- **Tap to Edit**: Modify any logged expense directly in a dedicated bottom sheet.

### 2. 👥 Split Expenses, Friend Loans & Friends Hub
- **Dedicated Friend Money Lending & Borrowing**:
  - Log money lent to friends or borrowed from friends directly in the primary Add bottom sheet via the top **"Lend / Borrow"** mode switch.
  - **Lending Flow**: Automatically categorizes as *"Money Lent"* (`cat_default_money_lent` with Handshake icon), creates an unpaid debt record in *Who Owes Me*, and deducts the amount from your available funds until the friend repays.
  - **Automated Repayment & Fund Balance Restoration**: When a friend repays and you mark the debt as settled (via the *Who Owes Me* card, *Split Hub*, or transaction details), SpendWise automatically generates an inflow fund entry (`"Repayment from [Friend]"`) that immediately restores the money back into your available balance, accompanied by a confirmation Snackbar with **UNDO** support.
  - **Borrowing Flow**: Log money borrowed from friends, instantly adding the received funds into your available spending balance.
- **Group Bill Splitting**: Split any expense among friends with one tap directly inside the expense creation sheet or settlement hub.
- **Three Division Modes**:
  - **Equal Division**: Splits the total equally among all selected participants.
  - **Percentage Split**: Custom percentage allocations per friend.
  - **Manual Entry**: Specify exact monetary amounts per participant.
- **Saved Friends Directory**: Add or remove frequent friends (e.g. Rahul, Priya, Alex) with instant auto-suggest chips.
- **Settlement Tracking & Checkbox System**:
  - Quick checkbox toggles to mark when someone pays you back.
  - Dedicated **Who Owes Me & Split Hub** on the home screen showing total pending receivables and individual breakdowns.
  - Visual status chips distinguishing **Pending** from **Settled**, with dedicated **"Awaiting Repayment"** and **"Repaid"** handshake badges for friend loans.
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
- **Dynamic Income-Based Budget**: When the monthly limit toggle is switched **OFF**, monthly budget targets are automatically derived from total income (added funds), accurately showing healthy progress rather than flagging a false *"Budget Exceeded"* state.
- **Key Financial Metrics**: Monthly total spend, month-over-month percentage change (`%`) badge, daily average expenditure, and peak spend day.
- **Category Donut Chart**: Compose Canvas donut chart with animated sweep angles, central total display, slice tap selection, and legend breakdown.
- **Daily Spending Bar Chart**: Visualizes day-by-day expenditure across the month with weekend indicators and peak day highlight.
- **6-Month Trend Line Chart**: Smooth cubic Bézier curve displaying month-over-month trajectory with gradient underfill.
- **Top Categories Breakdown**: Ranked list with proportional progress indicators and transaction counts.

### 5. 🏷️ Category Management
- **Predefined Categories**: Seeded defaults (*Food*, *Transport*, *Rent*, *Bills*, *Shopping*, *Health*, *Entertainment*, *Education*, *Money Lent*, and *Other*), each with distinct icons and colors.
- **Custom Categories**: Create, edit, and color-code custom categories from a curated icon set and color palette.
- **Safe Reassignment**: Deleting any category automatically reassigns all past and future expenses to *"Other"*, keeping historical balances intact.

### 6. 📅 Calendar Range Data Management
- **Clear Specific Date Range**: Select "From" and "To" dates via calendar dialogs to permanently delete expenses within that range without wiping categories or custom funds.
- **Clear All Expenses**: One-tap full wipe for expense records while cleanly preserving user categories, saved friends, and preferences.

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
- **Custom Adaptive Launcher Icon**: Branded vector adaptive icon featuring a Deep Navy-to-Royal Blue gradient background with a white wallet, mint green flap, golden clasp, and growth trendline, pre-rendered across all screen densities (`mdpi` through `xxxhdpi`) in standard and round formats.
- **Accessibility**: All interactive targets are at least 48dp with semantic content descriptions and smooth transitions under 300ms.

---

## 🛠️ Tech Stack & Build Configuration

| Component | Version / Configuration |
| :--- | :--- |
| **Language** | Kotlin `2.2.10` |
| **Android Gradle Plugin (AGP)** | `9.1.1` |
| **Gradle Wrapper** | `9.3.1` |
| **KSP (Symbol Processing)** | `2.3.5` |
| **SDK Versions** | `compileSdk = 36` (`minorApiLevel = 1`), `targetSdk = 36`, `minSdk = 26` |
| **Java Target** | `JavaVersion.VERSION_11` bytecode (built with **JDK 17 / JDK 21**) |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM (Model-View-ViewModel) + Repository Pattern |
| **Local Database** | Android Room `2.7.0` (SQLite) |
| **Settings Store** | Jetpack DataStore Preferences `1.1.7` |
| **Testing** | JUnit 4 + Robolectric `4.16.1` |

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
│   │   ├── ExpenseSplitEntity.kt    # Room entity for friend split debts & loans
│   │   ├── FriendEntity.kt          # Room entity for saved friends directory
│   │   ├── FundEntity.kt            # Room entity for received funds / inflows / repayments
│   │   ├── ExpenseWithCategory.kt   # Composite Room relation model (expenses + splits)
│   │   ├── PaymentMethod.kt         # Cash, UPI, Card enum
│   │   ├── SplitAllocationMode.kt   # Equal, Percentage, Manual split modes
│   │   └── AnalysisModels.kt        # Aggregation models (MonthSummary, DailySpending, etc.)
│   ├── dao/
│   │   ├── CategoryDao.kt           # Category queries and reassignment logic
│   │   ├── ExpenseDao.kt            # Transaction queries and date range filters
│   │   ├── ExpenseSplitDao.kt       # Split expense debt entries & settlement queries
│   │   ├── FriendDao.kt             # Saved friend contacts
│   │   └── FundDao.kt               # Received fund inflow & repayment records
│   ├── database/
│   │   └── SpendWiseDatabase.kt     # Room database definition with schema migrations
│   ├── preferences/
│   │   └── UserPreferencesRepository.kt # DataStore for currency, budget, theme, & limits
│   ├── seeder/
│   │   └── SampleDataSeeder.kt      # Predefined categories seeding
│   ├── repository/
│   │   └── ExpenseRepository.kt     # Central repository for metrics, funds, splits, and loans
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
    │   ├── AddExpenseBottomSheet.kt # Expense & friend loan entry modal with split allocation
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
        ├── ExpenseViewModel.kt      # Transactions, funds, friend splits, loans, and undo state
        ├── AnalysisViewModel.kt     # Reactive monthly summary and navigation
        └── SettingsViewModel.kt     # Preferences, backup streams, and report generation
```

---

## 🧪 Testing

The test suite validates Room database persistence, mathematical aggregations, split allocations, fund balances, friend loans with automatic balance restoration, backup serialization round-trips, and CSV RFC 4180 escaping:

```bash
# Run all unit and Robolectric tests via Gradle
./gradlew :app:testDebugUnitTest
```

### Key Test Suites:
- `FundsAndFriendsTest`: Tests inflow fund additions, available balance math, friend management, split expense allocation modes, and **friend money lending with automatic fund restoration upon settlement**.
- `BudgetManipulationTest`: Tests stepper bounds, percentage scaling, and daily safe allowance calculations.
- `CsvEscapingTest`: Validates RFC 4180 quoting, double-quote escaping, multiline notes, and UTF-8 BOM prepending.
- `BackupSerializationTest`: Validates JSON schema compliance, preview metadata extraction, and corrupt file detection.
- `AggregationTest`: Validates month-over-month percentage changes, daily averages, and chronological grouping.
- `ExampleRobolectricTest`: Verifies resource resolution and application context configuration.

---

## 🚀 Building from Source (Developers)

### Prerequisites
- **Android Studio** (Ladybug / Meerkat or newer recommended).
- **Android SDK** API 36 (compileSdk 36, minSdk 26).
- **JDK 17 or JDK 21**.

### Cloning & Running Locally
```bash
git clone https://github.com/Bappi2007/SpendWise.git
cd SpendWise
./gradlew assembleDebug
```

---

## 📦 Automated GitHub Releases & CI Workflows

SpendWise includes two GitHub Actions workflows in `.github/workflows/`:

1. **`.github/workflows/release.yml` (Tag-Based GitHub Releases)**:
   - Automatically triggers whenever a version tag matching `v*` (e.g., `v1.0.0`, `v1.0.1`, `v1.1.0`) is pushed to GitHub, or manually via **Actions > SpendWise Automatic GitHub Release > Run workflow**.
   - Builds both **Release** (`SpendWise-<tag>-release.apk`) and **Debug** (`SpendWise-<tag>-debug.apk`) APKs, generates `SHA256SUMS.txt`, creates a new GitHub Release, and attaches the files to [GitHub Releases](https://github.com/Bappi2007/SpendWise/releases/latest).
2. **`.github/workflows/release-apk.yml` (Main Branch CI Build)**:
   - Runs on every push to `main` and uploads the Release APK as a workflow build artifact.

### Publishing a New Release via Git Tags
```bash
git tag -a v1.0.0 -m "SpendWise v1.0.0 Release"
git push origin v1.0.0
```

### Optional Production Signing Secrets
To sign release APKs with your own persistent production keystore across all future updates:

1. Generate your upload keystore with alias `upload`:
   ```bash
   keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Encode the keystore as Base64:
   ```bash
   base64 -w 0 my-upload-key.jks
   ```
3. In your GitHub repository (**Settings > Secrets and variables > Actions > New repository secret**), add:
   - `SIGNING_KEYSTORE_BASE64`: Base64 string of `my-upload-key.jks`.
   - `STORE_PASSWORD`: Your keystore password.
   - `KEY_PASSWORD`: Your key password for alias `upload`.

---

## 🔒 Privacy & Permissions

SpendWise requires **NO runtime permissions** and **NO network permissions**:
- `<uses-permission android:name="android.permission.INTERNET" />` is **NOT** declared.
- File exports and backups use the Android **Storage Access Framework** and **FileProvider**, ensuring zero access to private external storage.
- All application data is stored in the app's private, sandboxed SQLite database at `/data/user/0/com.aistudio.spendwise.qvrpnm/databases/spendwise_database`.
