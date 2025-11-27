package ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import com.libman.dao.*;
import com.libman.model.*;

import java.util.List;
import java.util.Optional;

public class DocumentsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> searchTypeField;
    @FXML private VBox documentsListView;
    @FXML private Label docCountLabel;
    @FXML private Label totalDocsLabel;
    @FXML private Label availableDocsLabel;
    @FXML private Label borrowedDocsLabel;
    @FXML private Label genresCountLabel;

    private LibraryManagerDAO manager;

    public DocumentsController() {
        MemberDAO memberDAO = new MemberDAOImpl();
        DocumentDAO documentDAO = new DocumentDAOImpl();
        BookDAO bookDAO = new BookDAOImpl();
        MagazineDAO magazineDAO = new MagazineDAOImpl();
        BorrowDAO borrowDAO = new BorrowDAOImpl();

        manager = new LibraryManagerDAO(memberDAO, documentDAO, bookDAO, magazineDAO, borrowDAO);
    }

    @FXML
    public void initialize() {
        if (searchTypeField != null) {
            searchTypeField.getItems().addAll("All", "Title", "Author", "Genre", "ISBN");
            searchTypeField.setValue("All");
        }

        // Fetch all documents
        List<Document> allDocs = manager.getAllDocuments();

        // Update availability for each document based on borrow status
        for (Document doc : allDocs) {
            boolean isBorrowed = manager.isDocumentBorrowed(doc.getIdDoc());
            doc.setAvailability(!isBorrowed);
            addDocumentCard(doc);
        }

        // Update labels
        docCountLabel.setText("Showing " + allDocs.size() + " documents");
        updateStatistics(allDocs);
    }

    @FXML
    private void searchDocument() {
        String query = searchField.getText().trim();
        String type = searchTypeField.getValue();
        documentsListView.getChildren().clear();

        if (query.isEmpty()) {
            showAllDocuments();
            return;
        }

        List<Document> results;
        try {
            results = manager.searchDocuments(type, query);
            if (results.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Not Found", "No document found matching: " + query);
                docCountLabel.setText("Found: 0 documents");
                return;
            }
            results.forEach(this::addDocumentCard);
            docCountLabel.setText("Found: " + results.size() + " documents");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Search failed: " + e.getMessage());
        }
    }

    @FXML
    private void showAllDocuments() {
        documentsListView.getChildren().clear();
        List<Document> allDocs = manager.getAllDocuments();

        if (allDocs.isEmpty()) {
            Label placeholder = new Label("No documents available.\nAdd books or magazines to populate the catalog.");
            placeholder.setStyle("-fx-text-fill: #718096; -fx-font-size: 14px; -fx-padding: 20; -fx-text-alignment: center;");
            placeholder.setWrapText(true);
            documentsListView.getChildren().add(placeholder);
            docCountLabel.setText("Showing 0 documents");
            updateStatistics(allDocs);  // pass empty list
            return;
        }

        // Update availability for each document based on borrow status
        for (Document doc : allDocs) {
            boolean isBorrowed = manager.isDocumentBorrowed(doc.getIdDoc());
            doc.setAvailability(!isBorrowed);
            addDocumentCard(doc);
        }

        docCountLabel.setText("Showing " + allDocs.size() + " documents");
        updateStatistics(allDocs);
    }

    @FXML
    private void addDocument() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Document");
        dialog.setHeaderText("Choose document type and enter details");

        // Create dialog buttons
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Document type selector
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Book", "Magazine");
        typeCombo.setValue("Book");

        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        
        TextField authorField = new TextField();
        authorField.setPromptText("Author");
        
        TextField genreField = new TextField();
        genreField.setPromptText("Genre");

        // Book-specific fields
        TextField isbnField = new TextField();
        isbnField.setPromptText("ISBN");
        
        TextField pagesField = new TextField();
        pagesField.setPromptText("Number of Pages");

        // Magazine-specific fields
        TextField numberField = new TextField();
        numberField.setPromptText("Issue Number (e.g., 42)");
        
        ComboBox<String> periodicityCombo = new ComboBox<>();
        periodicityCombo.getItems().addAll("DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY", 
                                           "BIMONTHLY", "QUARTERLY", "YEARLY");
        periodicityCombo.setPromptText("Select Periodicity");

        // Container for type-specific fields
        VBox specificFieldsBox = new VBox(10);
        
        // Initially show book fields
        specificFieldsBox.getChildren().addAll(
            new Label("ISBN:"), isbnField,
            new Label("Pages:"), pagesField
        );

        // Update fields based on document type
        typeCombo.setOnAction(e -> {
            specificFieldsBox.getChildren().clear();
            if ("Book".equals(typeCombo.getValue())) {
                specificFieldsBox.getChildren().addAll(
                    new Label("ISBN:"), isbnField,
                    new Label("Pages:"), pagesField
                );
            } else {
                specificFieldsBox.getChildren().addAll(
                    new Label("Issue Number:"), numberField,
                    new Label("Periodicity:"), periodicityCombo
                );
            }
        });

        grid.add(new Label("Document Type:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Title:"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("Author:"), 0, 2);
        grid.add(authorField, 1, 2);
        grid.add(new Label("Genre:"), 0, 3);
        grid.add(genreField, 1, 3);
        grid.add(specificFieldsBox, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Handle add button
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == addButtonType) {
            try {
                String title = titleField.getText().trim();
                String author = authorField.getText().trim();
                String genre = genreField.getText().trim();

                if (title.isEmpty() || author.isEmpty() || genre.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Validation Error", "Title, Author, and Genre are required!");
                    return;
                }

                if ("Book".equals(typeCombo.getValue())) {
                    String isbn = isbnField.getText().trim();
                    String pagesStr = pagesField.getText().trim();

                    if (isbn.isEmpty() || pagesStr.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "ISBN and Pages are required for books!");
                        return;
                    }

                    int pages = Integer.parseInt(pagesStr);
                    Book book = new Book(title, author, genre, isbn, pages);
                    manager.addBook(book);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Book added successfully!");
                } else {
                    String numberStr = numberField.getText().trim();
                    String periodicity = periodicityCombo.getValue();

                    if (numberStr.isEmpty() || periodicity == null) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Issue Number and Periodicity are required for magazines!");
                        return;
                    }

                    int number = Integer.parseInt(numberStr);
                    Magazine magazine = new Magazine(title, author, genre, number, 
                                                    Magazine.Periodicity.valueOf(periodicity));
                    manager.addMagazine(magazine);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Magazine added successfully!");
                }

                showAllDocuments();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Pages must be a valid number!");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add document: " + e.getMessage());
            }
        }
    }

    private void addDocumentCard(Document doc) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; " +
                      "-fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-border-width: 1;");

        // Basic info labels
        Label titleLabel = new Label(doc.getTitle());
        titleLabel.setPrefWidth(200);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        Label authorLabel = new Label(doc.getAuthor());
        authorLabel.setPrefWidth(150);
        authorLabel.setWrapText(true);

        Label genreLabel = new Label(doc.getGenre());
        genreLabel.setPrefWidth(120);
        genreLabel.setStyle("-fx-text-fill: #667eea; -fx-font-weight: bold;");

        Label statusLabel = new Label(doc.isAvailable() ? "✅ Available" : "📤 Borrowed");
        statusLabel.setPrefWidth(100);
        statusLabel.setStyle(doc.isAvailable() ? "-fx-text-fill: #48bb78; -fx-font-weight: bold;" : "-fx-text-fill: #ed8936; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Actions
        Button viewBtn = new Button("👁 View");
        viewBtn.setStyle("-fx-background-color: #4299e1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        viewBtn.setOnAction(e -> viewDocumentDetails(doc));

        Button editBtn = new Button("✏️ Edit");
        editBtn.setStyle("-fx-background-color: #ed8936; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        editBtn.setOnAction(e -> editDocument(doc));

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setStyle("-fx-background-color: #f56565; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteDocument(doc));

        HBox actionBox = new HBox(10, viewBtn, editBtn, deleteBtn);
        card.getChildren().addAll(titleLabel, authorLabel, genreLabel, statusLabel, spacer, actionBox);
        documentsListView.getChildren().add(card);
    }

    private void viewDocumentDetails(Document doc) {
        StringBuilder details = new StringBuilder();
        details.append("═══════════════════════════════\n");
        details.append("       DOCUMENT DETAILS\n");
        details.append("═══════════════════════════════\n\n");
        details.append("📖 Title: ").append(doc.getTitle()).append("\n");
        details.append("✍️  Author: ").append(doc.getAuthor()).append("\n");
        details.append("🎭 Genre: ").append(doc.getGenre()).append("\n");
        details.append("📊 Status: ").append(doc.isAvailable() ? "✅ Available" : "📤 Borrowed").append("\n\n");

        if (doc instanceof Book) {
            Book b = (Book) doc;
            details.append("📚 Type: Book\n");
            details.append("🔢 ISBN: ").append(b.getIsbn()).append("\n");
            details.append("📄 Pages: ").append(b.getPageNumber()).append("\n");
        } else if (doc instanceof Magazine) {
            Magazine m = (Magazine) doc;
            details.append("📰 Type: Magazine\n");
            details.append("🔢 Issue Number: ").append(m.getNumber()).append("\n");
            details.append("📅 Periodicity: ").append(m.getPeriodicity()).append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Document Details");
        alert.setHeaderText(null);
        alert.setContentText(details.toString());
        alert.getDialogPane().setMinWidth(450);
        alert.showAndWait();
    }

    private void editDocument(Document doc) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Document");
        dialog.setHeaderText("Edit document details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField(doc.getTitle());
        TextField authorField = new TextField(doc.getAuthor());
        TextField genreField = new TextField(doc.getGenre());

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Author:"), 0, 1);
        grid.add(authorField, 1, 1);
        grid.add(new Label("Genre:"), 0, 2);
        grid.add(genreField, 1, 2);

        VBox specificFieldsBox = new VBox(10);

        TextField isbnField = null, pagesField = null, numberField = null;
        ComboBox<String> periodicityCombo = null;

        if (doc instanceof Book) {
            Book book = (Book) doc;
            isbnField = new TextField(book.getIsbn());
            pagesField = new TextField(String.valueOf(book.getPageNumber()));
            specificFieldsBox.getChildren().addAll(
                    new Label("ISBN:"), isbnField,
                    new Label("Pages:"), pagesField
            );
        } else if (doc instanceof Magazine) {
            Magazine magazine = (Magazine) doc;
            numberField = new TextField(String.valueOf(magazine.getNumber()));
            periodicityCombo = new ComboBox<>();
            periodicityCombo.getItems().addAll("DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY", 
                                            "BIMONTHLY", "QUARTERLY", "YEARLY");
            periodicityCombo.setValue(magazine.getPeriodicity().toString());
            specificFieldsBox.getChildren().addAll(
                    new Label("Issue Number:"), numberField,
                    new Label("Periodicity:"), periodicityCombo
            );
        }

        grid.add(specificFieldsBox, 0, 3, 2, 1);
        dialog.getDialogPane().setContent(grid);

        // Show dialog once
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            try {
                String newTitle = titleField.getText().trim();
                String newAuthor = authorField.getText().trim();
                String newGenre = genreField.getText().trim();

                if (newTitle.isEmpty() || newAuthor.isEmpty() || newGenre.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Validation Error", "All fields are required!");
                    return;
                }

                if (doc instanceof Book) {
                    int newPages = Integer.parseInt(pagesField.getText().trim());
                    String newIsbn = isbnField.getText().trim();

                    if (newIsbn.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "ISBN is required!");
                        return;
                    }

                    Book book = (Book) doc;
                    book.setTitle(newTitle);
                    book.setAuthor(newAuthor);
                    book.setGenre(newGenre);
                    book.setIsbn(newIsbn);
                    book.setPageNumber(newPages);
                    manager.updateBook(book);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Book updated successfully!");
                } else if (doc instanceof Magazine) {
                    int newNumber = Integer.parseInt(numberField.getText().trim());
                    String newPeriodicity = periodicityCombo.getValue();
                    if (newPeriodicity == null) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Periodicity is required!");
                        return;
                    }

                    Magazine magazine = (Magazine) doc;
                    magazine.setTitle(newTitle);
                    magazine.setAuthor(newAuthor);
                    magazine.setGenre(newGenre);
                    magazine.setNumber(newNumber);
                    magazine.setPeriodicity(Magazine.Periodicity.valueOf(newPeriodicity));
                    manager.updateMagazine(magazine);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Magazine updated successfully!");
                }

                showAllDocuments();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Numeric fields must be valid numbers!");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update document: " + e.getMessage());
            }
        }
    }

    private void deleteDocument(Document doc) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Document");
        confirmAlert.setContentText("Are you sure you want to delete \"" + doc.getTitle() + "\"?\n\nThis action cannot be undone!");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (doc instanceof Book) {
                    manager.deleteBook(((Book) doc).getIsbn());
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Book deleted successfully!");
                } else if (doc instanceof Magazine) {
                    manager.deleteMagazine(((Magazine) doc).getNumber());
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Magazine deleted successfully!");
                }
                showAllDocuments();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete document: " + e.getMessage());
            }
        }
    }

    private void updateStatistics(List<Document> allDocs) {
        totalDocsLabel.setText(String.valueOf(allDocs.size()));
        long availableCount = allDocs.stream().filter(Document::isAvailable).count();
        long borrowedCount = allDocs.size() - availableCount;
        
        availableDocsLabel.setText(String.valueOf(availableCount));
        borrowedDocsLabel.setText(String.valueOf(borrowedCount));
        genresCountLabel.setText(String.valueOf(allDocs.stream().map(Document::getGenre).distinct().count()));
    }

    @FXML
    private void goBack() throws Exception {
        Stage stage = (Stage) documentsListView.getScene().getWindow();
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