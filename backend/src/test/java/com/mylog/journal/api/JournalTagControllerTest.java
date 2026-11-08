package com.mylog.journal.api;

import com.mylog.identity.application.IdentityService;
import com.mylog.journal.api.response.JournalTagResponse;
import com.mylog.journal.application.JournalTagService;
import com.mylog.journal.application.query.JournalTagView;
import com.mylog.platform.security.CurrentUser;
import com.mylog.platform.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JournalTagControllerTest {

    private JournalTagService tags;
    private CurrentUserProvider current;
    private IdentityService identity;
    private JournalTagController controller;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tags = mock(JournalTagService.class);
        current = mock(CurrentUserProvider.class);
        identity = mock(IdentityService.class);
        controller = new JournalTagController(tags, current, identity);
        userId = UUID.randomUUID();

        when(current.requireCurrent()).thenReturn(new CurrentUser(userId, Set.of("ROLE_USER")));
    }

    @Test
    @DisplayName("listForEntry - Gọi tags.listForEntry với đúng userId và entryId và map sang JournalTagResponse")
    void listForEntry_returnsTags() {
        UUID entryId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        JournalTagView tagView = new JournalTagView(tagId, "Học tập", "#3b82f6");
        when(tags.listForEntry(userId, entryId)).thenReturn(List.of(tagView));

        List<JournalTagResponse> response = controller.listForEntry(entryId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(tagId);
        assertThat(response.get(0).name()).isEqualTo("Học tập");
        assertThat(response.get(0).color()).isEqualTo("#3b82f6");
        verify(tags).listForEntry(userId, entryId);
    }
}
