package com.khushi.passwordcracker;

import javafx.fxml.FXML;

import java.io.IOException;

public class MainMenuController {

    @FXML
    private void handleOpenHashGenerator() throws IOException {
        PasswordCrackerGUI.showScene("/hash-generator.fxml");
    }

    @FXML
    private void handleOpenAttackLab() throws IOException {
        PasswordCrackerGUI.showScene("/dashboard.fxml");
    }
}
