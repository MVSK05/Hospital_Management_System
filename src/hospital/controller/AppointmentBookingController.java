package hospital.controller;

import hospital.model.DBConnection;
import hospital.util.Utils;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class AppointmentBookingController {

    public static void showAppointmentScreen(Stage stage, String patientUsername) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #eaf2f8;");

        Label title = new Label("Book Appointment");
        title.setStyle("-fx-font-size: 18px; -fx-text-fill: #154360;");

        TextField doctorField = new TextField();
        doctorField.setEditable(false);

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Select Appointment Date");

        Spinner<Integer> hourSpinner = new Spinner<>(1, 12, 10);
        hourSpinner.setEditable(true);

        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0);
        minuteSpinner.setEditable(true);

        Spinner<String> ampmSpinner = new Spinner<>();
        ampmSpinner.setValueFactory(new SpinnerValueFactory.ListSpinnerValueFactory<>(
                FXCollections.observableArrayList("AM", "PM")
        ));
        ampmSpinner.getValueFactory().setValue("AM");

        HBox timeBox = new HBox(5, hourSpinner, new Label(":"), minuteSpinner, ampmSpinner);
        timeBox.setAlignment(Pos.CENTER);

        Button bookButton = new Button("Book Appointment");
        Button backButton = new Button("Back");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT assigned_doctor FROM patient_info WHERE username = ?")) {

            stmt.setString(1, patientUsername);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                doctorField.setText(rs.getString("assigned_doctor"));
            } else {
                Utils.showError("Assigned doctor not found.");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            Utils.showError("Error loading assigned doctor.");
            return;
        }

        bookButton.setOnAction(e -> {
            LocalDate date = datePicker.getValue();
            int hour = hourSpinner.getValue();
            int minute = minuteSpinner.getValue();
            String ampm = ampmSpinner.getValue();
            String doctor = doctorField.getText().trim();

            if (date == null) {
                Utils.showError("Please select a date.");
                return;
            }

            String time = String.format("%02d:%02d %s", hour, minute, ampm);

            try (Connection conn = DBConnection.getConnection()) {
                String sql = "INSERT INTO appointments (doctor, patient_name, appointment_date, appointment_time) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, doctor);
                stmt.setString(2, patientUsername);
                stmt.setString(3, date.toString());
                stmt.setString(4, time);
                stmt.executeUpdate();

                Utils.showInfo("Appointment booked successfully.");
                PatientDashboardController.showPatientDashboard(stage, patientUsername);
            } catch (Exception ex) {
                ex.printStackTrace();
                Utils.showError("Error: " + ex.getMessage());
            }
        });

        backButton.setOnAction(e -> PatientDashboardController.showPatientDashboard(stage, patientUsername));

        root.getChildren().addAll(
                title,
                new Label("Assigned Doctor:"),
                doctorField,
                new Label("Select Date:"),
                datePicker,
                new Label("Select Time:"),
                timeBox,
                bookButton,
                backButton
        );

        stage.setScene(new Scene(root, 400, 450));
        stage.setTitle("Book Appointment");
        stage.show();
    }
}
