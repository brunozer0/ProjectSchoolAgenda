package com.schoolagenda.service;

import com.schoolagenda.repository.NoteReadRepository;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class NoteReadPersistenceTest {
    JdbcDataSource dataSource;
    JdbcTemplate jdbc;
    NoteReadRepository repository;

    @BeforeEach void setup() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE notes(id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE users(id BIGINT PRIMARY KEY)");
        jdbc.update("INSERT INTO notes VALUES (1)");
        jdbc.update("INSERT INTO users VALUES (10), (20)");
        var migration = new ResourceDatabasePopulator(new ClassPathResource("db/note-reads.sql"));
        migration.execute(dataSource);
        migration.execute(dataSource);
        repository = new NoteReadRepository(jdbc);
    }
    @Test void repeatedMarkPreservesFirstReadAndDoesNotDuplicate() {
        assertTrue(repository.findReadAt(1L, 10L).isEmpty());
        repository.markAsRead(1L, 10L);
        var firstRead = repository.findReadAt(1L, 10L).orElseThrow();
        repository.markAsRead(1L, 10L);
        assertEquals(firstRead, repository.findReadAt(1L, 10L).orElseThrow());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM note_reads", Integer.class));
    }
    @Test void readingIsIndividualPerUser() {
        repository.markAsRead(1L, 10L);
        assertTrue(repository.findReadAt(1L, 20L).isEmpty());
        repository.markAsRead(1L, 20L);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM note_reads", Integer.class));
    }
    @Test void concurrentMarksCreateOnlyOneReceipt() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            var tasks = java.util.stream.IntStream.range(0, 8)
                    .<Callable<Void>>mapToObj(i -> () -> { repository.markAsRead(1L, 10L); return null; }).toList();
            for (var result : executor.invokeAll(tasks, 10, TimeUnit.SECONDS)) result.get();
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM note_reads", Integer.class));
        } finally { executor.shutdownNow(); }
    }
}
