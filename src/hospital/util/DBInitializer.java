package hospital.util;

import hospital.model.DBConnection;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

public class DBInitializer {
    public static void initializeDatabase() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON");

            String usersTable = "CREATE TABLE IF NOT EXISTS users (" +
                                "username TEXT PRIMARY KEY, " +
                                "password TEXT NOT NULL, " +
                                "role TEXT NOT NULL, " +
                                "email TEXT, " +
                                "name TEXT)";
            stmt.execute(usersTable);

            String patientInfoTable = "CREATE TABLE IF NOT EXISTS patient_info (" +
                                      "username TEXT PRIMARY KEY, " +
                                      "name TEXT NOT NULL, " +
                                      "phone TEXT NOT NULL, " +
                                      "email TEXT, " +
                                      "gender TEXT, " +
                                      "dob TEXT, " +
                                      "blood_group TEXT, " +
                                      "assigned_doctor TEXT, " +
                                      "assigned_nurse TEXT, " +
                                      "type TEXT, " +
                                      "FOREIGN KEY(username) REFERENCES users(username) ON DELETE CASCADE" +
                                      ")";
            stmt.execute(patientInfoTable);

            String medicalHistoryTable = "CREATE TABLE IF NOT EXISTS medical_history (" +
                                         "patient_username TEXT NOT NULL, " +
                                         "date TEXT NOT NULL, " +
                                         "notes TEXT, " +
                                         "PRIMARY KEY(patient_username, date), " +
                                         "FOREIGN KEY(patient_username) REFERENCES users(username) ON DELETE CASCADE" +
                                         ")";
            stmt.execute(medicalHistoryTable);

            String appointmentsTable = "CREATE TABLE IF NOT EXISTS appointments (" +
                                       "doctor TEXT NOT NULL, " +
                                       "patient_name TEXT NOT NULL, " +
                                       "appointment_date TEXT NOT NULL, " +
                                       "appointment_time TEXT NOT NULL, " +
                                       "PRIMARY KEY(doctor, patient_name, appointment_date, appointment_time))";
            stmt.execute(appointmentsTable);

            String doctorNurseTable = "CREATE TABLE IF NOT EXISTS doctor_nurse_assignment (" +
                                      "doctor_username TEXT NOT NULL, " +
                                      "nurse_username TEXT NOT NULL, " +
                                      "PRIMARY KEY (doctor_username, nurse_username), " +
                                      "FOREIGN KEY(doctor_username) REFERENCES users(username) ON DELETE CASCADE, " +
                                      "FOREIGN KEY(nurse_username) REFERENCES users(username) ON DELETE CASCADE" +
                                      ")";
            stmt.execute(doctorNurseTable);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_role ON users(role)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_doctor_nurse_assignment ON doctor_nurse_assignment(nurse_username)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_patient_doctor ON patient_info(assigned_doctor)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_appointment_doctor ON appointments(doctor)");

            System.out.println("Database tables and indexes initialized.");

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM users WHERE username = 'admin'")) {
                if (rs.next() && rs.getInt("count") == 0) {
                    stmt.executeUpdate("INSERT INTO users (username, password, role, email, name) " +
                                       "VALUES ('admin', 'admin123', 'admin', 'admin@hms.com', 'Administrator')");
                    System.out.println("Default admin created: admin / admin123");
                }
            }

        } catch (Exception e) {
            System.err.println("Database initialization failed:");
            e.printStackTrace();
        }
    }
}