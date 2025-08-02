package hospital.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;

public class Components {
    
    public static Label createTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        label.setStyle("-fx-text-fill: #2c3e50;");
        label.setWrapText(true);
        label.setPrefWidth(Region.USE_COMPUTED_SIZE);
        return label;
    }

    public static Label createHeading(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 18));
        label.setStyle("-fx-text-fill: #34495e;");
        label.setWrapText(true);
        return label;
    }

    public static TextField createTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(280);
        tf.setPrefHeight(35);
        tf.setStyle("-fx-background-radius: 5; -fx-border-radius: 5;");
        return tf;
    }

    public static PasswordField createPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefWidth(280);
        pf.setPrefHeight(35);
        pf.setStyle("-fx-background-radius: 5; -fx-border-radius: 5;");
        return pf;
    }

    public static TextArea createTextArea(String prompt) {
        TextArea ta = new TextArea();
        ta.setPromptText(prompt);
        ta.setPrefWidth(280);
        ta.setPrefHeight(80);
        ta.setWrapText(true);
        ta.setStyle("-fx-background-radius: 5; -fx-border-radius: 5;");
        return ta;
    }

    public static Button createButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(150);
        btn.setPrefHeight(40);
        btn.setStyle(
            "-fx-background-color: #3498db; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 5; " +
            "-fx-border-radius: 5; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 12px;"
        );
        
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #2980b9; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 5; " +
            "-fx-border-radius: 5; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 12px;"
        ));
        
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #3498db; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 5; " +
            "-fx-border-radius: 5; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 12px;"
        ));
        
        return btn;
    }

    public static ComboBox<String> createComboBox(String prompt) {
        ComboBox<String> cb = new ComboBox<>();
        cb.setPromptText(prompt);
        cb.setPrefWidth(280);
        cb.setPrefHeight(35);
        cb.setStyle("-fx-background-radius: 5; -fx-border-radius: 5;");
        return cb;
    }
}