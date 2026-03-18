package secureshift;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import secureshift.data.UserRepository;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        new UserRepository().initialise();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/secureshift/presentation/views/login.fxml"));
        if (loader.getLocation() == null) throw new RuntimeException("❌ Cannot find login.fxml");
        Scene scene = new Scene(loader.load(), 480, 560);
        scene.getStylesheets().add(getClass().getResource("/secureshift/presentation/css/liquid-glass.css").toExternalForm());
        stage.setTitle("C-QURShift — Sign In");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
