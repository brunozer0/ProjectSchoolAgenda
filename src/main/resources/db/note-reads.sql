CREATE TABLE IF NOT EXISTS note_reads (
    note_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at DATETIME(6) NOT NULL,
    PRIMARY KEY (note_id, user_id),
    CONSTRAINT fk_note_reads_note FOREIGN KEY (note_id) REFERENCES notes(id),
    CONSTRAINT fk_note_reads_user FOREIGN KEY (user_id) REFERENCES users(id)
);
