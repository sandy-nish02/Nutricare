package com.nutricare.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Creates missing tables and upgrades older NutriCare databases. Safe to run on every start. */
public final class DatabaseSchema {
    /** Tables first, then the database objects that depend on them. */
    private static final List<String> SQL_SERVER_SCRIPTS = List.of(
            "/schema-sqlserver.sql", "/sqlserver-procedure-payments.sql", "/sqlserver-trigger-appointments.sql");
    private static final List<String> H2_SCRIPTS = List.of("/schema.sql");

    private DatabaseSchema() { }

    public static void initialize() throws SQLException, IOException {
        boolean sqlServer = Database.isSqlServer();
        try (Connection connection = Database.connect(); Statement statement = connection.createStatement()) {
            for (String resource : sqlServer ? SQL_SERVER_SCRIPTS : H2_SCRIPTS) {
                String sql = read(resource);
                // SQL Server scripts use SSMS-style GO separators because procedures and triggers contain semicolons.
                String[] batches = sqlServer ? sql.split("(?im)^\\s*GO\\s*$") : sql.split(";");
                for (String batch : batches) {
                    if (containsCode(batch)) statement.execute(batch.trim());
                }
            }
        }
    }

    private static String read(String resource) throws IOException {
        try (InputStream input = DatabaseSchema.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException(resource + " is missing");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static boolean containsCode(String batch) {
        return batch.lines().map(String::trim).anyMatch(line -> !line.isEmpty() && !line.startsWith("--"));
    }
}
