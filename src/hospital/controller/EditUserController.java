package hospital.controller;

import hospital.model.DBConnection;
import hospital.ui.Components;
import hospital.ui.LayoutHelper;
import hospital.util.Utils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EditUserController {

    public static void showEditUser(Stage stage, String username, String role, String editorRole) {
        
        if ("patient".equalsIgnoreCase(role)) {
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT name FROM patient_info WHERE username = ?");
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String name = rs.getString("name");
                    new EditInfoController(name); 
                    return;
                } else {
                    Utils.showError("Patient name not found.");
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Utils.showError("Error retrieving patient name.");
                return;
            }
        }

        VBox root = LayoutHelper.createBaseLayout();
        root.setStyle("-fx-background-color: #ecf0f1;");

        String displayName = getDisplayName(username, role);
        String headerText = "Edit User: " + displayName;

        Label header = LayoutHelper.createSectionHeader(headerText);

        TextField nameField = Components.createTextField("Full Name");
        TextField userNameField = Components.createTextField("User Name");
        TextField phoneField = Components.createTextField("Phone Number");
        TextField emailField = Components.createTextField("Email");

        loadCurrentData(username, nameField, userNameField, phoneField, emailField);

        VBox formBox = new VBox(10);
        formBox.setAlignment(Pos.CENTER);
        formBox.getChildren().addAll(
            LayoutHelper.createBoldLabel("Full Name:"), nameField,
            LayoutHelper.createBoldLabel("User Name:"), userNameField,
            LayoutHelper.createBoldLabel("Phone:"), phoneField,
            LayoutHelper.createBoldLabel("Email:"), emailField
        );

        Button saveBtn = Components.createButton("Save Changes");
        Button backBtn = Components.createButton("Back");
        saveBtn.setPrefWidth(150);
        saveBtn.setPrefHeight(40);
        backBtn.setPrefWidth(150);
        backBtn.setPrefHeight(40);

        HBox buttonBox = new HBox(10, saveBtn, backBtn);
        buttonBox.setAlignment(Pos.CENTER);

        saveBtn.setOnAction(e -> {
            saveUserChanges(stage, username, role, editorRole, nameField, userNameField, phoneField, emailField);
        });

        backBtn.setOnAction(e -> {
            if ("admin".equalsIgnoreCase(editorRole)) {
                AdminDashboardController.showAdminDashboard(stage);
            } else if ("doctor".equalsIgnoreCase(editorRole)) {
                DoctorDashboardController.showDoctorDashboard(stage, getCurrentDoctorUsername(editorRole));
            }
        });

        root.getChildren().addAll(header, formBox, buttonBox);

        Scene scene = new Scene(root, 500, 500);
        stage.setTitle("Edit User");
        stage.setScene(scene);
        stage.show();
    }

    private static void loadCurrentData(String username, TextField nameField, TextField userNameField,
                                        TextField phoneField, TextField emailField) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement userStmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
            userStmt.setString(1, username);
            ResultSet userRs = userStmt.executeQuery();

            if (userRs.next()) {
                emailField.setText(userRs.getString("email") != null ? userRs.getString("email") : "");
                nameField.setText(userRs.getString("name") != null ? userRs.getString("name") : "");
                phoneField.setText(userRs.getString("password") != null ? userRs.getString("password") : "");
                userNameField.setText(userRs.getString("username") != null ? userRs.getString("username") : "");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.showError("Error loading user data.");
        }
    }

   private static void saveUserChanges(Stage stage, String oldUsername, String role, String editorRole,
           TextField nameField, TextField userNameField, TextField phoneField, TextField emailField) {
        String newUsername = userNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String name = nameField.getText().trim();

        if (name.isEmpty() || newUsername.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            Utils.showError("Please fill in all fields.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) {
            Utils.showError("Please enter a valid email address.");
            return;
        }

        if (!phone.matches("\\d{10}")) {
            Utils.showError("Please enter a valid 10-digit phone number.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            
            if (!newUsername.equals(oldUsername)) {
                PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?");
                checkStmt.setString(1, newUsername);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    Utils.showError("Username '" + newUsername + "' already exists. Please choose a different username.");
                    return;
                }
            }

            PreparedStatement userUpdate = conn.prepareStatement(
                "UPDATE users SET username = ?, email = ?, name = ?, password = ? WHERE username = ?");
            userUpdate.setString(1, newUsername);
            userUpdate.setString(2, email);
            userUpdate.setString(3, name);
            userUpdate.setString(4, phone);
            userUpdate.setString(5, oldUsername);
            userUpdate.executeUpdate();

            Utils.showInfo("User updated successfully!");

            if ("admin".equalsIgnoreCase(editorRole)) {
                AdminDashboardController.showAdminDashboard(stage);
            } else if ("doctor".equalsIgnoreCase(editorRole)) {
                DoctorDashboardController.showDoctorDashboard(stage, getCurrentDoctorUsername(editorRole));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Utils.showError("Error updating user: " + e.getMessage());
        }
    }
    private static String getCurrentDoctorUsername(String editorRole) {
        return editorRole;
    }

    private static String getDisplayName(String username, String role) {
        String name = username;
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT name FROM users WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                name = rs.getString("name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if ("doctor".equalsIgnoreCase(role) && name != null && !name.toLowerCase().startsWith("dr.")) {
            return "Dr. " + name;
        }
        return name;
    }
}
