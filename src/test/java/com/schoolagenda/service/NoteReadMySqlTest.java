package com.schoolagenda.service;

import com.schoolagenda.repository.NoteReadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.*;

/** Runs only against the disposable MySQL database supplied by local/CI tests. */
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = ".+")
class NoteReadMySqlTest {
    @Test
    void initializesAgainstUnsignedParentKeysWithoutChangingExistingData() {
        var source = new DriverManagerDataSource(System.getenv("MYSQL_TEST_URL"),
                System.getenv("MYSQL_TEST_USER"), System.getenv("MYSQL_TEST_PASSWORD"));
        var jdbc = new JdbcTemplate(source);
        // The configured database must be disposable: these are test fixtures.
        jdbc.execute("DROP TABLE IF EXISTS note_reads");
        jdbc.execute("DROP TABLE IF EXISTS notes");
        jdbc.execute("DROP TABLE IF EXISTS users");
        jdbc.execute("CREATE TABLE notes(id BIGINT UNSIGNED PRIMARY KEY)");
        jdbc.execute("CREATE TABLE users(id BIGINT UNSIGNED PRIMARY KEY)");
        jdbc.update("INSERT INTO notes VALUES (1)");
        jdbc.update("INSERT INTO users VALUES (10)");

        var migration = new ResourceDatabasePopulator(new ClassPathResource("db/note-reads.sql"));
        migration.execute(source);
        var repository = new NoteReadRepository(jdbc);
        repository.markAsRead(1L, 10L);
        var firstRead = repository.findReadAt(1L, 10L).orElseThrow();
        migration.execute(source);
        repository.markAsRead(1L, 10L);
        assertEquals(firstRead, repository.findReadAt(1L, 10L).orElseThrow());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notes", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM note_reads", Integer.class));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> repository.markAsRead(999L, 10L));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> repository.markAsRead(1L, 999L));
    }
}
