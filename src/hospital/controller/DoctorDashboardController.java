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

public class DoctorDashboardController {

    private Stage stage;
    private String username;

    public DoctorDashboardController(Stage stage, String username) {
        this.stage = stage;
        this.username = username;
    }

    public void showDoctorView() {
        VBox root = LayoutHelper.createBaseLayout();
        root.setStyle("-fx-background-color: #ecf0f1;");

        String doctorDisplayName = getDoctorDisplayName();
        Label header = LayoutHelper.createSectionHeader("Doctor Dashboard - " + doctorDisplayName);

        ScrollPane scrollPane = new ScrollPane();
        VBox contentBox = new VBox(15);
        contentBox.setAlignment(Pos.TOP_CENTER);

        VBox nurseSection = getNurseAssignmentSection();
        VBox appointmentBox = getAppointments();
        VBox patientBox = getPatients();

        contentBox.getChildren().addAll(nurseSection, appointmentBox, patientBox);
        scrollPane.setContent(contentBox);
        scrollPane.setPrefHeight(400);
        scrollPane.setFitToWidth(true);

        Button refreshBtn = Components.createButton("Refresh");
        refreshBtn.setPrefWidth(150);
        refreshBtn.setOnAction(e -> showDoctorView());

        Button addPatientBtn = Components.createButton("Add Patient");
        addPatientBtn.setPrefWidth(150);
        addPatientBtn.setOnAction(e -> SignupController.showSignup(stage, "doctor", username));

        Button logoutBtn = Components.createButton("Logout");
        logoutBtn.setPrefWidth(150);
        logoutBtn.setOnAction(e -> LoginController.showLogin(stage));

        HBox buttonBox = new HBox(10, refreshBtn, addPatientBtn, logoutBtn);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(header, scrollPane, buttonBox);

        Scene scene = new Scene(root, 800, 700);
        stage.setTitle("Doctor Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    private String getDoctorDisplayName() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM users WHERE username = ? AND LOWER(role) = 'doctor'")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getString("name") != null) {
                return rs.getString("name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Dr. " + username;
    }

    private VBox getNurseAssignmentSection() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 15;");

        Label sectionTitle = LayoutHelper.createSectionHeader("My Assigned Nurses");
        box.getChildren().add(sectionTitle);

        VBox assignedNurses = new VBox(5);
        Label assignedLabel = LayoutHelper.createBoldLabel("Currently Assigned Nurses:");
        assignedNurses.getChildren().add(assignedLabel);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT nurse_username FROM doctor_nurse_assignment WHERE doctor_username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            boolean hasNurses = false;
            while (rs.next()) {
                hasNurses = true;
                String nurseUsername = rs.getString("nurse_username");
                String nurseDisplayName = getNurseDisplayName(nurseUsername);

                HBox nurseRow = new HBox(10);
                nurseRow.setAlignment(Pos.CENTER_LEFT);
                Label nurseLabel = new Label("• " + nurseDisplayName + " (" + nurseUsername + ")");

                Button removeBtn = new Button("Remove");
                removeBtn.setPrefWidth(100);
                removeBtn.setStyle(
                    "-fx-background-color: #e74c3c; " +
                    "-fx-text-fill: white; " +
                    "-fx-background-radius: 5; " +
                    "-fx-border-radius: 5; " +
                    "-fx-font-weight: bold;"
                );
                
                removeBtn.setOnMouseEntered(e -> removeBtn.setStyle(
                    "-fx-background-color: #c0392b; " +
                    "-fx-text-fill: white; " +
                    "-fx-background-radius: 5; " +
                    "-fx-border-radius: 5; " +
                    "-fx-font-weight: bold;"
                ));
                
                removeBtn.setOnMouseExited(e -> removeBtn.setStyle(
                    "-fx-background-color: #e74c3c; " +
                    "-fx-text-fill: white; " +
                    "-fx-background-radius: 5; " +
                    "-fx-border-radius: 5; " +
                    "-fx-font-weight: bold;"
                ));
                
                removeBtn.setOnAction(e -> {
                    removeNurse(nurseUsername);
                    showDoctorView();
                });

                nurseRow.getChildren().addAll(nurseLabel, removeBtn);
                assignedNurses.getChildren().add(nurseRow);
            }

            if (!hasNurses) {
                assignedNurses.getChildren().add(new Label("No nurses currently assigned."));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        HBox nurseAssignBox = new HBox(10);
        nurseAssignBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> availableNurses = new ComboBox<>();
        availableNurses.setPromptText("Select nurse to assign");
        availableNurses.setPrefWidth(300);
        loadAvailableNurses(availableNurses);

        Button assignNurseBtn = Components.createButton("Assign Nurse");
        assignNurseBtn.setPrefWidth(120);
        assignNurseBtn.setOnAction(e -> {
            String selected = availableNurses.getValue();
            if (selected != null) {
                
                if (selected.startsWith("⚠️")) {
                    Utils.showError("This nurse is already assigned to another doctor and cannot be reassigned.");
                    return;
                }
                
                if (selected.contains("(")) {
                    String selectedUsername = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
                    assignNurseToDoctor(selectedUsername);
                    showDoctorView();
                } else {
                    Utils.showError("Please select a valid nurse.");
                }
            } else {
                Utils.showError("Please select a nurse to assign.");
            }
        });

        nurseAssignBox.getChildren().addAll(new Label("Assign new nurse:"), availableNurses, assignNurseBtn);
        box.getChildren().addAll(assignedNurses, nurseAssignBox);
        return box;
    }

    private void removeNurse(String nurseUsername) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM doctor_nurse_assignment WHERE nurse_username = ? AND doctor_username = ?")) {
            stmt.setString(1, nurseUsername);
            stmt.setString(2, username);
            stmt.executeUpdate();
            Utils.showInfo("Nurse removed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            Utils.showError("Failed to remove nurse.");
        }
    }

    private String getNurseDisplayName(String nurseUsername) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM users WHERE username = ? AND LOWER(role) = 'nurse'")) {
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

    private void loadAvailableNurses(ComboBox<String> nurseBox) {
        nurseBox.getItems().clear();
        try (Connection conn = DBConnection.getConnection()) {
            
            PreparedStatement currentStmt = conn.prepareStatement(
                "SELECT u.username, u.name FROM users u " +
                "WHERE LOWER(u.role) = 'nurse' AND u.username IN " +
                "(SELECT nurse_username FROM doctor_nurse_assignment WHERE doctor_username = ?)");
            currentStmt.setString(1, username);
            ResultSet currentRs = currentStmt.executeQuery();
            while (currentRs.next()) {
                String uname = currentRs.getString("username");
                String name = currentRs.getString("name");
                String displayName = name != null ? name : uname;
                nurseBox.getItems().add(displayName + " (" + uname + ")");
            }

            
            PreparedStatement conflictStmt = conn.prepareStatement(
                "SELECT u.username, u.name, d.doctor_username FROM users u " +
                "JOIN doctor_nurse_assignment d ON u.username = d.nurse_username " +
                "WHERE LOWER(u.role) = 'nurse' AND d.doctor_username != ?");
            conflictStmt.setString(1, username);
            ResultSet conflictRs = conflictStmt.executeQuery();
            while (conflictRs.next()) {
                String uname = conflictRs.getString("username");
                String name = conflictRs.getString("name");
                String assignedDoctor = conflictRs.getString("doctor_username");
                String displayName = name != null ? name : uname;
                nurseBox.getItems().add("⚠️ " + displayName + " (" + uname + ") [Assigned to " + assignedDoctor + "]");
            }
            
            PreparedStatement unassignedStmt = conn.prepareStatement(
                "SELECT u.username, u.name FROM users u " +
                "WHERE LOWER(u.role) = 'nurse' AND u.username NOT IN " +
                "(SELECT nurse_username FROM doctor_nurse_assignment)");
            ResultSet unassignedRs = unassignedStmt.executeQuery();
            while (unassignedRs.next()) {
                String uname = unassignedRs.getString("username");
                String name = unassignedRs.getString("name");
                String displayName = name != null ? name : uname;
                nurseBox.getItems().add(displayName + " (" + uname + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void assignNurseToDoctor(String nurseUsername) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM doctor_nurse_assignment WHERE nurse_username = ?");
            deleteStmt.setString(1, nurseUsername);
            deleteStmt.executeUpdate();

            PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO doctor_nurse_assignment (doctor_username, nurse_username) VALUES (?, ?)");
            insertStmt.setString(1, username);
            insertStmt.setString(2, nurseUsername);
            insertStmt.executeUpdate();

            String nurseDisplayName = getNurseDisplayName(nurseUsername);
            Utils.showInfo("Nurse " + nurseDisplayName + " assigned successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            Utils.showError("Failed to assign nurse.");
        }
    }

    private VBox getAppointments() {
    VBox box = new VBox(10);
    box.setAlignment(Pos.CENTER_LEFT);
    box.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 15;");

    Label sectionTitle = LayoutHelper.createSectionHeader("Today's Appointments");
    box.getChildren().add(sectionTitle);

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(
             "SELECT * FROM appointments WHERE doctor = ? AND appointment_date <= DATE('now')"
         )) {
            stmt.setString(1, getDoctorDisplayName());
            ResultSet rs = stmt.executeQuery();

            boolean hasAppointments = false;
            while (rs.next()) {
                hasAppointments = true;
                String patient = rs.getString("patient_name");
                String date = rs.getString("appointment_date");
                String time = rs.getString("appointment_time");

                String patientName = getPatientDisplayName(patient);
                Label label = LayoutHelper.createBoldLabel("● " + patientName + " at " + date + " " + time);
                box.getChildren().add(label);
            }

            if (!hasAppointments) {
                box.getChildren().add(new Label("No appointments scheduled for today."));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return box;
    }
    
    private VBox getPatients() {
    VBox box = new VBox(15);
    box.setAlignment(Pos.CENTER_LEFT);
    box.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 15;");

    Label sectionTitle = LayoutHelper.createSectionHeader("My Patients");
    box.getChildren().add(sectionTitle);

    TextField searchField = new TextField();
    searchField.setPromptText("Search patient by name");
    searchField.setPrefWidth(300);
    box.getChildren().add(searchField);

    VBox patientList = new VBox(10);
    patientList.setAlignment(Pos.CENTER_LEFT);
    box.getChildren().add(patientList);

    loadPatients(null, patientList);

    searchField.textProperty().addListener((obs, oldVal, newVal) -> {
        loadPatients(newVal.trim(), patientList);
    });

    return box;
}

    private void showChangeDoctorDialog(String patientUsername, String patientName) {
        Stage dialog = new Stage();
        dialog.setTitle("Change Doctor for " + patientName);

        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new javafx.geometry.Insets(20));

        Label instruction = new Label("Select new doctor for " + patientName + ":");
        ComboBox<String> doctorBox = new ComboBox<>();
        doctorBox.setPrefWidth(250);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT username, name FROM users WHERE LOWER(role) = 'doctor'");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String doctorUsername = rs.getString("username");
                String doctorName = rs.getString("name");
                String displayText = doctorName != null ? doctorName + " (" + doctorUsername + ")" : "Dr. " + doctorUsername;
                doctorBox.getItems().add("Dr. "+doctorName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Button saveBtn = Components.createButton("Save");
        Button cancelBtn = Components.createButton("Cancel");

        HBox buttonBox = new HBox(10, saveBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);

        saveBtn.setOnAction(e -> {
            String selectedDoctor = doctorBox.getValue();
            if (selectedDoctor != null && !selectedDoctor.isEmpty()) {
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("UPDATE patient_info SET assigned_doctor = ? WHERE username = ?")) {
                    stmt.setString(1, selectedDoctor);
                    stmt.setString(2, patientUsername);
                    stmt.executeUpdate();

                    String doctorDisplayName = getDoctorDisplayName(selectedDoctor);
                    Utils.showInfo("Patient " + patientName + " assigned to " + doctorDisplayName + " successfully!");
                    dialog.close();
                    showDoctorView();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Utils.showError("Failed to change doctor.");
                }
            } else {
                Utils.showError("Please select a doctor.");
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());

        layout.getChildren().addAll(instruction, doctorBox, buttonBox);
        dialog.setScene(new Scene(layout, 400, 200));
        dialog.show();
    }

    private String getDoctorDisplayName(String doctorUsername) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM users WHERE username = ? AND LOWER(role) = 'doctor'")) {
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

    private String getPatientDisplayName(String username) {
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
    
    private void loadPatients(String filter, VBox container) {
    container.getChildren().clear();

    String baseQuery = "SELECT * FROM patient_info WHERE (assigned_doctor = ? OR assigned_doctor = ?)";
    boolean hasFilter = filter != null && !filter.isEmpty();

    if (hasFilter) {
        baseQuery += " AND LOWER(name) LIKE ?";
    }

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(baseQuery)) {

        stmt.setString(1, username);
        stmt.setString(2, getDoctorDisplayName());

        if (hasFilter) {
            stmt.setString(3, "%" + filter.toLowerCase() + "%");
        }

        ResultSet rs = stmt.executeQuery();
        boolean found = false;

        while (rs.next()) {
            found = true;
            String patientUsername = rs.getString("username");
            String name = rs.getString("name");

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label patientLabel = new Label("● " + name + " (" + patientUsername + ")");
            patientLabel.setPrefWidth(200);

            Button editBtn = Components.createButton("Edit");
            editBtn.setPrefWidth(80);
            editBtn.setOnAction(e -> new EditInfoController(name));

            Button changeDoctorBtn = Components.createButton("Change Doctor");
            changeDoctorBtn.setPrefWidth(120);
            changeDoctorBtn.setOnAction(e -> showChangeDoctorDialog(patientUsername, name));

            row.getChildren().addAll(patientLabel, editBtn, changeDoctorBtn);
            container.getChildren().add(row);
        }

        if (!found) {
            container.getChildren().add(new Label("No patients found."));
        }

    } catch (Exception e) {
        e.printStackTrace();
        container.getChildren().add(new Label("Error loading patients."));
    }
}


    public static void showDoctorDashboard(Stage stage, String username) {
        new DoctorDashboardController(stage, username).showDoctorView();
    }
}