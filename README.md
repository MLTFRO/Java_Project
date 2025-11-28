# Library Management System

A comprehensive Java-based library management application built with JavaFX, Maven, and SQLite. This system allows librarians to efficiently manage books, magazines, members, and borrowing operations with an intuitive graphical interface.

## Features

### Document Management
- **Add, update, and remove** books and magazines
- **Search** documents by title, author, genre, or ISBN
- **Track availability** automatically based on active borrows
- Support for both **Books** (with ISBN and page count) and **Magazines** (with issue number and periodicity)

### Member Management
- **Register new members** with unique IDs
- **Update member information** (name, surname, penalty status)
- **Track active borrows** per member (max 5 simultaneous borrows)
- **Penalty system** with three levels: GOOD, SUSPENDED, BANNED
- **Automatic penalty calculation** based on overdue items

### Borrowing System
- **Create new borrows** with automatic 14-day loan period
- **Track active borrows** and overdue items
- **Return documents** with automatic penalty calculation ($0.50 per day late)
- **View borrow history** with all returned documents
- **Delete borrow records** from history
- **Real-time statistics** dashboard showing:
  - Active borrows count
  - Overdue items count
  - Documents returned today
  - Total penalties owed

### User Interface
- Modern, clean JavaFX interface with intuitive navigation
- **Color-coded status indicators**:
  - Green for available/on-time
  - Yellow for warnings (3 days until due)
  - Red for overdue/unavailable
- **Tabbed interface** for different sections (Active Borrows, Overdue, History)
- **Interactive cards** for each document and borrow with action buttons
- **Real-time updates** after each operation

## Technology Stack

- **Java 17** - Core programming language
- **JavaFX 21** - GUI framework
- **Maven 3.9+** - Build and dependency management
- **SQLite 3.44** - Embedded database
- **JDBC** - Database connectivity
- **DAO Pattern** - Data access layer architecture
- **MVC Pattern** - Model-View-Controller design

## Prerequisites

Before running this project, ensure you have the following installed:

1. **Java Development Kit (JDK) 17 or higher**
   ```bash
   java -version
   # Should output: java version "17.x.x" or higher
   ```

2. **Apache Maven 3.9 or higher**
   ```bash
   mvn -version
   # Should output: Apache Maven 3.9.x or higher
   ```

3. **Git** (optional, for cloning)
   ```bash
   git --version
   ```

## Installation

### Option 1: Clone from Git (if available)
```bash
git clone <repository-url>
cd library-management-system
```

### Option 2: Extract from ZIP
```bash
unzip library-management-system.zip
cd library-management-system
```

## Running the Project

### Method 1: Using Maven (Recommended)

1. **Clean and compile the project:**
   ```bash
   mvn clean compile
   ```

2. **Run the application:**
   ```bash
   mvn javafx:run
   ```

### Method 2: Using Maven Wrapper (if included)

1. **On Linux/Mac:**
   ```bash
   ./mvnw clean javafx:run
   ```

2. **On Windows:**
   ```bash
   mvnw.cmd clean javafx:run
   ```

### Method 3: Package as JAR and Run

1. **Build the JAR:**
   ```bash
   mvn clean package
   ```

2. **Run the JAR:**
   ```bash
   java -jar target/library-app-1.0-SNAPSHOT.jar
   ```

## 📁 Project Structure

```
library-management-system/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/libman/
│   │   │       ├── dao/              # Data Access Objects
│   │   │       │   ├── BorrowDAO.java
│   │   │       │   ├── BorrowDAOImpl.java
│   │   │       │   ├── DocumentDAO.java
│   │   │       │   ├── DocumentDAOImpl.java
│   │   │       │   ├── MemberDAO.java
│   │   │       │   ├── MemberDAOImpl.java
│   │   │       │   ├── DatabaseManager.java
│   │   │       │   └── LibraryManagerDAO.java
│   │   │       ├── model/            # Domain Models
│   │   │       │   ├── Book.java
│   │   │       │   ├── Magazine.java
│   │   │       │   ├── Document.java
│   │   │       │   ├── Member.java
│   │   │       │   └── Borrow.java
│   │   │       ├── exception/        # Custom Exceptions
│   │   │       │   ├── BorrowException.java
│   │   │       │   ├── DocumentNotFoundException.java
│   │   │       │   └── MemberNotFoundException.java
│   │   │       └── ui/
│   │   │           └── controller/   # JavaFX Controllers
│   │   │               ├── MainController.java
│   │   │               ├── DocumentsController.java
│   │   │               ├── MembersController.java
│   │   │               └── BorrowsController.java
│   │   └── resources/
│   │       └── ui/                   # FXML Files
│   │           ├── main-view.fxml
│   │           ├── documents-view.fxml
│   │           ├── members-view.fxml
│   │           └── borrows-view.fxml
│   └── test/
│       └── java/                     # Unit Tests (if any)
├── pom.xml                           # Maven configuration
├── library.db                        # SQLite database (auto-generated)
└── README.md
```

## Database Schema

The application uses SQLite with the following tables:

### Document Table
```sql
CREATE TABLE Document (
    id_doc INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    genre TEXT NOT NULL
);
```

### Book Table
```sql
CREATE TABLE Book (
    id_doc INTEGER PRIMARY KEY,
    title TEXT,
    isbn TEXT UNIQUE,
    pageNumber INTEGER,
    author TEXT,
    genre TEXT,
    FOREIGN KEY (id_doc) REFERENCES Document(id_doc)
);
```

### Magazine Table
```sql
CREATE TABLE Magazine (
    id_doc INTEGER PRIMARY KEY,
    title TEXT,
    author TEXT,
    genre TEXT,
    number INTEGER,
    periodicity TEXT,
    FOREIGN KEY (id_doc) REFERENCES Document(id_doc)
);
```

### Member Table
```sql
CREATE TABLE Member (
    idMember INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    surname TEXT NOT NULL,
    nbBorrows INTEGER DEFAULT 0,
    penaltyStatus TEXT DEFAULT 'GOOD'
);
```

### Borrow Table
```sql
CREATE TABLE Borrow (
    id TEXT PRIMARY KEY,
    id_doc INTEGER NOT NULL,
    idMember INTEGER NOT NULL,
    borrowDate DATE NOT NULL,
    expectedReturnDate DATE NOT NULL,
    returnDate DATE,
    FOREIGN KEY (id_doc) REFERENCES Document(id_doc),
    FOREIGN KEY (idMember) REFERENCES Member(idMember)
);
```

## Usage Guide

### 1. Starting the Application
When you run the application, you'll see the main dashboard with navigation to three main sections:
- **Documents** - Manage books and magazines
- **Members** - Manage library members
- **Borrows** - Handle borrowing operations

### 2. Managing Documents

#### Adding a Document:
1. Navigate to **Documents** section
2. Fill in the document details (title, author, genre)
3. For books: provide ISBN and page count
4. For magazines: provide issue number and periodicity
5. Click **Add Document**

#### Searching Documents:
- Use the search bar with filters (Title, Author, Genre, ISBN)
- Click **Search** to find documents
- Results display availability status

#### Updating/Removing Documents:
- Search for the document first
- Use **Update** or **Remove** buttons on the document card
- Documents currently borrowed cannot be removed

### 3. Managing Members

#### Registering a Member:
1. Navigate to **Members** section
2. Enter name and surname
3. Click **Add Member**
4. System auto-generates unique member ID

#### Updating Member Status:
- Search for the member
- Click **Update** and change penalty status if needed
- Status options: GOOD, SUSPENDED, BANNED

### 4. Managing Borrows

#### Creating a Borrow:
1. Navigate to **Borrows** section
2. Enter member ID and click **Search Member**
3. Enter document title and click **Search Document**
4. Verify both are found (green checkmarks)
5. Select borrow date (defaults to today)
6. Click **Borrow Document**

**Validation Rules:**
- Member must not be SUSPENDED or BANNED
- Member cannot have more than 5 active borrows
- Document must be available (not currently borrowed)

#### Returning a Document:
1. Find the borrow in **Active Borrows** or **Overdue** tab
2. Click **Return** button
3. Confirm the return
4. System calculates penalties if overdue ($0.50/day)
5. Document automatically becomes available again

#### Deleting Borrow Records:
1. Navigate to **History** tab
2. Find the returned borrow
3. Click **Delete** button
4. Confirm permanent deletion

## Configuration

### Database Location
The SQLite database file (`library.db`) is created automatically in the project root directory on first run.

To change the database location, modify `DatabaseManager.java`:
```java
private static final String DB_URL = "jdbc:sqlite:your/custom/path/library.db";
```

### Loan Period
Default loan period is 14 days. To change it, modify `Borrow.java`:
```java
this.expectedReturnDate = borrowDate.plusDays(14); // Change 14 to desired days
```

### Penalty Rate
Default penalty is $0.50 per day. To change it, modify calculations in `BorrowsController.java`:
```java
double penalty = daysOverdue * 0.5; // Change 0.5 to desired rate
```

### Maximum Borrows per Member
Default limit is 5 simultaneous borrows. To change it, modify `BorrowsController.java`:
```java
if (selectedMember.getNbBorrows() >= 5) // Change 5 to desired limit
```

## Troubleshooting

### Issue: "JavaFX runtime components are missing"
**Solution:** Ensure you're using the JavaFX Maven plugin to run:
```bash
mvn javafx:run
```
Do NOT use `java -jar` directly as JavaFX modules need special handling.

### Issue: Database locked error
**Solution:** Close any SQLite database browsers/tools that might have the database open.

### Issue: "Failed to parse date" errors
**Solution:** The database stores dates in multiple formats. The system handles this automatically, but if issues persist, delete `library.db` and let it regenerate.

### Issue: Maven build fails
**Solution:**
1. Clean the project: `mvn clean`
2. Update Maven: `mvn -U clean install`
3. Check Java version: `java -version` (must be 17+)

### Issue: UI components not displaying
**Solution:** Verify FXML files are in `src/main/resources/ui/` directory and paths in controllers match exactly.

## Testing

Run unit tests (if available):
```bash
mvn test
```

Run with verbose output:
```bash
mvn clean javafx:run -X
```

## Building for Distribution

Create a standalone JAR:
```bash
mvn clean package
```

The JAR will be in `target/library-app-1.0-SNAPSHOT.jar`

## 🔄 Sample Data

On first run, the system creates sample data:
- **3 sample books** (1984, To Kill a Mockingbird, The Great Gatsby)
- **2 sample magazines** (National Geographic, Time Magazine)
- **2 sample members** (John Doe, Jane Smith)
- **2 sample borrows** (one active, one overdue)

To reset to fresh database, simply delete `library.db` and restart the application.

## 📝 Development Notes

### Design Patterns Used
- **DAO (Data Access Object)** - Separates business logic from data persistence
- **MVC (Model-View-Controller)** - Separates UI from business logic
- **Singleton** - DatabaseManager ensures single connection
- **Factory** - Document creation for Books/Magazines

### Key Classes
- `DatabaseManager` - Manages SQLite connection
- `LibraryManagerDAO` - Facade for all DAO operations
- `BorrowDAOImpl` - Handles all borrow CRUD operations
- `DocumentDAOImpl` - Manages documents with inheritance (Book/Magazine)
- `MemberDAOImpl` - Member operations and penalty tracking

## Contributing

To contribute to this project:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is created for educational purposes.

## Authors

Suvomita Roy & Théo Focsa

## Acknowledgments

- JavaFX Documentation
- SQLite Documentation
- Maven Central Repository

---

**Version:** 1.0.0  
**Last Updated:** November 2025  

For questions or issues, please open an issue on the repository or contact the development team.
