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

public class MedicalHistoryViewController {

    private Stage stage;
    private String patientUsername;
    private String currentUserRole;

    public MedicalHistoryViewController(Stage stage, String patientUsername, String currentUserRole) {
        this.stage = stage;
        this.patientUsername = patientUsername;
        this.currentUserRole = currentUserRole;
    }

    public void showHistoryView() {
        VBox root = LayoutHelper.createBaseLayout();
        root.getChildren().add(LayoutHelper.createSectionHeader("Medical History"));

        VBox historyBox = new VBox(10);
        historyBox.setAlignment(Pos.CENTER_LEFT);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT date, notes FROM medical_history WHERE patient_username = ? ORDER BY date DESC")) {

            stmt.setString(1, patientUsername);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String date = rs.getString("date");
                String notes = rs.getString("notes");
                Label record = new Label("• " + date + ": " + notes);
                record.setWrapText(true);
                historyBox.getChildren().add(record);
            }

            if (historyBox.getChildren().isEmpty()) {
                historyBox.getChildren().add(LayoutHelper.createBoldLabel("No medical records found."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Utils.showError("Failed to load medical history.");
        }

        root.getChildren().add(historyBox);

        if ("doctor".equalsIgnoreCase(currentUserRole)) {
            
            VBox entryBox = new VBox(10);
            entryBox.setAlignment(Pos.CENTER);
            TextField dateField = new TextField();
            dateField.setPromptText("Enter Date (YYYY-MM-DD)");
            TextArea notesArea = new TextArea();
            notesArea.setPromptText("Enter Medical Notes");
            notesArea.setPrefRowCount(4);
            Button addBtn = Components.createButton("Add Record");

            addBtn.setOnAction(e -> {
                String date = dateField.getText();
                String notes = notesArea.getText();
                if (date.isEmpty() || notes.isEmpty()) {
                    Utils.showError("Please enter both date and notes.");
                    return;
                }
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("INSERT INTO medical_history (patient_username, date, notes) VALUES (?, ?, ?)")) {
                    stmt.setString(1, patientUsername);
                    stmt.setString(2, date);
                    stmt.setString(3, notes);
                    stmt.executeUpdate();
                    Utils.showInfo("Medical record added successfully.");
                    showHistoryView();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Utils.showError("Error saving medical record.");
                }
            });

            entryBox.getChildren().addAll(
                LayoutHelper.createBoldLabel("Add New Record"),
                dateField,
                notesArea,
                addBtn
            );

            root.getChildren().add(entryBox);
        }

        Button backBtn = Components.createButton("Back");
        backBtn.setOnAction(e -> new PatientDashboardController(stage, patientUsername).showPatientView());

        root.getChildren().add(backBtn);

        Scene scene = new Scene(root, 600, 600);
        stage.setScene(scene);
        stage.setTitle("Medical History");
        stage.show();
    }
} 
