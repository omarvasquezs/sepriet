/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Forms;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 *
 * @author omarv
 */
public class DatabaseConfig {

    private static final String PROPS_FILE = "db_settings.properties";
    private static final Properties props = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream(PROPS_FILE)) {
            props.load(fis);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error loading " + PROPS_FILE + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns a new Connection using settings from db_settings.properties.
     *
     * @return 
     * @throws SQLException if connection fails
     * @throws IllegalStateException if any property is missing or empty
     */
    public static Connection getConnection() throws SQLException {
        String host = props.getProperty("db.host");
        String port = props.getProperty("db.port");
        String database = props.getProperty("db.database");
        String user = props.getProperty("db.username");
        String pass = props.getProperty("db.password");

        if (host == null || host.isBlank()
                || port == null || port.isBlank()
                || database == null || database.isBlank()
                || user == null || user.isBlank()
                || pass == null) {
            throw new IllegalStateException(
                    "db_settings.properties incompleto: configurar db.host, db.port, "
                    + "db.database, db.username, db.password");
        }

        String url = "jdbc:mariadb://" + host + ":" + port + "/" + database;
        return DriverManager.getConnection(url, user, pass);
    }
}
