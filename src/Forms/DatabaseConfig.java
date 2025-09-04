/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Forms;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author omarv
 */
public class DatabaseConfig {

    private static final String JSON_FILE = "db_settings.json";

    /**
     * Returns a new Connection using settings from db_settings.json (JSON-only).
     *
     * @return a live SQL Connection
     * @throws SQLException if connection fails
     * @throws IllegalStateException if the JSON file is missing or contains invalid/missing fields
     */
    public static Connection getConnection() throws SQLException {
        java.nio.file.Path jsonPath = java.nio.file.Paths.get(JSON_FILE);
        if (!java.nio.file.Files.exists(jsonPath)) {
            throw new IllegalStateException(JSON_FILE + " no encontrado. Asegúrate de crear " + JSON_FILE);
        }

        String json;
        try {
            json = new String(java.nio.file.Files.readAllBytes(jsonPath), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo " + JSON_FILE + ": " + e.getMessage(), e);
        }

        String host = extract(json, "db.host");
        String port = extract(json, "db.port");
        String database = extract(json, "db.database");
        String user = extract(json, "db.username");
        String pass = extract(json, "db.password");

        if (isEmpty(host) || isEmpty(port) || isEmpty(database) || isEmpty(user) || pass == null) {
            throw new IllegalStateException(JSON_FILE + " incompleto: configurar db.host, db.port, db.database, db.username, db.password");
        }

        String url = "jdbc:mariadb://" + host + ":" + port + "/" + database;
        return DriverManager.getConnection(url, user, pass);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }

    private static String extract(String json, String key) {
        if (json == null) return null;
        String pattern = "\\\"" + java.util.regex.Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) return m.group(1);
        return null;
    }
}
