package com.campus.lostandfound.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

/** Plain unit test: uses a fake DataSource, so no database is needed. */
class HealthServiceTest {

    @Test
    void reportsUpWhenConnectionIsValid() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        assertTrue(new HealthService(dataSource).isDatabaseUp());
    }

    @Test
    void reportsDownWhenConnectionFails() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("no database"));

        assertFalse(new HealthService(dataSource).isDatabaseUp());
    }
}
