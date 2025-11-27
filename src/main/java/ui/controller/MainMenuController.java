package ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import java.io.IOException;

public class MainMenuController {

    // ===============================
    // BUTTON ACTION METHODS
    // ===============================

    @FXML
    private void goToMembers(ActionEvent event) {
        changeScene(event, "/ui/members-view.fxml");
    }

    @FXML
    private void goToDocuments(ActionEvent event) {
        changeScene(event, "/ui/documents-view.fxml");
    }

    @FXML
    private void goToBorrows(ActionEvent event) {
        changeScene(event, "/ui/borrows-view.fxml");
    }

    // ===============================
    // SCENE SWITCHING HELPER
    // ===============================

    private void changeScene(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Scene Load Error", "Cannot load scene: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    // ===============================
    // UTILITY METHOD FOR ALERTS
    // ===============================

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}