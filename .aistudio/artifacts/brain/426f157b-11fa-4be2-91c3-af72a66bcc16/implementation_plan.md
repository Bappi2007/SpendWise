# Implementation Plan: SpendWise Modern UI/UX Redesign

Redesign the UI/UX of SpendWise into a modern, polished, and professional financial management app using Jetpack Compose and Material 3, adhering strictly to the brand palette and design system requirements without modifying underlying business logic, Room entities, or ViewModel calculations.

---

## 1. Project Analysis & UI/UX Audit

### UI Framework
- **Detected Framework**: 100% **Jetpack Compose** with Material 3 (`androidx.compose.material3`).
- **No XML Layouts**: All screens, components, dialogs, and sheets are built with Compose functions.

### Screens & Navigation Architecture
- **Root Screen & Navigation**: `MainActivity.kt` orchestrates 4 top-level destinations via `NavigationBar` and `Crossfade`:
  1. **Home** (`HomeScreen.kt`): Main dashboard containing the balance header, split settlement card, daily grouped transactions list (expenses & added funds), and primary action FAB.
  2. **Analysis** (`AnalysisScreen.kt`): Financial breakdown with month picker, budget vs spend bar, highest spend day, category spend progress bars, daily breakdown, and settlement split charts.
  3. **Categories** (`CategoriesScreen.kt`): Category list/grid with color badges, icon picker, default vs custom categories, and add/edit category modal.
  4. **Settings** (`SettingsScreen.kt`): User preferences (currency, monthly limit toggle, theme selector), SAF backup/restore, CSV/Excel report exporter, date-range clear, and developer attribution footer.
- **Sub-Views & Modals**:
  - `BudgetManipulationSheet.kt`: 3-tab controller ("My Funds", "Monthly Limit", "Category Targets").
  - `AddExpenseBottomSheet.kt`: Comprehensive expense creation & editing with split friend allocation.
  - `SettlementSummaryDialog.kt`: Friend settlement tracker with payment status toggles.
  - `ClearDateRangeDialog.kt`, `RestorePreviewDialog.kt`, `CsvExcelExportDialog.kt`.

### Identified UI/UX Issues & Opportunities
1. **Inconsistent Color System**: The app previously mixed legacy emerald/mint greens (`#064E3B`, `#34D399`, `#10B981`) and ad-hoc hex values (`0xFF059669`, `0xFF10B981`) with default Material colors. Needs unified migration to the brand palette.
2. **Contrast & Accessibility Violations**: White text was occasionally paired with light green or yellow badges, violating WCAG AA. On green (`#3CAE8B`) and yellow (`#FFD140`), the text/icon color MUST be deep navy (`#121660`). On primary blue (`#0171DF`), text/icon must be white.
3. **Card Inconsistencies & Clutter**: Disparate corner radii (8dp, 12dp, 16dp, 20dp, 24dp) and inconsistent padding create visual dissonance. Needs a structured 8dp spacing grid and standard shape tokens.
4. **Navigation & Top App Bar**: Navigation bar lacks distinctive brand styling and active pill indicators. Screen headers lack a unified top bar with deep navy branding.
5. **Key Highlights & FAB**: Primary creation actions should leverage the tertiary accent `#FFD140` (yellow) with `#121660` for immediate thumb-zone recognition and high conversion.
6. **Micro-Interactions & Transitions**: List item changes and dialog reveals need smooth, snappy animations (<300ms) with proper ripple states and 48dp minimum touch targets.

---

## 2. Design System & Brand Palette Specification

### Brand Palette Tokens
- **Primary**: `#0171DF` (Royal Blue) — Primary interactive buttons, key progress indicators, active tab selection.
  - *On-Primary*: `#FFFFFF` (White text and icons)
  - *Primary Container (Light)*: `#E0EEFF` (Soft blue tint), *On-Primary Container*: `#004794`
  - *Primary Container (Dark)*: `#004B99`, *On-Primary Container*: `#D3E4FF`
- **Secondary**: `#3CAE8B` (Mint Green) — Success states, inflow/funds added badges, positive balances, settled splits.
  - *On-Secondary*: `#121660` (Deep Navy text and icons — strictly enforced)
  - *Secondary Container (Light)*: `#DDF5ED`, *On-Secondary Container*: `#121660`
  - *Secondary Container (Dark)*: `#1B4E3E`, *On-Secondary Container*: `#A8E6D3`
- **Tertiary / Accent**: `#FFD140` (Warm Sunburst Yellow) — FAB, urgent alert badges, highlighted active pills, focal callouts.
  - *On-Tertiary*: `#121660` (Deep Navy text and icons — strictly enforced)
  - *Tertiary Container (Light)*: `#FFF3CD`, *On-Tertiary Container*: `#121660`
  - *Tertiary Container (Dark)*: `#524000`, *On-Tertiary Container*: `#FFE28A`
- **Deep Navy Base**: `#121660` — Core brand anchor:
  - *Text on light theme*: `#121660` (High contrast, crisp legibility)
  - *Headers / Top App Bar background*: Deep Navy gradient or solid `#121660`
  - *Dark theme surface base*: Very dark navy (`#080A1E` background, `#0E1235` card surfaces, `#151B4D` elevated containers)
- **Light Theme Background**: `#F4F8FC` (Near-white with faint cool blue tint)
  - *Surface Light*: `#FFFFFF` with subtle `#D7E3F0` outline borders

### Typography & Spacing Tokens
- **Typography Scale**: Clean Google-spec Material 3 Typography using distinct weight hierarchy (Bold headlines, Medium labels, Regular body).
- **Spacing Grid**: Standard 8dp spatial tokens (`SpaceExtraSmall = 4.dp`, `SpaceSmall = 8.dp`, `SpaceMedium = 16.dp`, `SpaceLarge = 24.dp`, `SpaceExtraLarge = 32.dp`).
- **Shapes**: Standardized Corner Radii:
  - Small elements (chips, badges, pills): `8.dp`
  - Medium cards & inputs (text fields, action items): `16.dp`
  - Large containers (header summary card, bottom sheets): `24.dp`

---

## 3. Proposed File Changes

### Theme & Global Styling
- `app/src/main/java/com/example/ui/theme/Color.kt`:
  - Define `BrandBlue` (`#0171DF`), `BrandGreen` (`#3CAE8B`), `BrandYellow` (`#FFD140`), `BrandNavy` (`#121660`).
  - Define tonal containers, outlines, light backgrounds (`#F4F8FC`), and dark navy surfaces (`#080A1E`, `#0E1235`, `#151B4D`).
  - Retain category default colors with high contrast.
- `app/src/main/java/com/example/ui/theme/Theme.kt`:
  - Build complete `SpendWiseLightColorScheme` and `SpendWiseDarkColorScheme` matching the strict brand palette rules.
  - Disable dynamic color (`dynamicColor = false` by default) so the signature brand identity always applies.
  - Configure `MaterialTheme` with custom `Shapes` and `Typography`.

### Navigation & App Shell
- `app/src/main/java/com/example/MainActivity.kt`:
  - Add a styled, cohesive top bar or unified header with deep navy background and title branding.
  - Style the `NavigationBar` with `BrandNavy` text, `BrandBlue` or `BrandYellow` active indicator pill, and crisp elevated background.
  - Ensure edge-to-edge window insets padding.

### Core Screens & Components
- `app/src/main/java/com/example/ui/screens/HomeScreen.kt`:
  - **Header Card (`HomeHeaderCard`)**: Update gradient from legacy green to a sleek, modern Deep Navy (`#121660`) to Royal Blue (`#0171DF`) gradient.
  - **"Add Money" Pill**: Styled in `#FFD140` (yellow) or `#3CAE8B` (green) with `#121660` text/icons.
  - **Split Hub Card (`WhoOwesMeCard`)**: Polish with rounded 16dp container, navy text, green settled badges (`#3CAE8B` on `#121660`).
  - **Recent Transactions Feed**:
    - Fund Inflows: Decorated with `#3CAE8B` icon container, `+₹...` bold green amount, and `#121660` badge text.
    - Expense Outflows: Category badges, payment method chips, and `-₹...` amounts.
    - Daily Headers: High contrast label with fund (+green) and expense (-navy/error) totals.
  - **Floating Action Button (FAB)**: Styled in Tertiary Accent `#FFD140` (yellow) with `#121660` Add icon for high visual affordance.
  - **Empty State**: Friendly illustration and dual action buttons ("Add Funds" in outline, "Add Expense" in filled brand blue).

- `app/src/main/java/com/example/ui/screens/AnalysisScreen.kt`:
  - Month selector chips with brand blue selection and navy text.
  - Spending summary cards with modern progress bars and budget status badges.
  - Polish the 6-month trend bar visualization and settlement breakdown charts.

- `app/src/main/java/com/example/ui/screens/CategoriesScreen.kt`:
  - Grid/list cards with modern surface elevation, 16dp rounded corners, and clear custom vs default status.
  - Category icon badges with balanced color luminescence.

- `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`:
  - Settings sections grouped in elegant elevated cards.
  - Brand-colored switches and selection chips.
  - Developer footer retained and styled in deep navy with soft secondary typography:
    ```
    SpendWise v1.0
    Offline-first Jetpack Compose Room Database
    Developed by Bishal Mandal
    ```

- `app/src/main/java/com/example/ui/components/BudgetManipulationSheet.kt` & `AddExpenseBottomSheet.kt`:
  - Modern bottom sheet layout with 24dp top rounding.
  - TabRow with brand blue pill indicator.
  - Steppers, sliders, and percentage chips using brand blue and green accents.
  - Inline validation feedback for amount entries.

---

## 4. Verification Plan

### Automated Testing
- Execute local unit & Robolectric tests:
  `gradle :app:testDebugUnitTest`
  - Verify that `AggregationTest`, `FundsAndFriendsTest`, `BudgetManipulationTest`, `SeedingPersistenceTest`, and `SplitExpensesTest` continue to pass 100%.
- Applet compilation check:
  `compile_applet`
  - Verify zero Kotlin compiler errors, valid Compose imports, and zero deprecation breakages.

### Manual Review & Accessibility Checks
- Contrast verification: Ensure all text on `#3CAE8B` (green) and `#FFD140` (yellow) uses `#121660`.
- Touch target inspection: All buttons, chips, and icons meet the minimum 48dp touch target guideline.
- Dark & Light mode toggle verification: Seamless visual consistency across system and manual theme toggling.
