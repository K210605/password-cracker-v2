package com.khushi.passwordcracker;

import com.khushi.passwordcracker.utils.HashUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.io.IOException;

public class HashGeneratorController {

    @FXML
    private TextField passwordField;

    @FXML
    private ComboBox<String> algorithmBox;

    @FXML
    private TextField hashOutputField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button generateButton;

    @FXML
    private void initialize() {
        // SHA-256 is the only algorithm HashUtil currently implements.
        // Add more entries here (and to HashUtil) if that changes later.
        algorithmBox.getItems().setAll("SHA-256");
        algorithmBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleGenerate() {

        String password = passwordField.getText();

        if (password == null || password.isEmpty()) {
            showMessage("Please enter a password to hash.", false);
            return;
        }

        String algorithm = algorithmBox.getValue();

        if (!"SHA-256".equals(algorithm)) {
            showMessage("Unsupported algorithm: " + algorithm, false);
            return;
        }

        // Hashing happens locally via HashUtil (java.security.MessageDigest) -
        // the password is never sent anywhere.
        String hash = HashUtil.generateSHA256(password);
        hashOutputField.setText(hash);
        showMessage("Hash generated.", true);
    }

    @FXML
    private void handleCopy() {

        String hash = hashOutputField.getText();

        if (hash == null || hash.isEmpty()) {
            showMessage("Nothing to copy yet - generate a hash first.", false);
            return;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(hash);
        Clipboard.getSystemClipboard().setContent(content);

        showMessage("Hash copied to clipboard.", true);
    }

    @FXML
    private void handleUseAsTarget() {

        String hash = hashOutputField.getText();

        if (hash == null || hash.isEmpty()) {
            showMessage("Generate a hash first.", false);
            return;
        }

        AppSession.setPendingTargetHash(hash);

        try {
            PasswordCrackerGUI.showScene("/dashboard.fxml");
        } catch (IOException ex) {
            showMessage("Failed to open Attack Lab: " + ex.getMessage(), false);
        }
    }

    @FXML
    private void handleClear() {
        passwordField.clear();
        hashOutputField.clear();
        messageLabel.setText("");
    }

    @FXML
    private void handleBack() {
        try {
            PasswordCrackerGUI.showScene("/main-menu.fxml");
        } catch (IOException ex) {
            showMessage("Failed to open Dashboard: " + ex.getMessage(), false);
        }
    }

    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().setAll(success ? "status-success" : "status-fail");
    }
}
