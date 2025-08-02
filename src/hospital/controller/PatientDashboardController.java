package hospital.controller;

import hospital.model.DBConnection;
import hospital.ui.Components;
import hospital.ui.LayoutHelper;
import hospital.util.Utils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PatientDashboardController {

    private Stage stage;
    private String username;

    public PatientDashboardController(Stage stage, String username) {
        this.stage = stage;
        this.username = username;
    }

    public void showPatientView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #ecf0f1;");

        String patientName = getPatientName();
        Label title = Components.createTitle("Patient Dashboard - " + patientName);

        ScrollPane scrollPane = new ScrollPane();
        VBox contentBox = new VBox(15);
        contentBox.setAlignment(Pos.TOP_CENTER);

        VBox patientInfo = getPatientInfo();
        VBox appointments = getAppointments();
        VBox medicalHistory = getMedicalHistory();

        contentBox.getChildren().addAll(patientInfo, appointments, medicalHistory);
        scrollPane.setContent(contentBox);
        scrollPane.setPrefHeight(400);
        scrollPane.setFitToWidth(true);

        Button bookAppointment = Components.createButton("Book Appointment");
        bookAppointment.setPrefWidth(200);
        bookAppointment.setOnAction(e -> AppointmentBookingController.showAppointmentScreen(stage, username));

        Button editInfoBtn = Components.createButton("Edit My Info");
        editInfoBtn.setPrefWidth(200);
        
        editInfoBtn.setOnAction(e -> new EditInfoController(patientName));

        Button logoutBtn = Components.createButton("Logout");
        logoutBtn.setPrefWidth(200);
        logoutBtn.setOnAction(e -> LoginController.showLogin(stage));

        HBox buttonBox1 = new HBox(10, bookAppointment, editInfoBtn);
        HBox buttonBox2 = new HBox(10, logoutBtn);
        buttonBox1.setAlignment(Pos.CENTER);
        buttonBox2.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, scrollPane, buttonBox1, buttonBox2);

        Scene scene = new Scene(root, 700, 700);
        stage.setTitle("Patient Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    private String getPatientName() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM patient_info WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return username; 
    }

    private VBox getPatientInfo() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 15;");
        
        Label sectionTitle = LayoutHelper.createSectionHeader("Personal Information");
        box.getChildren().add(sectionTitle);

        String query = "SELECT * FROM patient_info WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                String doctorDisplayName = getDoctorDisplayName(rs.getString("assigned_doctor"));
                
                box.getChildren().addAll(
                        LayoutHelper.createBoldLabel("Name: " + rs.getString("name")),
                        LayoutHelper.createBoldLabel("Phone: " + rs.getString("phone")),
                        LayoutHelper.createBoldLabel("Email: " + (rs.getString("email") != null ? rs.getString("email") : "Not provided")),
                        LayoutHelper.createBoldLabel("Gender: " + (rs.getString("gender") != null ? rs.getString("gender") : "Not provided")),
                        LayoutHelper.createBoldLabel("DOB: " + (rs.getString("dob") != null ? rs.getString("dob") : "Not provided")),
                        LayoutHelper.createBoldLabel("Blood Group: " + (rs.getString("blood_group") != null ? rs.getString("blood_group") : "Not provided")),
                        LayoutHelper.createBoldLabel("Assigned Doctor: " + doctorDisplayName)
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            box.getChildren().add(new Label("Failed to load patient info."));
        }

        return box;
    }

    private String getDoctorDisplayName(String doctorUsername) {
        if (doctorUsername == null) return "Not assigned";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM users WHERE username = ? AND role = 'doctor'")) {
            stmt.setString(1, doctorUsername);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getString("name") != null) {
                return rs.getString("name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Dr. " + doctorUsername; 
    }

    private VBox getAppointments() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 15;");
        
        Label sectionTitle = LayoutHelper.createSectionHeader("My Appointments");
        box.getChildren().add(sectionTitle);

        String sql = "SELECT doctor, appointment_time FROM appointments WHERE patient_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            boolean hasAppointments = false;
            while (rs.next()) {
                hasAppointments = true;
                String doctor = rs.getString("doctor");
                String time = rs.getString("appointment_time");
                String doctorDisplayName = getDoctorDisplayName(doctor);
                box.getChildren().add(new Label("• With " + doctorDisplayName + " on " + time));
            }

            if (!hasAppointments) {
                box.getChildren().add(new Label("No appointments scheduled."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            box.getChildren().add(new Label("Failed to load appointments."));
        }

        return box;
    }

    private VBox getMedicalHistory() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 15;");
        
        Label sectionTitle = LayoutHelper.createSectionHeader("Medical History");
        box.getChildren().add(sectionTitle);

        String sql = "SELECT date, notes FROM medical_history WHERE patient_username = ? ORDER BY date DESC LIMIT 5";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            boolean hasHistory = false;
            while (rs.next()) {
                hasHistory = true;
                String date = rs.getString("date");
                String notes = rs.getString("notes");
                Label record = new Label("• " + date + ": " + notes);
                record.setWrapText(true);
                box.getChildren().add(record);
            }

            if (!hasHistory) {
                box.getChildren().add(new Label("No medical records found."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            box.getChildren().add(new Label("Failed to load medical history."));
        }

        return box;
    }

    public static void showPatientDashboard(Stage stage, String username) {
        new PatientDashboardController(stage, username).showPatientView();
    }
}