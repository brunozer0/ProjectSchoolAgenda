package com.schoolagenda.dto.note;

import java.time.LocalDateTime;

public record NoteReadResponse(Long noteId, boolean read, LocalDateTime readAt) { }
