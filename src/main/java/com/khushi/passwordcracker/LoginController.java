package com.khushi.passwordcracker;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController {

    // No existing login logic was found anywhere in the uploaded project,
    // so this is a new, minimal local check for this desktop toolkit.
    // Replace these with your own credential source if you need something
    // stronger than a hardcoded demo account.
    private static final String VALID_USERNAME = "admin";
    private static final String VALID_PASSWORD = "password123";

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    @FXML
    private void handleLogin() {

        String username = usernameField.getText() == null
                ? ""
                : usernameField.getText().trim();

        String password = passwordField.getText() == null
                ? ""
                : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        if (!VALID_USERNAME.equals(username) || !VALID_PASSWORD.equals(password)) {
            errorLabel.setText("Invalid username or password.");
            return;
        }

        errorLabel.setText("");

        try {
            PasswordCrackerGUI.showScene("/main-menu.fxml");
        } catch (IOException ex) {
            errorLabel.setText("Failed to load dashboard: " + ex.getMessage());
        }
    }
}
