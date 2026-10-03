package com.nutricare.repository;

import com.nutricare.config.Database;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Shared JDBC plumbing for the six module repositories: transactions, query helpers, parameter binding,
 * and the audit log. Every module repository extends this class.
 */
public abstract class BaseRepository {
    /** Appointment statuses that still occupy a time slot. */
    protected static final String OPEN_APPOINTMENT = "('PENDING','CONFIRMED')";

    /** Writes one audit-trail entry (design document FR05). */
    public void log(long userId, String action, String details) throws SQLException {
        try (Connection connection = Database.connect()) { log(connection, userId, action, details); }
    }

    /** Recognises unique-key violations from H2 (23505) and SQL Server (2627 / 2601). */
    public static boolean isDuplicateKey(SQLException exception) {
        int code = exception.getErrorCode();
        return "23505".equals(exception.getSQLState()) || code == 2627 || code == 2601;
    }

    /** A null that is sent with an explicit SQL type, needed for VARBINARY columns on SQL Server. */
    protected record TypedNull(int sqlType) { }

    @FunctionalInterface
    protected interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    protected static <T> T transaction(Work<T> work) throws SQLException {
        return transaction(Connection.TRANSACTION_READ_COMMITTED, work);
    }

    /** Runs the work in one transaction: commit on success, roll back on any error. */
    protected static <T> T transaction(int isolation, Work<T> work) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(isolation);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    protected static Optional<Map<String, Object>> one(String sql, Object... values) throws SQLException {
        try (Connection connection = Database.connect()) {
            return one(connection, sql, values);
        }
    }

    protected static Optional<Map<String, Object>> one(Connection connection, String sql, Object... values) throws SQLException {
        List<Map<String, Object>> rows = list(connection, sql, values);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    protected static List<Map<String, Object>> list(String sql, Object... values) throws SQLException {
        try (Connection connection = Database.connect()) {
            return list(connection, sql, values);
        }
    }

    /** Returns rows as column-label (lower case) to value maps, which the JSPs read directly. */
    protected static List<Map<String, Object>> list(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            try (ResultSet results = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData meta = results.getMetaData();
                while (results.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int column = 1; column <= meta.getColumnCount(); column++) {
                        row.put(meta.getColumnLabel(column).toLowerCase(Locale.ROOT), value(results.getObject(column)));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    /** Drivers differ in how they return date-times; the JSPs format java.util.Date values. */
    protected static Object value(Object raw) {
        return raw instanceof LocalDateTime local ? Timestamp.valueOf(local) : raw;
    }

    protected static long scalar(String sql, Object... values) throws SQLException {
        try (Connection connection = Database.connect()) {
            return count(connection, sql, values);
        }
    }

    protected static String money(String sql, Object... values) throws SQLException {
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                BigDecimal amount = results.getBigDecimal(1);
                return String.format(Locale.ROOT, "%,.2f", amount == null ? BigDecimal.ZERO : amount);
            }
        }
    }

    protected static long count(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            try (ResultSet results = statement.executeQuery()) { results.next(); return results.getLong(1); }
        }
    }

    protected static long insert(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, values);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("The database did not return a generated ID.");
                return keys.getLong(1);
            }
        }
    }

    protected static int execute(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            return statement.executeUpdate();
        }
    }

    protected static void bind(PreparedStatement statement, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof TypedNull typedNull) statement.setNull(i + 1, typedNull.sqlType());
            else statement.setObject(i + 1, values[i]);
        }
    }

    protected static void log(Connection connection, long userId, String action, String details) throws SQLException {
        String text = details != null && details.length() > 1000 ? details.substring(0, 997) + "..." : details;
        execute(connection, "INSERT INTO audit_logs(user_id,action,details) VALUES(?,?,?)", userId, action, text);
    }

    protected static Object[] withOptional(Object optional, Object... values) {
        if (optional == null) return values;
        Object[] all = Arrays.copyOf(values, values.length + 1);
        all[values.length] = optional;
        return all;
    }

    protected static boolean isClinician(String role) {
        return "DOCTOR".equals(role) || "NUTRITIONIST".equals(role);
    }

    protected static long number(Object value) {
        return ((Number) value).longValue();
    }

    protected static Timestamp timestamp(LocalDateTime time) {
        return Timestamp.valueOf(time);
    }

    protected static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
