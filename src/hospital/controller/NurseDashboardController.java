package hospital.controller;

import hospital.model.DBConnection;
import hospital.ui.Components;
import hospital.ui.LayoutHelper;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class NurseDashboardController {

    private Stage stage;
    private String nurseUsername;

    public NurseDashboardController(Stage stage, String nurseUsername) {
        this.stage = stage;
        this.nurseUsername = nurseUsername;
    }

    public void showNurseView() {
        VBox root = LayoutHelper.createBaseLayout();
        root.setStyle("-fx-background-color: #ecf0f1;");

        String nurseDisplayName = getNurseDisplayName();
        Label header = LayoutHelper.createSectionHeader("Nurse Dashboard - " + nurseDisplayName);

        ScrollPane scrollPane = new ScrollPane();
        VBox contentBox = new VBox(15);
        contentBox.setAlignment(Pos.TOP_CENTER);

        VBox assignedDoctorInfo = getAssignedDoctorInfo();
        VBox appointmentDetails = getAppointmentDetails();
        VBox patientDetails = getPatientDetails();

        contentBox.getChildren().addAll(assignedDoctorInfo, appointmentDetails, patientDetails);
        scrollPane.setContent(contentBox);
        scrollPane.setPrefHeight(400);
        scrollPane.setFitToWidth(true);

        Button logoutBtn = Components.createButton("Logout");
        logoutBtn.setPrefWidth(150);
        logoutBtn.setOnAction(e -> LoginController.showLogin(stage));

        root.getChildren().addAll(header, scrollPane, logoutBtn);

        Scene scene = new Scene(root, 700, 600);
        stage.setScene(scene);
        stage.setTitle("Nurse Dashboard");
        stage.show();
    }

    private String getNurseDisplayName() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM users WHERE username = ? AND role = 'nurse'")) {
            stmt.setString(1, nurseUsername);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getString("name") != null) {
                return rs.getString("name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nurseUsername;
    }

    private VBox getAssignedDoctorInfo() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 15;");

        Label sectionTitle = LayoutHelper.createSectionHeader("Assigned Doctor Information");
        box.getChildren().add(sectionTitle);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT doctor_username FROM doctor_nurse_assignment WHERE nurse_username = ?")) {

            stmt.setString(1, nurseUsername);
            ResultSet rs = stmt.executeQuery();

            boolean hasAssignment = false;
            while (rs.next()) {
                hasAssignment = true;
                String assignedDoctor = rs.getString("doctor_username");
                if (assignedDoctor != null) {
                    String doctorDisplayName = getDoctorDisplayName(assignedDoctor);
                    box.getChildren().add(LayoutHelper.createBoldLabel("Working under: " + doctorDisplayName));
                }
            }

            if (!hasAssignment) {
                box.getChildren().add(LayoutHelper.createBoldLabel("No doctor assignment found."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            box.getChildren().add(LayoutHelper.createBoldLabel("Error loading assignment data."));
        }

        return box;
    }

    private String getDoctorDisplayName(String doctorUsername) {
        String name = "";
        if (doctorUsername == null) return "Not assigned";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM users WHERE username = ? AND role = 'Doctor'")) {
            stmt.setString(1, doctorUsername);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getString("name") != null) {
                name = rs.getString("name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Dr. " + name;
    }

    private VBox getAppointmentDetails() {
    VBox box = new VBox(10);
    box.setAlignment(Pos.CENTER_LEFT);
    box.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 15;");

    Label sectionTitle = LayoutHelper.createSectionHeader("Today's Appointments");
    box.getChildren().add(sectionTitle);

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(
             "SELECT a.patient_name, a.appointment_date, a.appointment_time, p.name " +
             "FROM appointments a " +
             "JOIN patient_info p ON a.patient_name = p.username " +
             "WHERE a.appointment_date = DATE('now') " +
             "AND a.doctor IN (SELECT u.name FROM doctor_nurse_assignment d " +
             "JOIN users u ON d.doctor_username = u.username " +
             "WHERE d.nurse_username = ?)")
    ) {
        stmt.setString(1, nurseUsername);
        ResultSet rs = stmt.executeQuery();

        boolean hasAppointments = false;
        while (rs.next()) {
            hasAppointments = true;
            String patientUsername = rs.getString("patient_name");
            String patientName = rs.getString("name");
            String date = rs.getString("appointment_date");
            String time = rs.getString("appointment_time");

            box.getChildren().add(new Label("• " + patientName + " (" + patientUsername + ") at " + date + " " + time));
        }

        if (!hasAppointments) {
            box.getChildren().add(new Label("No appointments for today."));
        }

    } catch (Exception e) {
        e.printStackTrace();
        box.getChildren().add(new Label("Error loading appointment data."));
    }

    return box;
}


    private VBox getPatientDetails() {
    VBox box = new VBox(10);
    box.setAlignment(Pos.CENTER_LEFT);
    box.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 15;");

    Label sectionTitle = LayoutHelper.createSectionHeader("Doctor's Patients");
    box.getChildren().add(sectionTitle);

    TextField searchField = new TextField();
    searchField.setPromptText("Search patient by name...");
    searchField.setPrefWidth(300);
    box.getChildren().add(searchField);

    VBox patientList = new VBox(10);
    patientList.setAlignment(Pos.CENTER_LEFT);
    box.getChildren().add(patientList);

    loadPatientsForNurse(null, patientList);

    searchField.textProperty().addListener((obs, oldVal, newVal) -> {
        loadPatientsForNurse(newVal.trim(), patientList);
    });

    return box;
}

    private void loadPatientsForNurse(String filter, VBox container) {
    container.getChildren().clear();

    String query = "SELECT p.username, p.name, p.phone, p.assigned_doctor " +
                   "FROM patient_info p " +
                   "WHERE p.assigned_doctor IN (" +
                       "SELECT u.name FROM doctor_nurse_assignment d " +
                       "JOIN users u ON d.doctor_username = u.username " +
                       "WHERE d.nurse_username = ?)";

    if (filter != null && !filter.isEmpty()) {
        query += " AND LOWER(p.name) LIKE ?";
    }

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {

        stmt.setString(1, nurseUsername);
        if (filter != null && !filter.isEmpty()) {
            stmt.setString(2, "%" + filter.toLowerCase() + "%");
        }

        ResultSet rs = stmt.executeQuery();
        boolean found = false;

        while (rs.next()) {
            found = true;
            String username = rs.getString("username");
            String name = rs.getString("name");
            String phone = rs.getString("phone");
            String doctorName = rs.getString("assigned_doctor");

            VBox patientBox = new VBox(5);
            patientBox.setStyle("-fx-border-color: #ecf0f1; -fx-border-width: 1; -fx-padding: 10;");

            Label header = LayoutHelper.createBoldLabel("● " + name + " (" + username + ")");
            Label phoneLabel = new Label("  Phone: " + phone);
            Label doctorLabel = new Label("  Under Dr. " + doctorName);

            Button appointmentChecker = Components.createButton("View Last Appointment");
            appointmentChecker.setOnAction(e -> showLastAppointment(username, name));

            patientBox.getChildren().addAll(header, phoneLabel, doctorLabel, appointmentChecker);
            container.getChildren().add(patientBox);
        }

        if (!found) {
            container.getChildren().add(new Label("No patients found."));
        }

    } catch (Exception e) {
        e.printStackTrace();
        container.getChildren().add(new Label("Error retrieving patient data."));
    }
}

    private void showLastAppointment(String patientUsername, String patientName) {
    Stage dialog = new Stage();
    dialog.setTitle("Latest Appointment - " + patientName);

    VBox layout = new VBox(15);
    layout.setAlignment(Pos.CENTER);
    layout.setPadding(new javafx.geometry.Insets(20));

    Label title = LayoutHelper.createBoldLabel("Latest Appointment for: " + patientName);

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(
             "SELECT * FROM appointments " +
             "WHERE patient_name = ? " +
             "ORDER BY appointment_date DESC, appointment_time DESC LIMIT 1"
         )) {

        stmt.setString(1, patientUsername);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            String doctor = rs.getString("doctor");
            String date = rs.getString("appointment_date");
            String time = rs.getString("appointment_time");

            layout.getChildren().addAll(
                new Label("Doctor: " + doctor),
                new Label("Date: " + date),
                new Label("Time: " + time)
            );
        } else {
            layout.getChildren().add(new Label("No appointments found."));
        }

    } catch (Exception e) {
        e.printStackTrace();
        layout.getChildren().add(new Label("Error loading appointment data."));
    }

    Button closeBtn = Components.createButton("Close");
    closeBtn.setOnAction(e -> dialog.close());

    layout.getChildren().add(closeBtn);

    Scene scene = new Scene(layout, 350, 250);
    dialog.setScene(scene);
    dialog.show();
}

    
    public static void showNurseDashboard(Stage stage, String nurseUsername) {
        new NurseDashboardController(stage, nurseUsername).showNurseView();
    }
}
