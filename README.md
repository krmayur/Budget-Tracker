# PocketWise Budget — Offline Android Budget Tracker

A 100% offline Android personal finance app built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room (SQLite)**. Zero internet permissions, zero cloud sync, and full local privacy.

## Features

- **Monthly Salary Input**: Set and update your monthly take-home salary (plus optional bonus/other income) per month with month-by-month navigation.
- **Multiple Loans & EMI Manager**: Add unlimited loans (Home, Auto, Personal, Education, Credit Card EMI, etc.) with monthly EMI, optional remaining principal, interest rate (%), and remaining tenure. Pause or mark loans paid off anytime.
- **Investments Tracker**: Log monthly investments (SIP / Mutual Funds, Stocks, PPF / EPF / NPS, Fixed Deposit, Gold, Emergency Fund) with category breakdowns and investment rate tracking.
- **Personal Expenditure Tracker**: Log daily personal expenses by category, tag each as **Essential** or **Discretionary**, filter by category, and track spending share.
- **100% Offline Local Storage**: Powered by Android Room SQLite (`budget_tracker_offline.db`). Works in Airplane Mode with no `INTERNET` permission in `AndroidManifest.xml`.
- **Offline Savings Insights Engine**:
  - **Financial Health Score (0–100)** based on savings rate, EMI-to-income (DTI) ratio, discretionary spending, and cashflow balance.
  - **50/30/20 Benchmark Comparison** (Needs vs. Wants vs. Savings & Investments).
  - **Prioritized Actionable Recommendations** with concrete ₹ monthly and annual savings estimates (cashflow deficit alerts, high-EMI avalanche/snowball payoff suggestions, top discretionary cut targets, idle surplus allocation, investment rate gap, and frequent-expense audits).
  - **Interactive What-If Savings Simulator**: Adjust a discretionary spending cut slider (0%–50%) to see instant monthly, 1-year, and 5-year compounded SIP projections.

## Option 1 (No Android Studio Needed): Build the APK via GitHub Actions

This project includes a ready-to-run workflow at `.github/workflows/build-apk.yml` that builds your installable `.apk` for free in the cloud:

1. Extract `BudgetTrackerAndroid.zip`.
2. Create a new free repository on GitHub (public or private).
3. Upload all files inside the `BudgetTrackerAndroid` folder (including the hidden `.github/workflows/build-apk.yml` file) or push them with Git:
   ```bash
   cd BudgetTrackerAndroid
   git init -b main
   git add .
   git commit -m "Initial commit: PocketWise Budget"
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
4. Open the **Actions** tab in your GitHub repository and click the running **Build Android APK** workflow.
5. Once it finishes (~2–3 minutes), scroll down to **Artifacts** and download **PocketWise-Budget-APK** (unzip to get `app-debug.apk`).
6. Transfer `app-debug.apk` to your Android phone and tap it to install (allow "Install unknown apps" if prompted).

## Option 2: Open & Run in Android Studio

1. Extract `BudgetTrackerAndroid.zip`.
2. Open **Android Studio** (Ladybug 2024.2+ recommended, with JDK 17).
3. Select **File → Open** and choose the extracted `BudgetTrackerAndroid` folder.
4. Wait for Gradle sync to download AndroidX / Compose / Room dependencies (internet is only needed during build-time Gradle sync on your computer; the compiled app runs 100% offline on Android).
5. Click **Run ▶** to install on an Android emulator or physical Android phone (Android 8.0 / API 26+).

## Project Structure

```text
app/src/main/java/com/budgettracker/offline/
├── BudgetTrackerApp.kt          # Application class initializing Room DB & Repository
├── MainActivity.kt              # Navigation host & bottom navigation bar
├── data/
│   ├── local/                   # Room entities, DAO, and SQLite Database
│   └── repository/              # Single source of truth repository
├── domain/
│   └── SavingsInsightsEngine.kt # Offline rule-based insights & financial health engine
├── ui/
│   ├── components/              # Reusable Material 3 cards, bars, and input helpers
│   ├── screens/                 # Dashboard, Loans, Investments, Expenses, Insights
│   ├── theme/                   # Material 3 color palette & typography
│   └── viewmodel/               # StateFlow-based MVVM ViewModel
└── util/
    ├── CurrencyFormatter.kt     # Indian Rupee (₹) formatting helper
    └── MonthUtils.kt            # Month key navigation (YYYY-MM)
```

## Running Unit Tests

Run `SavingsInsightsEngineTest` from Android Studio or via terminal:

```bash
./gradlew testDebugUnitTest
```
