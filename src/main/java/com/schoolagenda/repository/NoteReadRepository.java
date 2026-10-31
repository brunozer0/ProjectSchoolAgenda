package com.schoolagenda.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NoteReadRepository {
    private final JdbcTemplate jdbcTemplate;

    public LocalDateTime markAsRead(Long noteId, Long userId) {
        // The unique key preserves the first read time, including concurrent requests.
        jdbcTemplate.update("""
                INSERT INTO note_reads (note_id, user_id, read_at)
                VALUES (?, ?, CURRENT_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE read_at = read_at
                """, noteId, userId);
        // A locking read sees the winning insert even with MySQL REPEATABLE READ.
        return jdbcTemplate.queryForObject(
                "SELECT read_at FROM note_reads WHERE note_id = ? AND user_id = ? FOR UPDATE",
                (rs, rowNum) -> rs.getTimestamp("read_at").toLocalDateTime(), noteId, userId);
    }

    public Optional<LocalDateTime> findReadAt(Long noteId, Long userId) {
        return jdbcTemplate.query("SELECT read_at FROM note_reads WHERE note_id = ? AND user_id = ?",
                (rs, rowNum) -> rs.getTimestamp("read_at").toLocalDateTime(), noteId, userId)
                .stream().findFirst();
    }
}
