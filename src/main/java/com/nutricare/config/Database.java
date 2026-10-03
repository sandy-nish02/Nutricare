package com.nutricare.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens JDBC connections. NutriCare runs on Microsoft SQL Server Express; H2 is only used by the
 * automated tests (DB_MODE=h2).
 *
 * <p>SQL Server sign-in: when DB_PASSWORD is set, SQL authentication is used with DB_USER (default
 * {@code sa}). Otherwise Windows authentication is used, so each team member can run the app with
 * their own Windows login and no password is stored anywhere.</p>
 */
public final class Database {
    static final String DEFAULT_SQLSERVER_URL =
            "jdbc:sqlserver://localhost:1433;databaseName=NutriCare;encrypt=false;trustServerCertificate=true";
    static final String DEFAULT_H2_URL =
            "jdbc:h2:file:./data/nutricare;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE";

    private Database() { }

    public static Connection connect() throws SQLException {
        String mode = mode();
        String password = setting("db.password", "DB_PASSWORD", "");
        return switch (mode) {
            case "sqlserver" -> {
                String url = setting("db.url", "DB_URL", DEFAULT_SQLSERVER_URL);
                Connection connection = password.isBlank()
                        ? DriverManager.getConnection(windowsAuthentication(url))
                        : DriverManager.getConnection(url, setting("db.user", "DB_USER", "sa"), password);
                yield markAsApplication(connection);
            }
            case "h2" -> DriverManager.getConnection(setting("db.url", "DB_URL", DEFAULT_H2_URL),
                    setting("db.user", "DB_USER", "sa"), password);
            default -> throw new SQLException("Unsupported database mode: " + mode + ". Use sqlserver.");
        };
    }

    public static String mode() {
        return setting("db.mode", "DB_MODE", "sqlserver").trim().toLowerCase();
    }

    public static boolean isSqlServer() {
        return "sqlserver".equals(mode());
    }

    /**
     * Tags the session so the appointment trigger knows NutriCare already wrote its own audit entry (with the
     * signed-in user and reason). The trigger then only records changes made outside the app, e.g. in SSMS.
     */
    private static Connection markAsApplication(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("EXEC sp_set_session_context @key = N'nutricare_app', @value = 1");
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }

    static String windowsAuthentication(String url) {
        return url.toLowerCase().contains("integratedsecurity=") ? url : url + ";integratedSecurity=true";
    }

    private static String setting(String property, String environment, String fallback) {
        String systemValue = System.getProperty(property);
        if (systemValue != null) return systemValue;
        String environmentValue = System.getenv(environment);
        return environmentValue == null || environmentValue.isBlank() ? fallback : environmentValue;
    }
}
