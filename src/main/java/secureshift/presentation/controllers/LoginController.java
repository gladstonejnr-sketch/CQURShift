package secureshift.presentation.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import secureshift.UserSession;
import secureshift.data.UserRepository;
import java.util.Optional;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private Label versionLabel;

    private final UserRepository userRepo = new UserRepository();

    @FXML
    public void initialize() {
        if (versionLabel != null) versionLabel.setText("C-QURShift v1.0");
        passwordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) login(); });
        usernameField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) passwordField.requestFocus(); });
        Platform.runLater(() -> usernameField.requestFocus());
    }

    @FXML
    public void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) { showError("Please enter username and password."); return; }
        loginBtn.setDisable(true); loginBtn.setText("Signing in…");
        Optional<String> role = userRepo.authenticate(username, password);
        if (role.isPresent()) {
            UserSession.getInstance().login(username, role.get());
            openMainApp();
        } else {
            showError("Invalid username or password.");
            passwordField.clear(); loginBtn.setDisable(false); loginBtn.setText("Sign In");
            passwordField.requestFocus();
        }
    }

    private void openMainApp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/secureshift/presentation/views/main.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/secureshift/presentation/css/liquid-glass.css").toExternalForm());
            Stage mainStage = new Stage();
            mainStage.setTitle("C-QURShift  —  " + UserSession.getInstance().getUsername() + "  [" + UserSession.getInstance().getRole() + "]");
            mainStage.setScene(scene); mainStage.show();
            ((Stage) loginBtn.getScene().getWindow()).close();
        } catch (Exception e) {
            showError("Failed to load application: " + e.getMessage());
            loginBtn.setDisable(false); loginBtn.setText("Sign In"); e.printStackTrace();
        }
    }

    private void showError(String msg) {
        if (errorLabel != null) { errorLabel.setText(msg); errorLabel.setTextFill(Color.web("#ff4d6d")); errorLabel.setVisible(true); }
    }
}
