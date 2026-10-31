package com.schoolagenda.service;

import com.schoolagenda.controller.NoteController;
import com.schoolagenda.dto.note.NoteReadResponse;
import com.schoolagenda.domain.enums.NoteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.schoolagenda.dto.note.NoteUpdateRequest;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class NoteHttpCompatibilityTest {
    NoteService service;
    MockMvc mvc;
    final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("professor@escola.com", "unused");
    @BeforeEach void setup() {
        service = mock(NoteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new NoteController(service)).build();
    }
    @Test void existingStudentListDoesNotMarkAsRead() throws Exception {
        when(service.listByStudent(100L, null, auth.getName())).thenReturn(List.of());
        mvc.perform(get("/api/notes/student/100").principal(auth)).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).listByStudent(100L, null, auth.getName());
        verifyNoMoreInteractions(service);
    }
    @Test void statusOnlyPatchKeepsExistingRequestContract() throws Exception {
        mvc.perform(patch("/api/notes/900").principal(auth).contentType("application/json")
                .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk());
        var request = ArgumentCaptor.forClass(NoteUpdateRequest.class);
        verify(service).update(eq(900L), request.capture(), eq(auth.getName()));
        assertEquals(NoteStatus.RESOLVED, request.getValue().getStatus());
        assertNull(request.getValue().getContent());
    }
    @Test void readReceiptUsesPutAndReturnsFirstReadTime() throws Exception {
        when(service.markAsRead(900L, auth.getName())).thenReturn(
                new NoteReadResponse(900L, true, LocalDateTime.of(2026, 1, 1, 12, 0)));
        mvc.perform(put("/api/notes/900/read").principal(auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.noteId").value(900))
                .andExpect(jsonPath("$.read").value(true)).andExpect(jsonPath("$.readAt").isString());
        verify(service).markAsRead(900L, auth.getName());
    }
    @Test void unreadStatusIsReturnedWithoutWrite() throws Exception {
        when(service.getReadStatus(900L, auth.getName())).thenReturn(new NoteReadResponse(900L, false, null));
        mvc.perform(get("/api/notes/900/read").principal(auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.read").value(false));
        verify(service).getReadStatus(900L, auth.getName());
        verifyNoMoreInteractions(service);
    }
    @Test void individualDetailDoesNotMarkAsRead() throws Exception {
        mvc.perform(get("/api/notes/900").principal(auth)).andExpect(status().isOk());
        verify(service).findById(900L, auth.getName());
        verifyNoMoreInteractions(service);
    }
}
