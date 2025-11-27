package ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.geometry.Pos;
import com.libman.dao.*;
import com.libman.model.*;
import com.libman.exception.*;

import java.util.List;

public class MembersController {
    @FXML private TextField memberNameField;
    @FXML private TextField memberSurnameField;
    @FXML private TextField searchField;
    @FXML private VBox membersListView;
    @FXML private Label memberCountLabel;

    private LibraryManagerDAO manager;

    public MembersController() {
        MemberDAO memberDAO = new MemberDAOImpl();
        DocumentDAO documentDAO = new DocumentDAOImpl();
        BookDAO bookDAO = new BookDAOImpl();
        BorrowDAO borrowDAO = new BorrowDAOImpl();
        MagazineDAO magazineDAO = new MagazineDAOImpl();
        manager = new LibraryManagerDAO(memberDAO, documentDAO, bookDAO, magazineDAO, borrowDAO);
    }

    @FXML
    public void initialize() {
        showAllMembers();
    }

    @FXML
    private void addMember() {
        try {
            String name = memberNameField.getText().trim();
            String surname = memberSurnameField.getText().trim();

            if (name.isEmpty() || surname.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Name and surname are required!");
                return;
            }

            int generatedId = manager.generateNextMemberId();
            Member m = new Member(generatedId, name, surname, PenaltyStatus.NONE);
            manager.addMember(m);

            showAlert(Alert.AlertType.INFORMATION,
                    "Member Added",
                    "Member added successfully!\nAssigned ID: " + generatedId);

            clearFields();
            showAllMembers();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add member: " + e.getMessage());
        }
    }

    @FXML
    private void clearFields() {
        memberNameField.clear();
        memberSurnameField.clear();
    }

    @FXML
    private void searchMember() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            showAllMembers();
            return;
        }

        membersListView.getChildren().clear();

        try {
            // Try ID search
            try {
                int id = Integer.parseInt(searchText);
                Member member = manager.searchMemberById(id);
                addMemberCard(member);
                memberCountLabel.setText("Found: 1 member");
                return;
            } catch (NumberFormatException ignored) {}

            // Try Name + Surname search
            String[] parts = searchText.split(" ");
            if (parts.length != 2) {
                showAlert(Alert.AlertType.INFORMATION, "Search",
                        "Please enter both name AND surname (e.g., 'John Doe').");
                return;
            }

            Member member = manager.searchMemberByName(parts[0], parts[1]);
            addMemberCard(member);
            memberCountLabel.setText("Found: 1 member");

        } catch (MemberNotFoundException e) {
            showAlert(Alert.AlertType.INFORMATION, "Not Found",
                    "No member found matching: " + searchText);
            memberCountLabel.setText("Found: 0 members");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Search failed: " + e.getMessage());
        }
    }

    @FXML
    private void showAllMembers() {
        membersListView.getChildren().clear();

        List<Member> members = manager.getAllMembers();
        if (members.isEmpty()) {
            Label placeholder = new Label("No members found.");
            placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #888;");
            membersListView.getChildren().add(placeholder);
            memberCountLabel.setText("Total: 0 members");
            return;
        }

        for (Member m : members) {
            addMemberCard(m);
        }
        memberCountLabel.setText("Total: " + members.size() + " members");
    }

    private void addMemberCard(Member member) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; " +
                      "-fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-border-width: 1;");

        Label idLabel = new Label(String.valueOf(member.getIdMember()));
        idLabel.setPrefWidth(80);
        idLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2d3748;");

        Label nameLabel = new Label(member.getName());
        nameLabel.setPrefWidth(150);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2d3748;");

        Label surnameLabel = new Label(member.getSurname());
        surnameLabel.setPrefWidth(150);
        surnameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2d3748;");

        Label statusLabel = new Label(member.getPenaltyStatus().name());
        statusLabel.setPrefWidth(120);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + getStatusColor(member.getPenaltyStatus()) +
                             "; -fx-font-weight: bold;");

        // Safely compute borrows & penalty
        int activeBorrows = 0;
        double totalPenalty = 0;
        try {
            activeBorrows = manager.getActiveBorrowsCount(member.getIdMember());
            totalPenalty = manager.getTotalPenaltyForMember(member.getIdMember());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Label borrowsLabel = new Label(String.valueOf(activeBorrows));
        borrowsLabel.setPrefWidth(80);
        borrowsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2d3748;");

        Label penaltyLabel = new Label("$" + String.format("%.2f", totalPenalty));
        penaltyLabel.setPrefWidth(80);
        penaltyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ed8936; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewButton = new Button("View");
        viewButton.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-size: 12px; " +
                            "-fx-background-radius: 6; -fx-padding: 6 15; -fx-cursor: hand;");
        viewButton.setOnAction(e -> viewMemberDetails(member));

        Button editButton = new Button("Edit");
        editButton.setStyle("-fx-background-color: #48bb78; -fx-text-fill: white; -fx-font-size: 12px; " +
                            "-fx-background-radius: 6; -fx-padding: 6 15; -fx-cursor: hand;");
        editButton.setOnAction(e -> showEditMemberDialog(member));

        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: #f56565; -fx-text-fill: white; -fx-font-size: 12px; " +
                              "-fx-background-radius: 6; -fx-padding: 6 15; -fx-cursor: hand;");
        deleteButton.setOnAction(e -> deleteMember(member));

        HBox actionsBox = new HBox(10, viewButton, editButton, deleteButton);
        actionsBox.setPrefWidth(180);

        card.getChildren().addAll(idLabel, nameLabel, surnameLabel, statusLabel,
                                 borrowsLabel, penaltyLabel, spacer, actionsBox);

        membersListView.getChildren().add(card);
    }

    private String getStatusColor(PenaltyStatus status) {
        switch (status) {
            case NONE: return "#48bb78";
            case WARNING: return "#ed8936";
            case SUSPENDED: return "#f56565";
            case BANNED: return "#c53030";
            default: return "#718096";
        }
    }

    private void viewMemberDetails(Member member) {
        try {
            List<Borrow> history = manager.getMemberHistory(member);
            int activeBorrows = manager.getActiveBorrowsCount(member.getIdMember());
            double totalPenalty = manager.getTotalPenaltyForMember(member.getIdMember());

            StringBuilder details = new StringBuilder();
            details.append("Member Details\n\n")
                   .append("ID: ").append(member.getIdMember()).append("\n")
                   .append("Name: ").append(member.getName()).append(" ").append(member.getSurname()).append("\n")
                   .append("Status: ").append(member.getPenaltyStatus().name()).append("\n")
                   .append("Active Borrows: ").append(activeBorrows).append("\n")
                   .append("Penalty: $").append(String.format("%.2f", totalPenalty)).append("\n\n")
                   .append("Borrow History: ").append(history.size()).append(" items");

            showAlert(Alert.AlertType.INFORMATION, "Member Details", details.toString());

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to fetch member details: " + e.getMessage());
        }
    }

    private void showEditMemberDialog(Member member) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Member");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(member.getName());
        TextField surnameField = new TextField(member.getSurname());

        ComboBox<PenaltyStatus> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(PenaltyStatus.values());
        statusBox.setValue(member.getPenaltyStatus());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Surname:"), 0, 1);
        grid.add(surnameField, 1, 1);
        grid.add(new Label("Penalty:"), 0, 2);
        grid.add(statusBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                manager.updateMember(member, nameField.getText().trim(), surnameField.getText().trim(), statusBox.getValue());
                showAlert(Alert.AlertType.INFORMATION, "Success", "Member modified successfully!");
                Platform.runLater(this::showAllMembers);
            }
        });
    }

    private void deleteMember(Member member) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Member");
        confirm.setContentText("Are you sure you want to delete " 
                + member.getName() + " " + member.getSurname() + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    manager.deleteMember(member);
                    showAlert(Alert.AlertType.INFORMATION,
                            "Deleted",
                            "Member '" + member.getName() + " " + member.getSurname() + "' was deleted.");
                    showAllMembers();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete member: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void goBack() throws Exception {
        Stage stage = (Stage) memberNameField.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/main-view.fxml"));
        stage.setScene(new Scene(loader.load()));
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}