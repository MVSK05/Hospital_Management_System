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

public class AdminDashboardController {

    private Stage stage;

    public AdminDashboardController(Stage stage) {
        this.stage = stage;
    }

    public void showAdminView() {
        VBox root = LayoutHelper.createBaseLayout();
        root.setStyle("-fx-background-color: #ecf0f1;");
        root.getChildren().add(LayoutHelper.createSectionHeader("Admin Dashboard"));

        TextField searchField = new TextField();
        searchField.setPromptText("Search username or name...");
        searchField.setPrefWidth(250);

        ComboBox<String> roleFilter = new ComboBox<>();
        roleFilter.getItems().addAll("All", "Doctor", "Nurse", "Patient");
        roleFilter.setValue("All");
        roleFilter.setPrefWidth(120);

        HBox filterBox = new HBox(10, searchField, roleFilter);
        filterBox.setAlignment(Pos.CENTER);

        ScrollPane scrollPane = new ScrollPane();
        VBox userList = new VBox(10);
        userList.setAlignment(Pos.CENTER_LEFT);
        scrollPane.setContent(userList);
        scrollPane.setPrefHeight(300);
        scrollPane.setFitToWidth(true);

        Button refreshBtn = Components.createButton("Refresh");
        refreshBtn.setPrefWidth(120);
        refreshBtn.setOnAction(e -> loadUsers(userList, searchField.getText(), roleFilter.getValue()));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadUsers(userList, newVal, roleFilter.getValue()));
        roleFilter.setOnAction(e -> loadUsers(userList, searchField.getText(), roleFilter.getValue()));

        loadUsers(userList, "", "All");

        Button logoutBtn = Components.createButton("Logout");
        logoutBtn.setPrefWidth(120);
        logoutBtn.setOnAction(e -> LoginController.showLogin(stage));

        Button createUserBtn = Components.createButton("Create User");
        createUserBtn.setPrefWidth(120);
        createUserBtn.setOnAction(e -> SignupController.showSignup(stage, "admin", "admin"));

        HBox buttonBox = new HBox(10, refreshBtn, createUserBtn, logoutBtn);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(filterBox, scrollPane, buttonBox);

        Scene scene = new Scene(root, 700, 600);
        stage.setTitle("Admin Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    private void loadUsers(VBox container, String searchTerm, String roleFilter) {
        container.getChildren().clear();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT u.username, u.role, u.name, " +
                 "CASE " +
                 "  WHEN u.role = 'patient' THEN COALESCE(p.name, u.name, u.username) " +
                 "  WHEN u.role = 'admin' THEN COALESCE(u.name, 'Administrator') " +
                 "  WHEN u.role = 'doctor' THEN COALESCE('Dr. ' || u.name, 'Dr. ' || u.username) " +
                 "  WHEN u.role = 'nurse' THEN COALESCE(u.name, u.username) " +
                 "  ELSE COALESCE(u.name, u.username) " +
                 "END as display_name " +
                 "FROM users u " +
                 "LEFT JOIN patient_info p ON u.username = p.username AND u.role = 'patient'")) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                String role = rs.getString("role");
                String displayName = rs.getString("display_name");

                boolean matchesSearch = searchTerm.isEmpty() ||
                        username.toLowerCase().contains(searchTerm.toLowerCase()) ||
                        (displayName != null && displayName.toLowerCase().contains(searchTerm.toLowerCase()));
                boolean matchesRole = roleFilter.equals("All") || role.equalsIgnoreCase(roleFilter);

                if (matchesSearch && matchesRole) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPrefWidth(650);

                    Label label = LayoutHelper.createBoldLabel(displayName + " (" + username + " - " + role + ")");
                    label.setPrefWidth(400);

                    if (!role.equalsIgnoreCase("admin")) {
                        Button editBtn = Components.createButton("Edit");
                        editBtn.setPrefWidth(80);
                        editBtn.setOnAction(e -> EditUserController.showEditUser(stage, username, role, "admin"));

                        Button deleteBtn = Components.createButton("Delete");
                        deleteBtn.setPrefWidth(80);
                        deleteBtn.setOnAction(e -> {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Delete Confirmation");
                            alert.setHeaderText("Delete User: " + username);
                            alert.setContentText("Are you sure you want to delete this user?");
                            alert.showAndWait().ifPresent(response -> {
                                if (response == ButtonType.OK) {
                                    deleteUser(username, container, searchTerm, roleFilter);
                                }
                            });
                        });

                        row.getChildren().addAll(label, editBtn, deleteBtn);
                    } else {
                        row.getChildren().add(label);
                    }

                    container.getChildren().add(row);
                }
            }

            if (container.getChildren().isEmpty()) {
                container.getChildren().add(LayoutHelper.createBoldLabel("No users found."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Utils.showError("Error loading users.");
        }
    }

    private void deleteUser(String username, VBox container, String searchTerm, String roleFilter) {
        try (Connection conn = DBConnection.getConnection()) {
            
            PreparedStatement roleStmt = conn.prepareStatement("SELECT role FROM users WHERE username = ?");
            roleStmt.setString(1, username);
            ResultSet roleRs = roleStmt.executeQuery();

            if (roleRs.next()) {
                String role = roleRs.getString("role");

                if ("doctor".equalsIgnoreCase(role)) {
                    PreparedStatement checkPatients = conn.prepareStatement(
                        "SELECT COUNT(*) FROM doctor_nurse_assignment WHERE doctor_username = ?");
                    checkPatients.setString(1, username);
                    ResultSet rs = checkPatients.executeQuery();

                    if (rs.next() && rs.getInt(1) > 0) {
                        Utils.showError("Cannot delete doctor. Patients are still assigned.");
                        return;
                    }
                }
            }

            PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE username = ?");
            stmt.setString(1, username);
            stmt.executeUpdate();

            Utils.showInfo("User '" + username + "' deleted successfully.");
            loadUsers(container, searchTerm, roleFilter);

        } catch (Exception e) {
            e.printStackTrace();
            Utils.showError("Error deleting user: " + e.getMessage());
        }
    }

    public static void showAdminDashboard(Stage stage) {
        new AdminDashboardController(stage).showAdminView();
    }
}
