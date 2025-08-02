package hospital.controller;

import hospital.model.DBConnection;
import hospital.ui.ColorTheme;
import hospital.ui.Components;
import hospital.util.Utils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {
    private Stage stage;

    public LoginController(Stage stage) {
        this.stage = stage;
    }

    public Parent getLoginView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #ecf0f1;");

        Label title = Components.createTitle("Hospital Management System - Login");
        TextField usernameField = Components.createTextField("Username");
        PasswordField passwordField = Components.createPasswordField("Password");
        Button loginButton = Components.createButton("Login");

        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                Utils.showError("Please enter both username and password.");
                return;
            }

            handleLogin(username, password);
        });

        root.getChildren().addAll(title, usernameField, passwordField, loginButton);
        return root;
    }

    private void handleLogin(String username, String password) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT role FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username.trim());
            stmt.setString(2, password.trim());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role").toLowerCase();
                switch (role) {
                    case "admin":
                        new AdminDashboardController(stage).showAdminView();
                        break;
                    case "doctor":
                        new DoctorDashboardController(stage, username).showDoctorView();
                        break;
                    case "nurse":
                        new NurseDashboardController(stage, username).showNurseView();
                        break;
                    case "patient":
                        new PatientDashboardController(stage, username).showPatientView();
                        break;
                    default:
                        Utils.showError("Unknown user role: " + role);
                        return;
                }
                stage.setTitle(capitalize(role) + " Dashboard");
            } else {
                Utils.showError("Invalid username or password.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.showError("Login error: " + e.getMessage());
        }
    }

    private String capitalize(String role) {
        if (role == null || role.isEmpty()) return role;
        return role.substring(0, 1).toUpperCase() + role.substring(1);
    }

    public static void showLogin(Stage primaryStage) {
        LoginController controller = new LoginController(primaryStage);
        Scene scene = new Scene(controller.getLoginView(), 500, 400);
        primaryStage.setTitle("Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
