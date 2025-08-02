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
import java.time.LocalDate;

public class EditInfoController {
    private final String name;
    private String username; 
    private final Stage editStage;

    public EditInfoController(String name) {
        this.name = name;
        this.editStage = new Stage();
        loadUsernameFromName(); 
    }

    private void loadUsernameFromName() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT username FROM users WHERE name = ?")) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    this.username = rs.getString("username");
                    showEditInfoView();
                } else {
                    Utils.showError("No user found with the given name.");
                    editStage.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.showError("Failed to find user: " + e.getMessage());
            editStage.close();
        }
    }

    private void showEditInfoView() {
        editStage.setTitle("Edit My Information");

        VBox root = LayoutHelper.createBaseLayout();
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #ecf0f1;");

        Label title = LayoutHelper.createSectionHeader("Edit My Information");

        TextField nameField = Components.createTextField("Full Name");
        TextField usernameField = Components.createTextField("Username"); 
        usernameField.setText(username); 

        TextField emailField = Components.createTextField("Email");
        TextField phoneField = Components.createTextField("Phone");

        ComboBox<String> genderCombo = new ComboBox<>();
        genderCombo.getItems().addAll("Male", "Female", "Other");
        genderCombo.setPromptText("Select Gender");
        genderCombo.setPrefWidth(280);

        DatePicker dobPicker = new DatePicker();
        dobPicker.setPromptText("Select Date of Birth");
        dobPicker.setPrefWidth(280);

        ComboBox<String> bloodGroupCombo = new ComboBox<>();
        bloodGroupCombo.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        bloodGroupCombo.setPromptText("Select Blood Group");
        bloodGroupCombo.setPrefWidth(280);

        Button updateBtn = Components.createButton("Update Information");
        Button cancelBtn = Components.createButton("Cancel");
        updateBtn.setPrefWidth(200);
        cancelBtn.setPrefWidth(200);

        loadPatientData(nameField, emailField, phoneField, genderCombo, dobPicker, bloodGroupCombo);

        updateBtn.setOnAction(e -> updatePatientInfo(
                nameField, usernameField, emailField, phoneField,
                genderCombo, dobPicker, bloodGroupCombo
        ));

        cancelBtn.setOnAction(e -> editStage.close());

        root.getChildren().addAll(
                title,
                LayoutHelper.createBoldLabel("Full Name:"), nameField,
                LayoutHelper.createBoldLabel("Username:"), usernameField,
                LayoutHelper.createBoldLabel("Email:"), emailField,
                LayoutHelper.createBoldLabel("Phone:"), phoneField,
                LayoutHelper.createBoldLabel("Gender:"), genderCombo,
                LayoutHelper.createBoldLabel("Date of Birth:"), dobPicker,
                LayoutHelper.createBoldLabel("Blood Group:"), bloodGroupCombo,
                updateBtn, cancelBtn
        );

        Scene scene = new Scene(root, 500, 700);
        editStage.setScene(scene);
        editStage.show();
    }

    private void loadPatientData(TextField nameField, TextField emailField, TextField phoneField,
                                 ComboBox<String> genderCombo, DatePicker dobPicker,
                                 ComboBox<String> bloodGroupCombo) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT u.name, u.email, p.phone, p.gender, p.dob, p.blood_group " +
                    "FROM users u JOIN patient_info p ON u.username = p.username WHERE u.username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        nameField.setText(rs.getString("name"));
                        emailField.setText(rs.getString("email"));
                        phoneField.setText(rs.getString("phone"));
                        String gender = rs.getString("gender");
                        if (gender != null) genderCombo.setValue(gender);
                        String dob = rs.getString("dob");
                        if (dob != null && !dob.isEmpty()) {
                            try {
                                dobPicker.setValue(LocalDate.parse(dob));
                            } catch (Exception ignored) {}
                        }
                        String bloodGroup = rs.getString("blood_group");
                        if (bloodGroup != null) bloodGroupCombo.setValue(bloodGroup);
                    } else {
                        Utils.showError("Patient information incomplete or missing.");
                        editStage.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.showError("Error loading patient information: " + e.getMessage());
            editStage.close();
        }
    }

    private void updatePatientInfo(TextField nameField, TextField usernameField,
                               TextField emailField, TextField phoneField,
                               ComboBox<String> genderCombo, DatePicker dobPicker,
                               ComboBox<String> bloodGroupCombo) {

    String newName = nameField.getText() != null ? nameField.getText().trim() : "";
    String newUsername = usernameField.getText() != null ? usernameField.getText().trim() : "";
    String newEmail = emailField.getText() != null ? emailField.getText().trim() : "";
    String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
    String gender = genderCombo.getValue();
    LocalDate dob = dobPicker.getValue();
    String bloodGroup = bloodGroupCombo.getValue();

    if (newName.isEmpty() || newUsername.isEmpty() || newEmail.isEmpty() || phone.isEmpty()
            || gender == null || dob == null || bloodGroup == null) {
        Utils.showError("All fields are mandatory. Please fill them.");
        return;
    }

    if (!newEmail.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
        Utils.showError("Invalid email format.");
        return;
    }

    if (!phone.matches("\\d{10}")) {
        Utils.showError("Phone number must be exactly 10 digits.");
        return;
    }

    try (Connection conn = DBConnection.getConnection()) {
        conn.setAutoCommit(false);

        String userUpdateQuery = "UPDATE users SET username = ?, email = ?, name = ? WHERE username = ?";
        try (PreparedStatement userStmt = conn.prepareStatement(userUpdateQuery)) {
            userStmt.setString(1, newUsername);
            userStmt.setString(2, newEmail);
            userStmt.setString(3, newName);
            userStmt.setString(4, username);
            userStmt.executeUpdate();
        }

        String patientUpdateQuery = "UPDATE patient_info SET username = ?, name = ?, phone = ?, email = ?, gender = ?, dob = ?, blood_group = ? WHERE username = ?";
        try (PreparedStatement patientStmt = conn.prepareStatement(patientUpdateQuery)) {
            patientStmt.setString(1, newUsername);
            patientStmt.setString(2, newName);
            patientStmt.setString(3, phone);
            patientStmt.setString(4, newEmail);
            patientStmt.setString(5, gender);
            patientStmt.setString(6, dob.toString());
            patientStmt.setString(7, bloodGroup);
            patientStmt.setString(8, username);
            patientStmt.executeUpdate();
        }

        conn.commit();
        Utils.showInfo("Information updated successfully.");
        editStage.close();

    } catch (Exception ex) {
        ex.printStackTrace();
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        if (msg.contains("unique constraint failed") || msg.contains("duplicate")) {
            Utils.showError("Username or email already exists.");
        } else if (msg.contains("check constraint failed")) {
            Utils.showError("Invalid data format. Please recheck email or phone.");
        } else {
            Utils.showError("Failed to update info: " + ex.getMessage());
        }
    }
}

}

