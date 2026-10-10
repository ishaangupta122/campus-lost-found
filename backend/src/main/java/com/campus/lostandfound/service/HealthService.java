package com.campus.lostandfound.service;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

/**
 * Checks whether the application can reach PostgreSQL.
 *
 * Demonstrates constructor-based dependency injection: Spring creates the
 * DataSource (from application.properties) and passes it to this constructor.
 */
@Service
public class HealthService {

    private static final int TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    public HealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isDatabaseUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(TIMEOUT_SECONDS);
        } catch (SQLException e) {
            return false;
        }
    }
}
