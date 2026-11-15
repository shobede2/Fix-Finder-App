# Fix Finder – Home & Repair Service Platform

Fix Finder is a native Android application built in Kotlin that connects households and individuals with verified local service providers and technicians (plumbers, electricians, appliance repair specialists, HVAC technicians, mechanics, and handymen).

---

## Table of Contents
1. [Purpose & Problem Addressed](#purpose--problem-addressed)
2. [Main Features](#main-features)
3. [Technologies & Libraries Used](#technologies--libraries-used)
4. [REST API Architecture](#rest-api-architecture)
5. [Application Structure](#application-structure)
6. [How to Build & Run](#how-to-build--run)
7. [Testing Approach](#testing-approach)
8. [Configuration & Environment](#configuration--environment)

---

## Purpose & Problem Addressed

### Problem
Finding reliable, trustworthy, and skilled service providers for home repairs (such as leaking pipes, electrical faults, broken fridges, or malfunctioning air conditioning) is often difficult, unpredictable, and slow. Users lack transparent pricing, verified customer reviews, and simple booking mechanisms.

### Purpose
Fix Finder solves this problem by providing a centralized digital marketplace where:
- **Customers** can browse service categories, search for qualified local technicians, view transparent pricing and verified reviews, request quotes, and submit service bookings.
- **Technicians** can manage incoming repair jobs, view daily schedules, track monthly earnings, and update their availability.

---

## Main Features

1. **Sign-In & Registration (Authentication)**
   - Role-based login and registration (Customer vs. Technician).
   - Validation for email format, password length, and mandatory fields.
   - Session persistence using `SessionManager` and token handling.
   - Smooth navigation to Home or Dashboard based on user role.

2. **Settings & Profile Management**
   - User profile summary (name, email, role).
   - Configurable notification preferences (Push notifications, Email booking updates, SMS status alerts).
   - Location preference selector for major South African regions (Polokwane, Johannesburg, Pretoria, Cape Town, Durban).
   - Theme mode configuration (System Default, Light Mode, Dark Mode).
   - Secure Logout with confirmation dialog.

3. **Service Search & Category Discovery (User-Defined Feature 1)**
   - Category filtering chips (Plumbing, Electrical, Appliances, Air Conditioning, Electronics, Automotive, Handyman).
   - Real-time debounced keyword search by provider name, service type, or specialty.
   - Comprehensive provider cards displaying experience, completed job counts, ratings, starting prices, and avatar images.
   - Empty result and retry views for graceful network handling.

4. **Service Request & Booking Creation (User-Defined Feature 2)**
   - Detailed technician profile view with bio, rating break-down, response time, and job stats.
   - Interactive booking form with service selection dropdown.
   - DatePickerDialog and TimePickerDialog for scheduling.
   - Service address and problem description input with required field validation.
   - Dynamic estimated price calculation and submission confirmation.

5. **Bookings & Requests Management (User-Defined Feature 3)**
   - Real-time fetching of all active and past service requests from the REST API.
   - Formatted booking cards showing request ID, status badge (`PENDING`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`), technician info, date/time, address, and estimated price.
   - Loading indicator and empty state guidance.

6. **Technician Dashboard**
   - Daily job counter, pending request alerts, and monthly earnings tracker.
   - Today's interactive schedule view.

---

## Technologies & Libraries Used

- **Language:** Kotlin 1.9+
- **Min SDK:** 24 (Android 7.0) | **Target SDK:** 34 (Android 14)
- **Architecture Pattern:** MVVM / Repository Pattern with clean separation of concerns.
- **Networking:** Retrofit 2, OkHttp 4, Gson Converter, HttpLoggingInterceptor.
- **API Simulation:** Custom `MockInterceptor` implementing robust mock REST responses for local offline execution.
- **Concurrency:** Kotlin Coroutines (`lifecycleScope`, `runTest`).
- **UI Toolkit:** Android XML Views, Material Design 3 Components (`MaterialButton`, `TextInputLayout`, `ChipGroup`, `CardView`, `BottomNavigationView`).
- **Local Storage:** `SharedPreferences` + `Gson` via `SessionManager`.
- **Testing:** JUnit 4, Kotlinx Coroutines Test, Fake API & Session Test Harnesses.

---

## REST API Architecture

The app usesRetrofit with a custom OkHttp `MockInterceptor` that intercepts HTTP requests to `https://api.fixfinder.app/` and serves realistic HTTP response codes (200 OK, 400 Bad Request, 404 Not Found) with full JSON payloads.

### API Endpoints Supported
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticates email & password, returns JWT token & user object |
| `POST` | `/api/auth/register` | Registers a new customer or technician account |
| `GET` | `/api/categories` | Retrieves available service categories |
| `GET` | `/api/providers` | Queries service providers by category, name keyword, or location |
| `GET` | `/api/providers/{id}` | Retrieves full details for a specific service provider |
| `POST` | `/api/requests` | Creates and submits a new service request |
| `GET` | `/api/requests/user/{userId}` | Fetches service requests for a specific user |
| `PUT` | `/api/user/settings` | Updates user preference settings |

---

## Application Structure

```
com.example.fix_finder/
├── data/
│   ├── api/
│   │   ├── ApiClient.kt             # Retrofit Singleton Configuration
│   │   ├── FixFinderApiService.kt   # Retrofit API Interface
│   │   └── MockInterceptor.kt      # Mock REST API Interceptor
│   ├── local/
│   │   └── SessionManager.kt        # Encapsulated SharedPreferences session storage
│   ├── model/
│   │   ├── AuthModels.kt            # Login & Register request/response DTOs
│   │   ├── ServiceModels.kt         # Categories, Providers, Requests, Settings DTOs
│   │   └── User.kt                  # User data model
│   └── repository/
│       ├── AuthRepository.kt        # Authentication business logic
│       ├── ServiceRepository.kt     # Services, Providers & Requests repository
│       └── SettingsRepository.kt    # User settings repository
├── BookingsAdapter.kt               # RecyclerView adapter for service requests/bookings
├── ServiceProviderAdapter.kt        # RecyclerView adapter for service providers
├── MainActivity.kt                  # Single-Activity host container
├── LoginFragment.kt                 # Login screen logic & validation
├── RegisterFragment.kt              # Account registration screen logic
├── HomeFragment.kt                  # Customer home screen with category cards
├── DashboardFragment.kt             # Technician dashboard screen
├── SearchFragment.kt                # Service search & filtering screen
├── ServiceRequestFragment.kt        # Service request creation & booking screen
├── BookingsFragment.kt              # User bookings list screen
├── ProfileFragment.kt               # Technician profile details screen
├── SettingsFragment.kt              # User settings & logout screen
└── SplashFragment.kt                # Application launch splash screen
```

---

## How to Build & Run

### Prerequisites
- **Android Studio:** Jellyfish / Koala / Ladybug or newer.
- **JDK:** Java 17 (bundled with Android Studio).
- **Android Emulator / Device:** API Level 24 or higher.

### Gradle Commands
From Android Studio Terminal or system command line:

- **Build Debug APK:**
  ```bash
  ./gradlew app:assembleDebug
  ```

- **Run Unit Tests:**
  ```bash
  ./gradlew app:testDebugUnitTest
  ```

---

## Testing Approach

Automated unit tests cover key repository and business logic:

1. **`AuthRepositoryTest`**:
   - Validation of blank login fields.
   - Successful authentication and session saving.
   - Rejection of invalid credentials.
   - Email format validation during registration.
   - Short password error handling.

2. **`ServiceRepositoryTest`**:
   - Fetching categories via REST API.
   - Provider search query handling.
   - Blank description validation when submitting service requests.
   - Successful creation and parsing of service requests.

---

## Configuration & Environment

- No external API keys or secrets are required to run or test the application.
- The `MockInterceptor` ensures 100% offline functionality without needing a remote backend server online during testing or grading.

Video link: https://drive.google.com/file/d/1-eJcYkJDmEWqSa1-VXcMm_IENvTVmSfa/view?usp=drivesdk
