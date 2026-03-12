# Finance Management App

An Android application designed to help users track their personal finances, manage accounts, monitor loans/debts, and set budget goals. This project uses Firebase for real-time data synchronization and authentication.

## 🚀 Features

### 💰 Transaction & Account Management
- **Expense & Income Tracking**: Easily record your daily transactions with categories.
- **Multiple Accounts**: Manage different wallets, bank accounts, or credit cards in one place.
- **Categories**: Organize spending with customizable categories and icons.
- **Transaction History**: View and filter all past financial activities.

### 📊 Budgeting & Planning
- **Budget Tracking**: Set monthly or category-based budgets to control spending.
- **Payment Reminders**: Stay on top of your bills with automated notifications and reminders.

### 🏦 Loans & Debts
- **Debt Management**: Keep track of money you owe or others owe you.
- **Repayment Tracking**: Log partial or full repayments for active loans.

### 💬 Intelligent Assistant
- **AI Chat**: Integrated AI assistant to help answer financial queries or guide you through the app.

### 🔐 Security & Sync
- **Firebase Authentication**: Secure login and registration.
- **Cloud Sync**: All data is stored in Firebase Firestore, ensuring your data is accessible across devices.
- **Profile Management**: Update user information and change passwords securely.

## 🛠 Tech Stack

- **Language**: Java
- **UI Framework**: Android XML (Material Design)
- **Backend**: Firebase Firestore (NoSQL Database)
- **Authentication**: Firebase Auth
- **Architecture**: DAO (Data Access Object) pattern for data management.

## 📂 Project Structure

- `ui/`: Contains Activity and Fragment classes for various screens (Auth, Chat, Home, Transactions, etc.).
- `data/`: Handles data models and data source interactions (FirebaseDAO).
- `res/`: Contains layout files, drawables, and string resources.

## ⚙️ Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone <your-repo-url>
   ```

2. **Firebase Setup**:
   - Create a new project on the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with the package name `com.example.myapplication`.
   - Download the `google-services.json` file and place it in the `app/` directory.
   - Enable **Email/Password** authentication in Firebase Auth.
   - Enable **Cloud Firestore** and set up your security rules.

3. **Build & Run**:
   - Open the project in Android Studio.
   - Sync project with Gradle files.
   - Run the app on an emulator or a physical device.
