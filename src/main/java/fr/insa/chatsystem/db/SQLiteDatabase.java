package fr.insa.chatsystem.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles SQLite connection and schema initialization.
 */
public class SQLiteDatabase {

    private static final String DB_URL = "jdbc:sqlite:chat.db";
    private final Connection connection;

    public SQLiteDatabase(String session) throws SQLException {
        String db_path = "jdbc:sqlite:chat"+session+".db";
        this.connection = DriverManager.getConnection(db_path);
        initSchema();
    }

    private void initSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {

            // Stores remote contacts (identified by user_id entered at startup)
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS contact (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id TEXT UNIQUE NOT NULL,
                    username TEXT NOT NULL
                );
            """);

            // Stores message history (1-to-1 conversations)
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS message (
                    id TEXT PRIMARY KEY,
                    contact_id TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    direction TEXT NOT NULL,
                    reaction TEXT NOT NULL,
                    FOREIGN KEY(contact_id) REFERENCES contact(id)
                );
            """);
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
