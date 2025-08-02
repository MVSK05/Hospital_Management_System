# Hospital Management System (JavaFX)

This is a **JavaFX-based Hospital Management System** designed for roles including **Admin**, **Doctor**, **Nurse**, and **Patient**. It manages user data, appointments, assignments, and medical records using a local **SQLite database**.

---

## Requirements

* Java 11 or above
* JavaFX SDK
* SQLite JDBC Driver

---

## Project Structure

```
HospitalManagementSystem/
├── src/
│   └── hospital/
│       ├── Main.java
│       ├── controller/
│       │   ├── LoginController.java
│       │   ├── SignupController.java
│       │   ├── AdminDashboardController.java
│       │   ├── DoctorDashboardController.java
│       │   ├── NurseDashboardController.java
│       │   ├── PatientDashboardController.java
│       │   ├── AppointmentBookingController.java
│       │   ├── EditInfoController.java
│       │   ├── EditUserController.java
│       │   └── MedicalHistoryViewController.java
│       ├── model/
│       │   ├── DBConnection.java
│       │   ├── User.java
│       │   ├── Appointment.java
│       │   └── MedicalHistory.java
│       ├── ui/
│       │   ├── ColorTheme.java
│       │   ├── Components.java
│       │   └── LayoutHelper.java
│       └── util/
│           ├── Utils.java
│           └── DBInitializer.java
└── hospital.db
```

---

## How to Run the Project

### Step 1: Compile All Java Files

```bash
javac -cp .;path/to/javafx-sdk/lib/* src/hospital/Main.java
```

> Replace `path/to/javafx-sdk/lib` with the actual path to your JavaFX SDK lib directory.

### Step 2: Run the Application

```bash
java -cp .;path/to/javafx-sdk/lib/* hospital.Main
```

If using **IntelliJ IDEA** or **Eclipse**, make sure:

* JavaFX SDK is added to project libraries.
* VM options include:

```
--module-path path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
```

---

## Functional Overview

### Roles:

* **Admin**: Create/Edit/Delete Users, Assign Roles
* **Doctor**: View Appointments, Assign Nurses, View/Edit Patients
* **Nurse**: View Assigned Doctor/Patients, View Appointments
* **Patient**: View Info, Book Appointments, Edit Profile

### Database:

* SQLite is used with `hospital.db` for persistence.
* Tables:

  * `users`, `patient_info`, `appointments`, `doctor_nurse_assignment`, `medical_history`

---

## Notes

* All passwords are stored as plaintext (for educational purposes only)
* Patient phone number is used as password during signup
* UI is written using JavaFX without FXML for easier code control

---

## Sample Admin Login

```text
Username: admin
Password: admin123
```

You can reset DB to only the default admin using the "Reset DB" button as admin.

---

## Done:

* Search filters for all dashboards
* Editable profile for patients
* Role-based navigation
* Medical history tracking

---

## 🏗️ In Progress / Suggested Improvements

* Encrypt passwords
* Pagination for large datasets
* Export reports to PDF/Excel
* Role-based permission enhancements

---
