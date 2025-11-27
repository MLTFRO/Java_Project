import dao.DatabaseManager;

public class TestDB {
    @SuppressWarnings("all")
    public static void main(String[] args) {
        DatabaseManager.getConnection();
    }
}
