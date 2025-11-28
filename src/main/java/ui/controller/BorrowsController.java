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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class BorrowsController {

    @FXML private TextField borrowMemberIdField;
    @FXML private TextField borrowDocTitleField;
    @FXML private DatePicker borrowDateField;
    @FXML private DatePicker expectedReturnDateField;
    @FXML private Label memberInfoLabel;
    @FXML private Label documentInfoLabel;
    @FXML private VBox borrowsListView;
    @FXML private VBox overdueListView;
    @FXML private VBox historyListView;
    @FXML private Label activeBorrowsLabel;
    @FXML private Label overdueBorrowsLabel;
    @FXML private Label returnedTodayLabel;
    @FXML private Label totalPenaltiesLabel;
    @FXML private Label overdueCountLabel;
    @FXML private ComboBox<String> historyFilterField;

    private LibraryManagerDAO manager;
    private Member selectedMember;
    private Document selectedDocument;

    public BorrowsController() {
        MemberDAO memberDAO = new MemberDAOImpl();
        DocumentDAO documentDAO = new DocumentDAOImpl();
        BookDAO bookDAO = new BookDAOImpl();
        BorrowDAO borrowDAO = new BorrowDAOImpl();
        MagazineDAO magazineDAO = new MagazineDAOImpl();
        manager = new LibraryManagerDAO(memberDAO, documentDAO, bookDAO, magazineDAO, borrowDAO);
    }

    @FXML
    public void initialize() {
        // Guard in case FXML injection failed for some fields
        try {
            System.out.println("BorrowsController initialized!");

            if (borrowDateField != null) {
                borrowDateField.setValue(LocalDate.now());
                borrowDateField.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (expectedReturnDateField != null && newVal != null) {
                        expectedReturnDateField.setValue(newVal.plusDays(14));
                    }
                });
            }

            if (expectedReturnDateField != null && expectedReturnDateField.getValue() == null) {
                expectedReturnDateField.setValue(LocalDate.now().plusDays(14));
            }

            if (historyFilterField != null) {
                historyFilterField.getItems().addAll(
                    "Last 7 days",
                    "Last 30 days",
                    "Last 3 months",
                    "All time"
                );
                historyFilterField.getSelectionModel().selectFirst();
            }

            // Defer heavy UI refresh to JavaFX thread to avoid FXML load ordering issues
            Platform.runLater(() -> {
                try {
                    refreshBorrows();
                    updateStatistics();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            // If anything fails during init, log but do not crash loader
            e.printStackTrace();
        }
    }

    @FXML
    private void searchMember() {
        try {
            if (borrowMemberIdField == null) return;

            String idText = borrowMemberIdField.getText().trim();
            if (idText.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Input Required", "Please enter a member ID!");
                return;
            }

            int memberId = Integer.parseInt(idText);
            selectedMember = manager.searchMemberById(memberId);

            if (selectedMember != null && memberInfoLabel != null) {
                memberInfoLabel.setText("✓ " + selectedMember.getName() + " " +
                        selectedMember.getSurname() + " (Status: " +
                        selectedMember.getPenaltyStatus().name() + ")");
                memberInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #48bb78; -fx-font-weight: bold;");
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Member ID must be a valid number!");
            selectedMember = null;
            if (memberInfoLabel != null) memberInfoLabel.setText("");
        } catch (MemberNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "Not Found", "No member found with ID: " + borrowMemberIdField.getText());
            selectedMember = null;
            if (memberInfoLabel != null) memberInfoLabel.setText("");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to search member: " + e.getMessage());
            selectedMember = null;
            if (memberInfoLabel != null) memberInfoLabel.setText("");
        }
    }

    @FXML
    private void searchDocument() {
        try {
            if (borrowDocTitleField == null) return;

            String title = borrowDocTitleField.getText().trim();
            if (title.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Input Required", "Please enter a book title!");
                return;
            }

            selectedDocument = manager.getDocumentByTitle(title);

            if (selectedDocument != null && documentInfoLabel != null) {
                // Verify the document has a valid ID
                if (selectedDocument.getIdDoc() <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Document ID not found. Please try again.");
                    selectedDocument = null;
                    documentInfoLabel.setText("");
                    return;
                }
                
                String availability = selectedDocument.isAvailable() ? "Available" : "Not Available";
                documentInfoLabel.setText("✓ " + selectedDocument.getTitle() + " by " +
                        selectedDocument.getAuthor() + " (" + availability + ")");

                if (selectedDocument.isAvailable()) {
                    documentInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #48bb78; -fx-font-weight: bold;");
                } else {
                    documentInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ed8936; -fx-font-weight: bold;");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Not Found", "No document found with title: " + title);
                selectedDocument = null;
                if (documentInfoLabel != null) documentInfoLabel.setText("");
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to search document: " + e.getMessage());
            selectedDocument = null;
            if (documentInfoLabel != null) documentInfoLabel.setText("");
            e.printStackTrace();
        }
    }

    @FXML
    private void addBorrow() {
        try {
            // 1️⃣ Validate UI selection
            if (selectedMember == null) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please search and select a member first!");
                return;
            }
            if (selectedDocument == null) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please search and select a document first!");
                return;
            }
            if (borrowDateField == null || borrowDateField.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a borrow date!");
                return;
            }
            if (!selectedDocument.isAvailable()) {
                showAlert(Alert.AlertType.ERROR, "Not Available", "This document is currently borrowed!");
                return;
            }
            if (selectedMember.getPenaltyStatus().getLevel() >= 2) {
                showAlert(Alert.AlertType.ERROR, "Member Suspended",
                        "This member is suspended or banned and cannot borrow books!");
                return;
            }
            if (selectedMember.getNbBorrows() >= 5) {
                showAlert(Alert.AlertType.ERROR, "Borrow Limit",
                        "This member has reached the maximum borrow limit (5 documents)!");
                return;
            }

            // 2️⃣ Fetch persisted member and document via DAOs
            Member persistedMember = manager.getMemberDAO().searchMemberById(selectedMember.getIdMember());
            if (persistedMember == null) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Selected member does not exist in the database!");
                return;
            }

            Document persistedDocument = manager.getDocumentDAO().getDocumentById(selectedDocument.getIdDoc());
            if (persistedDocument == null) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Selected document does not exist in the database!");
                return;
            }

            // 3️⃣ Create Borrow object with proper dates and IDs
            Borrow borrow = new Borrow(persistedDocument, persistedMember, borrowDateField.getValue());
            borrow.setExpectedReturnDate(expectedReturnDateField.getValue());
            borrow.setReturnDate(null);

            // 4️⃣ Persist borrow via DAO
            boolean success = manager.getBorrowDAO().addBorrow(borrow);

            if (success) {
                // Update document availability
                persistedDocument.setAvailability(false);
                manager.getDocumentDAO().updateDocument(persistedDocument);
                
                // Update member's borrow count
                // Update member's borrow count
                persistedMember.setNbBorrows(persistedMember.getNbBorrows() + 1);
                manager.getMemberDAO().updateMember(
                    persistedMember,
                    persistedMember.getName(),
                    persistedMember.getSurname(),
                    persistedMember.getPenaltyStatus()
                );
                
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Document borrowed successfully!\n\n" +
                                "Member: " + persistedMember.getName() + " " + persistedMember.getSurname() + "\n" +
                                "Document: " + persistedDocument.getTitle() + "\n" +
                                "Borrow Date: " + borrow.getBorrowDate() + "\n" +
                                "Expected Return: " + borrow.getExpectedReturnDate());

                clearFields();
                refreshBorrows();
                updateStatistics();
            } else {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save borrow to database!");
            }

        } catch (BorrowException e) {
            showAlert(Alert.AlertType.ERROR, "Borrow Error", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to create borrow: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void clearFields() {
        if (borrowMemberIdField != null) borrowMemberIdField.clear();
        if (borrowDocTitleField != null) borrowDocTitleField.clear();
        if (borrowDateField != null) borrowDateField.setValue(LocalDate.now());
        if (expectedReturnDateField != null) expectedReturnDateField.setValue(LocalDate.now().plusDays(14));
        if (memberInfoLabel != null) memberInfoLabel.setText("");
        if (documentInfoLabel != null) documentInfoLabel.setText("");
        selectedMember = null;
        selectedDocument = null;
    }

    private void addBorrowCard(Borrow borrow, VBox container, boolean isOverdue) {
        if (borrow == null || container == null) return;

        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);

        String borderColor = isOverdue ? "#f56565" : "#e2e8f0";
        String bgColor = isOverdue ? "#fff5f5" : "white";

        card.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 15; -fx-background-radius: 8; " +
                "-fx-border-color: " + borderColor + "; -fx-border-radius: 8; -fx-border-width: 2;");

        Label memberLabel = new Label(borrow.getMember().getName() + " " + borrow.getMember().getSurname());
        memberLabel.setPrefWidth(150);
        memberLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2d3748; -fx-font-weight: bold;");

        Label bookLabel = new Label(borrow.getDocument().getTitle());
        bookLabel.setPrefWidth(200);
        bookLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2d3748;");
        bookLabel.setWrapText(true);

        Label borrowDateLabel = new Label(borrow.getBorrowDate().toString());
        borrowDateLabel.setPrefWidth(120);
        borrowDateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #718096;");

        Label expectedReturnLabel = new Label(borrow.getExpectedReturnDate().toString());
        expectedReturnLabel.setPrefWidth(120);
        expectedReturnLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #718096;");

        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), borrow.getExpectedReturnDate());
        Label daysLabel = new Label();
        daysLabel.setPrefWidth(100);

        Label statusOrPenaltyLabel = new Label();
        statusOrPenaltyLabel.setPrefWidth(100);

        if (isOverdue) {
            long daysOverdue = Math.abs(daysLeft);
            daysLabel.setText(daysOverdue + " days");
            daysLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #e53e3e; -fx-font-weight: bold;");

            double penalty = daysOverdue * 0.5;
            statusOrPenaltyLabel.setText("$" + String.format("%.2f", penalty));
            statusOrPenaltyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ed8936; -fx-font-weight: bold;");
        } else {
            daysLabel.setText(daysLeft + " days");
            String color = daysLeft <= 3 ? "#ed8936" : "#48bb78";
            daysLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");

            statusOrPenaltyLabel.setText("Active");
            statusOrPenaltyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #667eea; -fx-font-weight: bold;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button returnButton = new Button("Return");
        returnButton.setStyle("-fx-background-color: #48bb78; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-background-radius: 6; -fx-padding: 8 20;");
        returnButton.setOnAction(e -> returnBook(borrow));

        Button detailsButton = new Button("Details");
        detailsButton.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-background-radius: 6; -fx-padding: 8 20;");
        detailsButton.setOnAction(e -> viewBorrowDetails(borrow));

        HBox actionsBox = new HBox(10, returnButton, detailsButton);
        actionsBox.setPrefWidth(120);

        card.getChildren().addAll(memberLabel, bookLabel, borrowDateLabel, expectedReturnLabel, daysLabel, spacer, statusOrPenaltyLabel, actionsBox);

        container.getChildren().add(card);
    }

    private void viewBorrowDetails(Borrow borrow) {
        if (borrow == null) return;

        StringBuilder details = new StringBuilder();
        details.append("Borrow Details\n\n");
        details.append("Document: ").append(borrow.getDocument().getTitle()).append("\n");
        details.append("Author: ").append(borrow.getDocument().getAuthor()).append("\n\n");
        details.append("Member: ").append(borrow.getMember().getName()).append(" ")
                .append(borrow.getMember().getSurname()).append("\n");
        details.append("Member ID: ").append(borrow.getMember().getIdMember()).append("\n\n");
        details.append("Borrow Date: ").append(borrow.getBorrowDate()).append("\n");
        details.append("Expected Return: ").append(borrow.getExpectedReturnDate()).append("\n");

        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), borrow.getExpectedReturnDate());
        if (daysLeft < 0) {
            details.append("Status: OVERDUE (").append(Math.abs(daysLeft)).append(" days late)\n");
            double penalty = Math.abs(daysLeft) * 0.5;
            details.append("Penalty: $").append(String.format("%.2f", penalty));
        } else {
            details.append("Status: Active\n");
            details.append("Days Left: ").append(daysLeft).append(" days");
        }

        showAlert(Alert.AlertType.INFORMATION, "Borrow Details", details.toString());
    }

    private void updateStatistics() {
        try {
            List<Borrow> currentBorrows = manager.getCurrentBorrows();
            List<Borrow> lateBorrows = manager.getLateBorrows();

            int activeCount = 0;
            if (currentBorrows != null) {
                for (Borrow b : currentBorrows) {
                    if (b.getReturnDate() == null) activeCount++;
                }
            }
            if (activeBorrowsLabel != null) activeBorrowsLabel.setText(String.valueOf(activeCount));

            if (overdueBorrowsLabel != null) overdueBorrowsLabel.setText(String.valueOf(lateBorrows == null ? 0 : lateBorrows.size()));

            if (returnedTodayLabel != null) returnedTodayLabel.setText(String.valueOf(manager.getReturnedTodayCount()));

            if (totalPenaltiesLabel != null) {
                double totalPenalties = 0;
                if (lateBorrows != null) {
                    for (Borrow b : lateBorrows) {
                        long daysLate = ChronoUnit.DAYS.between(b.getExpectedReturnDate(), LocalDate.now());
                        if (daysLate > 0) totalPenalties += daysLate * 0.5;
                    }
                }
                totalPenaltiesLabel.setText("$" + String.format("%.2f", totalPenalties));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // fail silently for UI stability, but show minimal feedback
            if (totalPenaltiesLabel != null) totalPenaltiesLabel.setText("$0.00");
        }
    }

    @FXML
    private void goBack() throws Exception {
        Stage stage = null;
        if (borrowMemberIdField != null) stage = (Stage) borrowMemberIdField.getScene().getWindow();
        if (stage == null && documentInfoLabel != null) stage = (Stage) documentInfoLabel.getScene().getWindow();
        if (stage == null) return;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/main-view.fxml"));
        stage.setScene(new Scene(loader.load()));
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        // Protect against being called while UI not ready
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

@FXML
private void refreshBorrows() {
    try {
        List<Borrow> allBorrows = manager.getAllBorrows();

        // --- Active Borrows ---
        if (borrowsListView != null) {
            borrowsListView.getChildren().clear();

            List<Borrow> activeBorrows = new ArrayList<>();
            if (allBorrows != null) {
                for (Borrow b : allBorrows) {
                    if (b.getReturnDate() == null) {
                        activeBorrows.add(b);
                    }
                }
            }

            if (activeBorrows.isEmpty()) {
                Label placeholder = new Label("No active borrows at the moment.");
                placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #718096; -fx-padding: 20;");
                borrowsListView.getChildren().add(placeholder);
            } else {
                for (Borrow b : activeBorrows) {
                    boolean overdue = b.getExpectedReturnDate() != null &&
                                      b.getExpectedReturnDate().isBefore(LocalDate.now());
                    addBorrowCard(b, borrowsListView, overdue, false);
                }
            }

            if (activeBorrowsLabel != null) activeBorrowsLabel.setText(String.valueOf(activeBorrows.size()));
        }

        // --- Overdue Borrows ---
        if (overdueListView != null) {
            overdueListView.getChildren().clear();

            List<Borrow> overdueBorrows = new ArrayList<>();
            if (allBorrows != null) {
                for (Borrow b : allBorrows) {
                    if (b.getReturnDate() == null && 
                        b.getExpectedReturnDate() != null && 
                        b.getExpectedReturnDate().isBefore(LocalDate.now())) {
                        overdueBorrows.add(b);
                    }
                }
            }

            if (overdueBorrows.isEmpty()) {
                Label placeholder = new Label("No overdue borrows. Great job! 🎉");
                placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #48bb78; -fx-padding: 20; -fx-font-weight: bold;");
                overdueListView.getChildren().add(placeholder);
            } else {
                for (Borrow b : overdueBorrows) {
                    addBorrowCard(b, overdueListView, true, false);
                }
            }

            if (overdueBorrowsLabel != null) overdueBorrowsLabel.setText(String.valueOf(overdueBorrows.size()));
        }

        // --- Returned / History Borrows ---
        if (historyListView != null) {
            historyListView.getChildren().clear();

            List<Borrow> returnedBorrows = new ArrayList<>();
            if (allBorrows != null) {
                for (Borrow b : allBorrows) {
                    if (b.getReturnDate() != null) {
                        returnedBorrows.add(b);
                    }
                }
            }

            if (returnedBorrows.isEmpty()) {
                Label placeholder = new Label("No returned documents in history.");
                placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #718096; -fx-padding: 20;");
                historyListView.getChildren().add(placeholder);
            } else {
                // Sort returned borrows by returnDate descending
                returnedBorrows.sort((b1, b2) -> b2.getReturnDate().compareTo(b1.getReturnDate()));

                for (Borrow b : returnedBorrows) {
                    addBorrowCard(b, historyListView, false, true);
                }
            }

            if (returnedTodayLabel != null) {
                long returnedToday = returnedBorrows.stream()
                        .filter(b -> b.getReturnDate().isEqual(LocalDate.now()))
                        .count();
                returnedTodayLabel.setText(String.valueOf(returnedToday));
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Error", "Failed to refresh borrows: " + e.getMessage());
    }
}

private void addBorrowCard(Borrow borrow, VBox container, boolean isOverdue, boolean isHistory) {
    if (borrow == null || container == null) return;

    HBox card = new HBox(10);
    card.setAlignment(Pos.CENTER_LEFT);

    String borderColor = isOverdue ? "#f56565" : isHistory ? "#cbd5e0" : "#e2e8f0";
    String bgColor = isOverdue ? "#fff5f5" : isHistory ? "#f7fafc" : "white";

    card.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 15; -fx-background-radius: 8; " +
            "-fx-border-color: " + borderColor + "; -fx-border-radius: 8; -fx-border-width: 2;");

    Label memberLabel = new Label(borrow.getMember().getName() + " " + borrow.getMember().getSurname());
    memberLabel.setPrefWidth(150);
    memberLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2d3748; -fx-font-weight: bold;");

    Label bookLabel = new Label(borrow.getDocument().getTitle());
    bookLabel.setPrefWidth(200);
    bookLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2d3748;");
    bookLabel.setWrapText(true);

    Label borrowDateLabel = new Label(borrow.getBorrowDate().toString());
    borrowDateLabel.setPrefWidth(120);
    borrowDateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #718096;");

    Label expectedReturnLabel = new Label(borrow.getExpectedReturnDate().toString());
    expectedReturnLabel.setPrefWidth(120);
    expectedReturnLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #718096;");

    Label statusLabel = new Label();
    statusLabel.setPrefWidth(100);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox actionsBox = new HBox(10);
    actionsBox.setPrefWidth(180);

    if (isHistory) {
        // For returned documents in history
        statusLabel.setText("✓ Returned");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #48bb78; -fx-font-weight: bold;");
        
        Label returnDateLabel = new Label("on " + borrow.getReturnDate().toString());
        returnDateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096;");
        
        Button detailsButton = new Button("Details");
        detailsButton.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-background-radius: 6; -fx-padding: 8 20;");
        detailsButton.setOnAction(e -> viewBorrowDetails(borrow));
        
        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: #f56565; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-background-radius: 6; -fx-padding: 8 20;");
        deleteButton.setOnAction(e -> deleteBorrow(borrow));
        
        actionsBox.getChildren().addAll(detailsButton, deleteButton);
        card.getChildren().addAll(memberLabel, bookLabel, borrowDateLabel, returnDateLabel, spacer, statusLabel, actionsBox);
        
    } else {
        // For active borrows
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), borrow.getExpectedReturnDate());
        Label daysLabel = new Label();
        daysLabel.setPrefWidth(100);

        if (isOverdue) {
            long daysOverdue = Math.abs(daysLeft);
            daysLabel.setText(daysOverdue + " days");
            daysLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #e53e3e; -fx-font-weight: bold;");

            double penalty = daysOverdue * 0.5;
            statusLabel.setText("$" + String.format("%.2f", penalty));
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ed8936; -fx-font-weight: bold;");
        } else {
            daysLabel.setText(daysLeft + " days");
            String color = daysLeft <= 3 ? "#ed8936" : "#48bb78";
            daysLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");

            statusLabel.setText("Active");
            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #667eea; -fx-font-weight: bold;");
        }

        Button returnButton = new Button("Return");
        returnButton.setStyle("-fx-background-color: #48bb78; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-background-radius: 6; -fx-padding: 8 20;");
        returnButton.setOnAction(e -> returnBook(borrow));

        Button detailsButton = new Button("Details");
        detailsButton.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-background-radius: 6; -fx-padding: 8 20;");
        detailsButton.setOnAction(e -> viewBorrowDetails(borrow));

        actionsBox.getChildren().addAll(returnButton, detailsButton);
        card.getChildren().addAll(memberLabel, bookLabel, borrowDateLabel, expectedReturnLabel, daysLabel, spacer, statusLabel, actionsBox);
    }

    container.getChildren().add(card);
}

private void returnBook(Borrow borrow) {
    if (borrow == null) return;

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Confirm Return");
    confirm.setHeaderText("Return Document");

    long daysLate = ChronoUnit.DAYS.between(borrow.getExpectedReturnDate(), LocalDate.now());
    String message = "Return document: " + borrow.getDocument().getTitle() + "\n" +
            "Member: " + borrow.getMember().getName() + " " + borrow.getMember().getSurname();

    if (daysLate > 0) {
        double penalty = daysLate * 0.5;
        message += "\n\nThis document is " + daysLate + " days late.\n" +
                "Penalty: $" + String.format("%.2f", penalty);
    }

    confirm.setContentText(message);

    confirm.showAndWait().ifPresent(response -> {
        if (response == ButtonType.OK) {
            try {
                // Use removeBorrow which sets returnDate
                manager.removeBorrow(borrow);

                String successMsg = "Document returned successfully!";
                if (daysLate > 0) {
                    double penalty = daysLate * 0.5;
                    successMsg += "\n\nPenalty charged: $" + String.format("%.2f", penalty);
                }

                showAlert(Alert.AlertType.INFORMATION, "Success", successMsg);
                refreshBorrows();
                updateStatistics();

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to return document: " + e.getMessage());
                e.printStackTrace();
            }
        }
    });
}

private void deleteBorrow(Borrow borrow) {
    if (borrow == null) return;

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Confirm Delete");
    confirm.setHeaderText("Delete Borrow Record");
    confirm.setContentText("Are you sure you want to permanently delete this borrow record?\n\n" +
            "Document: " + borrow.getDocument().getTitle() + "\n" +
            "Member: " + borrow.getMember().getName() + " " + borrow.getMember().getSurname() + "\n" +
            "Borrowed: " + borrow.getBorrowDate() + "\n" +
            "Returned: " + borrow.getReturnDate() + "\n\n" +
            "This action cannot be undone!");

    ButtonType deleteButton = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
    ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    confirm.getButtonTypes().setAll(deleteButton, cancelButton);

    confirm.showAndWait().ifPresent(response -> {
        if (response == deleteButton) {
            try {
                // Cast to BorrowDAOImpl to access deleteBorrow method
                if (manager.getBorrowDAO() instanceof BorrowDAOImpl borrowDAOImpl) {
                    borrowDAOImpl.deleteBorrow(borrow);
                    showAlert(Alert.AlertType.INFORMATION, "Success", 
                            "Borrow record deleted successfully!");
                    refreshBorrows();
                    updateStatistics();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", 
                            "Unable to access delete functionality.");
                }

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", 
                        "Failed to delete borrow: " + e.getMessage());
                e.printStackTrace();
            }
        }
    });
}

    
}