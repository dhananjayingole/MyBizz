# MyBizz — Billing Management App

## 📱 App Overview

MyBizz is a comprehensive Android billing and business management application developed as a freelance project to help small business owners manage their invoices, expenses, and client billing across multiple service categories — **General Billing**, **Rental**, **Task**, **Construction**, and **Plot Management**.

The app targets micro and small enterprises that need an affordable, mobile-first solution to track financial activity without expensive ERP software.

| Attribute | Detail |
|---|---|
| Platform | Android (min SDK 24 / Android 7.0+) |
| Language | Kotlin 2.0.21 |
| UI Framework | Jetpack Compose + Material Design 3 |
| Architecture | MVVM (Model-View-ViewModel) |
| Backend | Firebase Authentication + Firestore, Google Sheets API v4 |
| Package ID | `eu.tutorials.mybizz` |
| Version | 1.0 |
| Developer | Dhananjay Ingole |

---

## 🚀 Features

### Core Functionality
- **Multi-Category Billing** — Create and manage bills across:
  - 💰 General Billing — Custom invoices, expense tracking, client billing
  - 🏠 Rental Services — Tenant and property rental records
  - ✅ Task-based Projects — Task billing with status tracking
  - 🏗️ Construction Projects — Project-based site billing
  - 📊 Plot Management — Land/plot visitor and sales tracking

### Bill Management
- ✅ Create new bills with auto-generated bill numbers
- ✏️ Edit existing bills with full version history and audit trail
- 🗑️ Delete bills with confirmation dialog
- 💰 Mark bills as Paid/Unpaid with payment date and payer name
- 🔍 Filter bills by status (paid/unpaid) and category
- 📋 Full bill detail view with complete edit history

### Payment & Finance
- 💳 **Razorpay Integration** — Card, net banking, and wallet payments
- 📲 **UPI Payments** — Intent-based launch of any UPI app (Google Pay, PhonePe, etc.)
- 📊 **Monthly Reports** — Visual dashboards with Vico Charts
- 📄 **PDF Export** — Multi-page A4 reports with income/expense/net profit summary

### Smart Features
- 🔔 **Payment Reminder Notifications** — WorkManager background reminders for overdue and upcoming bills
- 🤖 **AI Chatbot** — Business query assistant backed by Retrofit API with source document attribution
- 📩 **Banking SMS Reader** — Parses bank transaction SMS for payment reconciliation
- ☁️ **Google Sheets Sync** — Every create/update/delete syncs to a dedicated Google Sheet tab
- 🌐 **Multi-Language Support** — Runtime locale switching
- 📢 **AdMob App-Open Ads** — Non-intrusive monetisation on foreground transitions

---

## 🛠️ Technology Stack

| Category | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.0.21 |
| UI Toolkit | Jetpack Compose | BOM 2024.09.00 |
| UI Components | Material Design 3 | Material3 Compose |
| Navigation | Navigation Compose | 2.7.7 |
| Authentication | Firebase Auth KTX | BOM 33.3.0 |
| Database | Firebase Firestore KTX | BOM 33.3.0 |
| Cloud Storage | Google Sheets API v4 | v4-rev20220927-2.0.0 |
| HTTP Client | Retrofit2 + OkHttp3 | 2.11.0 / 4.12.0 |
| Async | Kotlin Coroutines | 1.7.1 |
| Background Work | WorkManager | 2.9.0 |
| Payments | Razorpay Checkout | 1.6.33 |
| Ads | Google AdMob | play-services-ads 24.4.0 |
| Charts | Vico Charts (compose-m3) | 1.12.0 |
| Animations | Lottie Compose | 6.0.0 |
| Google Auth | google-auth-library-oauth2-http | 1.19.0 |

---

## 🏗️ Project Architecture

MyBizz follows the **MVVM (Model-View-ViewModel)** pattern with a single-activity architecture.

```
┌──────────────────────────────────────────┐
│              View Layer                   │
│   Jetpack Compose — UIScreens/            │
│   Observes StateFlow, drives NavController│
└─────────────────┬────────────────────────┘
                  │
┌─────────────────▼────────────────────────┐
│            ViewModel Layer                │
│   ChatViewModel, PaymentViewModel,        │
│   MonthlyReportViewModel                  │
│   (StateFlow / LiveData + Coroutines)     │
└─────────────────┬────────────────────────┘
                  │
┌─────────────────▼────────────────────────┐
│           Repository Layer                │
│   Logic/ — Firestore CRUD operations      │
│   *SheetsRepository — Google Sheets sync  │
└─────────────────┬────────────────────────┘
                  │
┌─────────────────▼────────────────────────┐
│             Model Layer                   │
│   Pure Kotlin data classes                │
│   Bill, Rental, Task, Construction,       │
│   Plot, Payment, User                     │
└──────────────────────────────────────────┘
```

### Package Structure

```
eu.tutorials.mybizz/
├── ads/                      # AppOpenAdManager, AppLifecycleObserver
├── bankingSms/               # BankSMS model, BankSMSRepository, BankSMSScreen
├── Chatbot/                  # ChatScreen, ChatViewModel, ChatRepository, RetrofitClient
├── language/                 # LocalManager — runtime locale switching
├── Logic/
│   ├── Auth/                 # AuthRepository — Firebase auth + role caching
│   ├── Bill/                 # BillRepository + BillSheetsRepository
│   ├── Construction/         # ConstructionRepository + ConstructionSheetsRepository
│   ├── Rental/               # RentalRepository + RentalSheetsRepository
│   ├── Task/                 # TaskRepository + TaskSheetsRepository
│   └── plot/                 # PlotRepository + PlotSheetsRepository
├── Model/                    # Bill, User, Construction, Rental, Task, Plot, Payment
├── Navigation/               # NavGraph.kt, Routes.kt
├── Notification/             # NotificationHelper, PaymentReminderWorker, ReminderScheduler
├── Payments/                 # PaymentScreen, PaymentViewModel, RazorpayPaymentHandler, UpiPaymentHandler
├── pdfgen/                   # PdfGenerator — Android PdfDocument multi-page builder
├── Reporting/                # MonthlyReportScreen, MonthlyReportViewModel, MonthlyReportRepository
├── UIScreens/                # All Jetpack Compose screens
├── ui.theme/                 # Color.kt, Theme.kt, Type.kt (Material 3)
├── DateUtils.kt              # Date formatting / parsing extension functions
├── MainActivity.kt           # App entry point, NavHost, WorkManager setup
└── MyBizzApp.kt              # Application class — Firebase, AdMob, singletons init
```

---

## 📋 Screens & Navigation

Single-activity architecture with a single `NavHost`. All routes are defined as constants in `Routes.kt`.

| Screen | Route | Purpose |
|---|---|---|
| SplashScreen | `splash_screens` | Startup animation; redirects based on auth state |
| LoginScreen | `login_screens` | Email/password login via Firebase |
| SignUpScreen | `signup_screens` | New user registration with role selection |
| UserDashBoardScreen | `user_dashboard` | Standard user home with category navigation |
| AdminDashBoardScreen | `admin_dashboard` | Admin home with user management access |
| BillsListScreen | `bills_list` | Paginated bill list with filter/search |
| AddBillScreen | `add_bill` | Create a new bill |
| EditBillScreen | `edit_bill/{billId}` | Edit existing bill, increments version |
| BillDetailScreen | `bill_details/{billId}` | Full bill detail + pay/mark-paid actions |
| RentalScreen | `rental_list` | Rental records with status badges |
| ConstructionScreen | `construction_list` | Construction project list and management |
| TaskScreen | `task_list` | Task-based project list |
| PlotScreens | `plotlistscreens` | Plot/land management records |
| PaymentScreen | `paymentscreen` | Razorpay + UPI payment dialog |
| MonthlyReportScreen | `monthlyreportscreen` | Charts + PDF export |
| ChatScreen | `chatscreen` | AI chatbot interface |
| BankSMSScreen | `banksmsscreen` | Banking SMS parser |
| ProfileScreen | `profilescreen` | User profile and logout |
| UserManagementScreen | `usermanagementscreen` | Admin: view, block/unblock users |
| SettingScreen | `settingscreens` | Notification toggles and preferences |
| LanguageSelectorDialog | `languageselectionscreen` | Language picker |

---

## 💾 Data Models

<details>
<summary><strong>Bill</strong></summary>

| Field | Type | Description |
|---|---|---|
| id | String | UUID |
| billNumber | String | Auto-generated (e.g. `2024001`) |
| version | Int | Edit version counter for audit trail |
| title / description | String | Bill summary and detail |
| amount | Double | Amount in INR |
| dueDate / paidDate | String | yyyy-MM-dd dates |
| status | String | `"paid"` or `"unpaid"` |
| category | String | Worker Payment, Maintenance, Tax, Insurance, Other |
| paidBy | String | Name of person who paid |
| createdBy / modifiedBy | String | Audit trail emails + timestamps |

</details>

<details>
<summary><strong>Rental</strong></summary>

| Field | Type | Description |
|---|---|---|
| id | String | UUID |
| tenantName / property | String | Tenant and property info |
| rentAmount | Double | Monthly rent in INR |
| month | String | `yyyy-MM` format |
| status | String | `"paid"` or `"unpaid"` |
| contactNo | String | Tenant phone (enables direct call) |

</details>

<details>
<summary><strong>Construction</strong></summary>

| Field | Type | Description |
|---|---|---|
| id | String | UUID |
| projectName / location | String | Project name and site address |
| startDate / endDate | String | Project duration |
| cost | String | Estimated/actual project cost |
| status | String | e.g. "In Progress", "Completed" |
| notes | String? | Optional project notes |

</details>

<details>
<summary><strong>Task</strong></summary>

| Field | Type | Description |
|---|---|---|
| id | String | UUID |
| title / description | String | Task summary and detail |
| assignedTo | String | Person responsible |
| dueDate | String | Task deadline |
| status | String | e.g. pending, completed |

</details>

<details>
<summary><strong>Plot</strong></summary>

| Field | Type | Description |
|---|---|---|
| id / plotId / plotName | String | Identifiers |
| location / plotSize | String | Geography and area measurement |
| visitorName / visitorNumber / visitorAddress | String | Prospective buyer details |
| askingAmount / initialPrice | String | Price negotiation fields |
| attendedBy / visitDate | String | Sales agent and visit tracking |

</details>

<details>
<summary><strong>Payment</strong></summary>

| Field | Type | Description |
|---|---|---|
| transactionId | String | Gateway reference ID |
| billId / rentalId | String | Links payment to a bill or rental record |
| paymentMethod | String | UPI, CARD, NET_BANKING, WALLET |
| status | String | pending, success, failed |
| razorpayOrderId / razorpayPaymentId / razorpaySignature | String | Razorpay verification fields |
| upiTransactionId | String | UPI-specific transaction reference |

</details>

---

## 🔧 Setup & Installation

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 11
- Android SDK — API level 24 through 36
- Google account with access to Firebase Console and Google Cloud Console
- Razorpay account — test mode key at [dashboard.razorpay.com](https://dashboard.razorpay.com)

### 1. Clone the Repository

```bash
git clone https://github.com/dhananjayingole/MyBizz.git
cd MyBizz
```

### 2. Firebase Configuration

1. Create a new project at [console.firebase.google.com](https://console.firebase.google.com)
2. Enable **Email/Password Authentication** under Authentication → Sign-in methods
3. Create a **Firestore Database** (test mode for development)
4. Register an Android app with package name `eu.tutorials.mybizz`
5. Download `google-services.json` and place it in the `app/` directory

> ⚠️ **Never commit `google-services.json` to a public repository.**

### 3. Google Sheets API Configuration

1. Enable the **Google Sheets API** in [Google Cloud Console](https://console.cloud.google.com)
2. Create a **Service Account** and download the JSON credentials key
3. Share your target Google Sheets with the service account email (Editor role)
4. Update the spreadsheet IDs and credentials path in each `*SheetsRepository` file

### 4. Razorpay Key

Update the key in `Payments/Razorpaypaymenthandler.kt`:

```kotlin
val options = JSONObject()
options.put("key", "YOUR_RAZORPAY_KEY_HERE")
```

### 5. Build and Run

```bash
./gradlew assembleDebug
```

Or open the project in Android Studio and run directly on a device or emulator.

### Recommended Firestore Security Rules (Production)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /{collection}/{docId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 📦 Download APK

> No pre-built release APK is currently published. To generate a debug APK, clone the repo, complete Firebase and Sheets configuration above, then run:

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

For a release build:

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

Once a release is published, it will be available under the **[Releases](https://github.com/dhananjayingole/MyBizz/releases)** tab on GitHub.

---

## 🔐 Security

- Firebase Authentication with email/password
- Role-based access control — `user` and `admin` roles cached in SharedPreferences
- Service account-based Google Sheets API auth (OAuth2)
- Razorpay signature verification for payment integrity
- Runtime permissions handled gracefully (Android 13+ `POST_NOTIFICATIONS`, `READ_SMS`)

---

## 📊 Future Enhancements

- [ ] PDF Invoice Sharing — WhatsApp/email sharing (FileProvider already configured)
- [ ] Advanced Analytics — Revenue trend charts, year-over-year comparisons
- [ ] Client Management System — Client profiles with aggregated billing history
- [ ] FCM Push Notifications — Server-triggered reminders (dependency already included)
- [ ] Offline Mode — Room database cache with sync-on-reconnect
- [ ] Full Localisation — Hindi and Marathi string resources
- [ ] Dark/Light Theme Toggle — Material 3 dynamic theming already set up
- [ ] Tenant Management — Dedicated tenant profiles with multiple rental records

---

## 🤝 Contributing

Contributions are welcome! Feel free to open a pull request or file an issue for bugs and feature requests.

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'Add your feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 📞 Support & Contact

For support and queries, please contact:
- **Email**: dhananjayingole2004@gmail.com
- **GitHub Issues**: [Create an issue](https://github.com/dhananjayingole/MyBizz/issues)

---

## 🏆 Acknowledgments

- [Jetpack Compose](https://developer.android.com/compose) — Modern Android UI toolkit
- [Firebase](https://firebase.google.com) — Authentication and Firestore backend
- [Google Sheets API](https://developers.google.com/sheets/api) — Flexible cloud data storage
- [Razorpay](https://razorpay.com) — Payment gateway integration
- [Vico Charts](https://patrykandpatrick.com/vico/) — Beautiful chart library for Compose
- [Material Design 3](https://m3.material.io) — Design system and components

---

<div align="center">

**Built with ❤️ using Kotlin · Jetpack Compose · Firebase · Google Sheets API**

</div>
