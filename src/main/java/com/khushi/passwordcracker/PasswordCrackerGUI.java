package com.khushi.passwordcracker;

import com.khushi.passwordcracker.algorithms.BruteForceCracker;
import com.khushi.passwordcracker.algorithms.DictionaryAttackCracker;
import com.khushi.passwordcracker.algorithms.HybridAttackCracker;
import com.khushi.passwordcracker.algorithms.MaskAttackCracker;
import com.khushi.passwordcracker.algorithms.MarkovChainCracker;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;

public class PasswordCrackerGUI extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {

        primaryStage = stage;

        stage.setTitle("Password Cracker Toolkit v2.0");

        // App now starts on the login screen, not directly on the
        // Attack Lab - see AppSession / showScene for navigation between
        // Login -> Dashboard -> Generate Hash / Attack Lab.
        showScene("/login.fxml");

        stage.show();
    }

    /**
     * Swaps the root of the primary stage's scene to the given FXML file,
     * re-applying the shared stylesheet. Used by every controller to move
     * between Login, Dashboard, Generate Hash, and the Attack Lab.
     */
    public static void showScene(String fxmlPath) throws IOException {

        FXMLLoader loader =
                new FXMLLoader(PasswordCrackerGUI.class.getResource(fxmlPath));

        Scene scene = new Scene(loader.load(), 1200, 720);

        scene.getStylesheets().add(
                PasswordCrackerGUI.class.getResource("/style.css").toExternalForm()
        );

        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }


    // =========================================================
    // CONTROLLER
    // =========================================================

    public static class Controller {

        @FXML
        private TextField targetHashField;

        @FXML
        private ComboBox<String> algorithmBox;

        @FXML
        private TextField maxLengthField;

        @FXML
        private TextField maskField;

        @FXML
        private ProgressBar progressBar;

        @FXML
        private Label statusLabel;

        @FXML
        private Label attemptsLabel;

        @FXML
        private Label executionTimeLabel;

        @FXML
        private Label passwordLabel;

        @FXML
        private Label techniquesLabel;

        @FXML
        private TextArea outputArea;

        @FXML
        private Button startButton;

        @FXML
        private Button stopButton;

        @FXML
        private Button clearButton;

        private Thread worker;

        private volatile boolean stopRequested = false;

        // Checked cooperatively inside the algorithm loops (see the
        // algorithms/*Cracker cancellable overloads), so Stop actually
        // stops the search instead of just detaching the UI from it.
        private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

        // 64 hex characters = a SHA-256 digest.
        private static final Pattern SHA256_HEX =
                Pattern.compile("^[0-9a-fA-F]{64}$");


        // =====================================================
        // INITIALIZE
        // =====================================================

        @FXML
        private void initialize() {

            algorithmBox.getItems().setAll(
                    "Brute Force",
                    "Dictionary",
                    "Hybrid",
                    "Mask",
                    "Markov"
            );

            algorithmBox.getSelectionModel().selectFirst();

            progressBar.setProgress(0);

            maxLengthField.setText("4");

            maskField.setText("?l?l?l");

            resetSummary();

            // If the user clicked "Use as Target Hash" in the Hash
            // Generator, pre-fill it here.
            String pendingHash = AppSession.consumePendingTargetHash();

            if (pendingHash != null && !pendingHash.isEmpty()) {
                targetHashField.setText(pendingHash);
            }
        }


        // =====================================================
        // START ATTACK
        // =====================================================

        @FXML
        private void handleStartAttack() {

            String targetHash = targetHashField.getText().trim();
            String selectedAlgorithm = algorithmBox.getValue();

            if (targetHash.isEmpty()) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Missing Target Hash",
                        "Please enter a target hash, or generate one in "
                                + "the Hash Generator and click "
                                + "\"Use as Target Hash\"."
                );

                updateStatus("Please enter a target hash.", false);
                return;
            }

            if (!SHA256_HEX.matcher(targetHash).matches()) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Invalid Target Hash",
                        "The target hash must be a 64-character "
                                + "hexadecimal SHA-256 digest.\n\n"
                                + "You entered " + targetHash.length()
                                + " character(s)."
                );

                updateStatus("Invalid target hash format.", false);
                return;
            }

            if (selectedAlgorithm == null ||
                    selectedAlgorithm.isEmpty()) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "No Algorithm Selected",
                        "Please select an attack algorithm."
                );

                updateStatus("Please select an attack algorithm.", false);
                return;
            }

            // Algorithms that read Maximum Length need it validated up
            // front, rather than silently falling back to a default deep
            // inside runAttack().
            boolean usesMaxLength =
                    "Brute Force".equals(selectedAlgorithm)
                            || "Hybrid".equals(selectedAlgorithm)
                            || "Markov".equals(selectedAlgorithm);

            if (usesMaxLength) {

                String rawMaxLength = maxLengthField.getText().trim();

                try {

                    int parsed = Integer.parseInt(rawMaxLength);

                    if (parsed < 1) {
                        throw new NumberFormatException("must be >= 1");
                    }

                } catch (NumberFormatException ex) {

                    showAlert(
                            Alert.AlertType.WARNING,
                            "Invalid Maximum Length",
                            "Maximum Length must be a whole number of 1 "
                                    + "or greater."
                    );

                    updateStatus("Invalid maximum length.", false);
                    return;
                }
            }


            // Disable button while attack is running
            startButton.setDisable(true);
            stopButton.setDisable(false);
            clearButton.setDisable(true);
            stopRequested = false;
            cancelFlag.set(false);

            progressBar.setProgress(
                    ProgressBar.INDETERMINATE_PROGRESS
            );

            resetSummary();

            updateStatus(
                    "Running " + selectedAlgorithm + "...",
                    true
            );


            // =================================================
            // BACKGROUND THREAD
            // =================================================

            worker = new Thread(() -> {

                try {

                    long startTime = System.nanoTime();

                    AttackResult result =
                            runAttack(
                                    selectedAlgorithm,
                                    targetHash
                            );

                    long elapsedMs =
                            (System.nanoTime() - startTime)
                                    / 1_000_000;


                    // =================================================
                    // ALL UI UPDATES MUST BE INSIDE runLater
                    // =================================================

                    Platform.runLater(() -> {

                        progressBar.setProgress(1.0);


                        if (stopRequested) {

                            statusLabel.setText(
                                    "Stopped by user"
                            );

                            statusLabel.getStyleClass().setAll(
                                    "status-fail"
                            );

                        } else if (result.isSuccess()) {

                            statusLabel.setText(
                                    "Completed successfully"
                            );

                            statusLabel.getStyleClass().setAll(
                                    "status-success"
                            );

                        } else {

                            statusLabel.setText(
                                    "No password found"
                            );

                            statusLabel.getStyleClass().setAll(
                                    "status-fail"
                            );
                        }


                        attemptsLabel.setText(
                                "Attempts : " +
                                        result.getAttempts()
                        );


                        executionTimeLabel.setText(
                                "Execution Time : " +
                                        elapsedMs +
                                        " ms"
                        );


                        passwordLabel.setText(
                                "Password : " +
                                        result.getPassword()
                        );


                        // ⭐ TECHNIQUES USED
                        techniquesLabel.setText(
                                result.getDetails()
                        );


                        // LIVE CONSOLE
                        outputArea.setText(
                                result.getDetails()
                        );


                        startButton.setDisable(false);
                        stopButton.setDisable(true);
                        clearButton.setDisable(false);
                    });


                } catch (Exception ex) {

                    ex.printStackTrace();

                    Platform.runLater(() -> {

                        progressBar.setProgress(0.0);

                        statusLabel.setText(
                                "Attack failed"
                        );

                        statusLabel.getStyleClass().setAll(
                                "status-fail"
                        );

                        attemptsLabel.setText(
                                "Attempts : 0"
                        );

                        executionTimeLabel.setText(
                                "Execution Time : 0 ms"
                        );

                        passwordLabel.setText(
                                "Password : Not Found"
                        );

                        techniquesLabel.setText(
                                "Attack failed."
                        );

                        outputArea.setText(
                                "Error: " + ex.getMessage()
                        );

                        startButton.setDisable(false);
                        stopButton.setDisable(true);
                        clearButton.setDisable(false);
                    });
                }
            });


            worker.setDaemon(true);

            worker.start();
        }


        // =========================================================
        // STOP ATTACK
        // =========================================================

        @FXML
        private void handleStopAttack() {

            if (worker == null || !worker.isAlive()) {
                return;
            }

            // The algorithm loops check this flag between candidates and
            // return as soon as they see it set, so this is a real stop -
            // not just detaching the UI from a thread that keeps running.
            stopRequested = true;
            cancelFlag.set(true);
            worker.interrupt();

            updateStatus("Stopping...", false);

            stopButton.setDisable(true);
            startButton.setDisable(false);
            clearButton.setDisable(false);
        }


        // =========================================================
        // CLEAR
        // =========================================================

        @FXML
        private void handleClear() {

            targetHashField.clear();

            algorithmBox.getSelectionModel().selectFirst();

            maxLengthField.setText("4");

            maskField.setText("?l?l?l");

            progressBar.setProgress(0);

            resetSummary();

            updateStatus("Idle", false);
        }


        // =========================================================
        // BACK TO DASHBOARD
        // =========================================================

        @FXML
        private void handleBack() {

            try {
                PasswordCrackerGUI.showScene("/main-menu.fxml");
            } catch (java.io.IOException ex) {
                updateStatus("Failed to open Dashboard: " + ex.getMessage(), false);
            }
        }


        // =========================================================
        // RUN ATTACK
        // =========================================================

        private AttackResult runAttack(
                String algorithm,
                String targetHash
        ) throws Exception {


            switch (algorithm) {


                // =================================================
                // BRUTE FORCE
                // =================================================

                case "Brute Force":

                    int bruteLength =
                            parseLength(
                                    maxLengthField.getText(),
                                    4
                            );


                    String bruteResult =
                            BruteForceCracker.crackPassword(
                                    targetHash,
                                    bruteLength,
                                    cancelFlag
                            );


                    return new AttackResult(

                            bruteResult != null,

                            bruteResult != null
                                    ? bruteResult
                                    : "Not Found",

                            BruteForceCracker.getAttemptsCount(),

                            "Brute Force Attack\n\n" +
                            "Technique: Brute Force\n" +
                            "Maximum Length: " +
                            bruteLength
                    );


                // =================================================
                // DICTIONARY
                // =================================================

                case "Dictionary":

                    String dictionaryResult =
                            DictionaryAttackCracker.crackPassword(
                                    targetHash,
                                    cancelFlag
                            );


                    return new AttackResult(

                            dictionaryResult != null,

                            dictionaryResult != null
                                    ? dictionaryResult
                                    : "Not Found",

                            DictionaryAttackCracker.getAttemptsCount(),

                            "Dictionary Attack\n\n" +
                            "Technique: Dictionary Attack\n" +
                            "Wordlist-based password search"
                    );


                // =================================================
                // HYBRID
                // =================================================

                case "Hybrid":

                    HybridAttackCracker hybrid =
                            new HybridAttackCracker();


                    int hybridMaxLength =
                            parseLength(
                                    maxLengthField.getText(),
                                    3
                            );


                    String hybridResult =
                            hybrid.crack(
                                    targetHash,
                                    hybridMaxLength,
                                    cancelFlag
                            );


                    return new AttackResult(

                            !"Not Found".equals(hybridResult),

                            hybridResult,

                            hybrid.getAttemptsCount(),

                            "Hybrid Attack\n\n" +

                            "Technique: Hybrid Attack\n\n" +

                            "Phase 1: Dictionary Attack\n" +

                            "Phase 2: Brute Force Attack\n\n" +

                            "Maximum Brute Force Length: " +
                            hybridMaxLength
                    );


                // =================================================
                // MASK
                // =================================================

                case "Mask":

                    String maskPattern =
                            maskField.getText().trim();


                    if (maskPattern.isEmpty()) {

                        maskPattern = "?l?l?l";
                    }


                    MaskAttackCracker mask =
                            new MaskAttackCracker();


                    String maskResult =
                            mask.crack(
                                    targetHash,
                                    maskPattern,
                                    cancelFlag
                            );


                    return new AttackResult(

                            !"Not Found".equals(maskResult),

                            maskResult,

                            mask.getAttemptsCount(),

                            "Mask Attack\n\n" +

                            "Technique: Mask-based Search\n" +

                            "Pattern: " +
                            maskPattern
                    );


                // =================================================
                // MARKOV
                // =================================================

                case "Markov":

                    int markovLength =
                            parseLength(
                                    maxLengthField.getText(),
                                    6
                            );


                    MarkovChainCracker markov =
                            new MarkovChainCracker();


                    List<String> trainingPasswords =
                            Arrays.asList(

                                    "password",
                                    "admin",
                                    "welcome",
                                    "hello",
                                    "abc123",
                                    "admin123",
                                    "password123",
                                    "cat",
                                    "dog",
                                    "test123"
                            );


                    markov.train(trainingPasswords);


                    String markovResult =
                            markov.crack(
                                    targetHash,
                                    markovLength,
                                    cancelFlag
                            );


                    return new AttackResult(

                            !"Not Found".equals(markovResult),

                            markovResult,

                            markov.getAttemptsCount(),

                            "Markov Chain Attack\n\n" +

                            "Technique: Markov-based Password Generation\n" +

                            "Training Passwords: " +
                            trainingPasswords.size() +

                            "\nMaximum Length: " +
                            markovLength
                    );


                // =================================================
                // DEFAULT
                // =================================================

                default:

                    throw new IllegalArgumentException(
                            "Unsupported algorithm: " +
                                    algorithm
                    );
            }
        }


        // =========================================================
        // SHOW ALERT
        // =========================================================

        private void showAlert(
                Alert.AlertType type,
                String title,
                String message
        ) {

            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
        }


        // =========================================================
        // PARSE LENGTH
        // =========================================================

        private int parseLength(
                String value,
                int defaultValue
        ) {

            try {

                return Math.max(
                        1,
                        Integer.parseInt(
                                value.trim()
                        )
                );

            } catch (NumberFormatException ex) {

                return defaultValue;
            }
        }


        // =========================================================
        // UPDATE STATUS
        // =========================================================

        private void updateStatus(
                String text,
                boolean running
        ) {

            statusLabel.setText(text);

            statusLabel.getStyleClass().setAll(
                    running
                            ? "status-running"
                            : "status-idle"
            );
        }


        // =========================================================
        // RESET SUMMARY
        // =========================================================

        private void resetSummary() {

            attemptsLabel.setText(
                    "Attempts : 0"
            );

            executionTimeLabel.setText(
                    "Execution Time : 0 ms"
            );

            passwordLabel.setText(
                    "Password : Not Found"
            );

            techniquesLabel.setText(
                    "-"
            );

            outputArea.setText(
                    "No attack run yet."
            );
        }
    }


    // =============================================================
    // ATTACK RESULT
    // =============================================================

    private static class AttackResult {

        private final boolean success;

        private final String password;

        private final int attempts;

        private final String details;


        private AttackResult(
                boolean success,
                String password,
                int attempts,
                String details
        ) {

            this.success = success;

            this.password = password;

            this.attempts = attempts;

            this.details = details;
        }


        // =========================================================
        // GETTERS
        // =========================================================

        public boolean isSuccess() {

            return success;
        }


        public String getPassword() {

            return password;
        }


        public int getAttempts() {

            return attempts;
        }


        public String getDetails() {

            return details;
        }
    }
}