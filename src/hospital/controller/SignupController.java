package hospital.controller;

import hospital.model.DBConnection;
import hospital.ui.Components;
import hospital.ui.LayoutHelper;
import hospital.util.Utils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SignupController {

    public static void showSignup(Stage stage, String creatorRole, String creatorUsername) {
        VBox root = LayoutHelper.createBaseLayout();
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #ecf0f1;");

        Label title = LayoutHelper.createSectionHeader("Create New User");

        TextField nameField = Components.createTextField("Enter Full Name");
        TextField usernameField = Components.createTextField("Enter Username");
        TextField phoneField = Components.createTextField("Enter Phone Number (Password will be this)");

        ComboBox<String> roleBox = new ComboBox<>();
        ComboBox<String> doctorBox = new ComboBox<>();
        doctorBox.setPromptText("Assign to Doctor");
        doctorBox.setPrefWidth(280);
        doctorBox.setVisible(false);

        TextField patientTypeField = Components.createTextField("Enter Patient Type (e.g., General, Emergency)");
        TextArea historyField = Components.createTextArea("Enter Initial Medical History");
        patientTypeField.setVisible(false);
        historyField.setVisible(false);

        if ("admin".equalsIgnoreCase(creatorRole)) {
            roleBox.getItems().addAll("Doctor", "Nurse", "Patient");
            roleBox.setPromptText("Select Role");
            roleBox.setPrefWidth(280);

            roleBox.setOnAction(e -> {
                String selectedRole = roleBox.getValue();
                if ("Patient".equalsIgnoreCase(selectedRole)) {
                    doctorBox.setVisible(true);
                    patientTypeField.setVisible(true);
                    historyField.setVisible(true);
                    loadDoctors(doctorBox);
                } else {
                    doctorBox.setVisible(false);
                    patientTypeField.setVisible(false);
                    historyField.setVisible(false);
                }
            });
        } else if ("doctor".equalsIgnoreCase(creatorRole)) {
            roleBox.getItems().add("Patient");
            roleBox.setValue("Patient");
            roleBox.setDisable(true);
            doctorBox.setVisible(false);
            patientTypeField.setVisible(true);
            historyField.setVisible(true);
        }

        Button registerButton = Components.createButton("Register");
        Button backButton = Components.createButton("Back");
        Button resetDBButton = null;

        registerButton.setPrefWidth(150);
        registerButton.setPrefHeight(40);
        backButton.setPrefWidth(150);
        backButton.setPrefHeight(40);

        if ("admin".equalsIgnoreCase(creatorRole)) {
            resetDBButton = Components.createButton("Reset DB");
            resetDBButton.setPrefWidth(150);
            resetDBButton.setPrefHeight(40);
            resetDBButton.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Admin Verification");
                dialog.setHeaderText("Enter admin password to confirm reset");
                dialog.setContentText("Password:");

                dialog.showAndWait().ifPresent(password -> {
                    if ("admin123".equals(password)) {
                        resetToDefaultAdmin();
                        Utils.showInfo("Database reset successfully. Only admin account remains.");
                    } else {
                        Utils.showError("Invalid admin password.");
                    }
                });
            });
        }

        registerButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String username = usernameField.getText().trim();
            String phone = phoneField.getText().trim();
            String role = creatorRole.equals("doctor") ? "patient" : roleBox.getValue();
            String assignedDoctor = creatorRole.equals("doctor") ? creatorUsername : doctorBox.getValue();
            String patientType = patientTypeField.getText().trim();
            String history = historyField.getText().trim();

            if (username.isEmpty() || phone.isEmpty() || role == null || name.isEmpty()) {
                Utils.showError("All required fields must be filled.");
                return;
            }
                
            if (!phone.matches("\\d{10}")) {
                Utils.showError("Phone number must be exactly 10 digits.");
                return;
            }

            if ("patient".equalsIgnoreCase(role) && (assignedDoctor == null || assignedDoctor.isEmpty())) {
                Utils.showError("Doctor assignment required for patient.");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {

                String sql = "INSERT INTO users (username, password, role, name) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, phone);
                stmt.setString(3, role);
                stmt.setString(4, name);
                stmt.executeUpdate();

                if ("patient".equalsIgnoreCase(role)) {
                    String info = "INSERT INTO patient_info (username, name, phone, assigned_doctor, type) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement insertPatient = conn.prepareStatement(info);
                    insertPatient.setString(1, username);
                    insertPatient.setString(2, name);
                    insertPatient.setString(3, phone);
                    insertPatient.setString(4, assignedDoctor);
                    insertPatient.setString(5, patientType);
                    insertPatient.executeUpdate();

                    if (!history.isEmpty()) {
                        String hist = "INSERT INTO medical_history (patient_username, date, notes) VALUES (?, datetime('now'), ?)";
                        PreparedStatement mh = conn.prepareStatement(hist);
                        mh.setString(1, username);
                        mh.setString(2, history);
                        mh.executeUpdate();
                    }
                }

                Utils.showInfo("User registered successfully.");
                if ("admin".equalsIgnoreCase(creatorRole))
                    AdminDashboardController.showAdminDashboard(stage);
                else
                    DoctorDashboardController.showDoctorDashboard(stage, creatorUsername);

            } catch (Exception ex) {
                ex.printStackTrace();
                Utils.showError("Error: " + ex.getMessage());
            }
        });

        backButton.setOnAction(e -> {
            if ("admin".equalsIgnoreCase(creatorRole))
                AdminDashboardController.showAdminDashboard(stage);
            else
                DoctorDashboardController.showDoctorDashboard(stage, creatorUsername);
        });

        root.getChildren().addAll(
            title,
            LayoutHelper.createBoldLabel("Full Name:"), nameField,
            LayoutHelper.createBoldLabel("Username:"), usernameField,
            LayoutHelper.createBoldLabel("Phone Number (used as password):"), phoneField
        );

        if ("admin".equalsIgnoreCase(creatorRole)) {
            root.getChildren().addAll(
                LayoutHelper.createBoldLabel("Select Role:"), roleBox,
                LayoutHelper.createBoldLabel("Assign Doctor (for Patients only):"), doctorBox
            );
        }

        root.getChildren().addAll(
            LayoutHelper.createBoldLabel("Patient Type:"), patientTypeField,
            LayoutHelper.createBoldLabel("Initial Medical History:"), historyField,
            registerButton, backButton
        );

        if (resetDBButton != null) root.getChildren().add(resetDBButton);

        Scene scene = new Scene(root, 650, 700);
        stage.setTitle("Signup - " + creatorRole);
        stage.setScene(scene);
        stage.show();
    }

    private static void loadDoctors(ComboBox<String> box) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT username, name FROM users WHERE LOWER(role) = 'doctor'");
             ResultSet rs = stmt.executeQuery()) {

            box.getItems().clear();
            while (rs.next()) {

                String name = rs.getString("name");
                box.getItems().add(name);
            }

            if (box.getItems().isEmpty()) {
                box.setPromptText("No doctors found");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void resetToDefaultAdmin() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("DELETE FROM users WHERE username != 'admin'");
            stmt.executeUpdate("DELETE FROM patient_info");
            stmt.executeUpdate("DELETE FROM appointments");
            stmt.executeUpdate("DELETE FROM medical_history");
            stmt.executeUpdate("DELETE FROM doctor_nurse_assignment");

            ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE username = 'admin'");
            if (!rs.next()) {
                stmt.executeUpdate("INSERT INTO users (username, password, role, name) VALUES ('admin', 'admin123', 'admin', 'Administrator')");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}