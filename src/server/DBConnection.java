package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static String url;
    private static String user;
    private static String password;

    static {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            props.load(fis);
            url = props.getProperty("db.url");
            user = props.getProperty("db.user");
            password = props.getProperty("db.password");
            
            // Explicitly load the PostgreSQL driver class
            Class.forName("org.postgresql.Driver");
        } catch (IOException e) {
            System.err.println("CRITICAL: Could not load db.properties file. Ensure it is in the working directory.");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL: PostgreSQL JDBC Driver not found in classpath.");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        if (url == null || user == null || password == null) {
            throw new SQLException("Database connection parameters are not initialized properly in db.properties.");
        }
        return DriverManager.getConnection(url, user, password);
    }
}
