package hospital.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;

public class LayoutHelper {
    
    public static VBox createBaseLayout() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPrefWidth(600);
        layout.setMinWidth(500);
        return layout;
    }
    
    public static Label createSectionHeader(String text) {
        Label header = new Label(text);
        header.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        header.setStyle("-fx-text-fill: #2c3e50; -fx-background-color: #ecf0f1; -fx-padding: 10;");
        header.setAlignment(Pos.CENTER);
        header.setPrefWidth(Region.USE_COMPUTED_SIZE);
        return header;
    }
    
    public static Label createBoldLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
        label.setStyle("-fx-text-fill: #34495e;");
        label.setWrapText(true);
        return label;
    }
    
    public static HBox createButtonRow(Button... buttons) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);
        for (Button button : buttons) {
            button.setPrefWidth(120);
            button.setPrefHeight(35);
        }
        row.getChildren().addAll(buttons);
        return row;
    }
    
    public static VBox createFormSection(String title, Control... controls) {
        VBox section = new VBox(8);
        section.setAlignment(Pos.CENTER_LEFT);
        section.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 15;");
        
        if (title != null && !title.isEmpty()) {
            Label sectionTitle = createSectionHeader(title);
            section.getChildren().add(sectionTitle);
        }
        
        for (Control control : controls) {
            if (control instanceof TextField) {
                ((TextField) control).setPrefWidth(300);
            } else if (control instanceof ComboBox) {
                ((ComboBox<?>) control).setPrefWidth(300);
            } else if (control instanceof TextArea) {
                ((TextArea) control).setPrefWidth(300);
                ((TextArea) control).setPrefHeight(100);
            }
            section.getChildren().add(control);
        }
        
        return section;
    }
}