package scasp.db;

import java.nio.file.*;
import java.sql.*;

/** Connection factory. In a Jakarta EE runtime, replace DriverManager with the container's pooled DataSource. */
public class Db {
    private final String url;
    public Db(String url) { this.url = url; }

    public Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) { s.execute("PRAGMA foreign_keys=ON"); }
        return c;
    }

    public void runScript(Path file) throws Exception {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            String text = String.join("\n", Files.readAllLines(file).stream().filter(l -> !l.trim().startsWith("--")).toList());
            for (String sql : text.split(";")) if (!sql.isBlank()) s.execute(sql.trim());
        }
    }
}
