package hospital;

import hospital.controller.LoginController;
import hospital.util.DBInitializer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        DBInitializer.initializeDatabase();
        LoginController controller = new LoginController(primaryStage);
        primaryStage.setTitle("Hospital Management");
        primaryStage.setScene(new Scene(controller.getLoginView(), 500, 400));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
