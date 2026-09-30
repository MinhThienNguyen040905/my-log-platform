package com.mylog.journal.application;

import com.mylog.journal.application.query.JournalTagView;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class JournalTagService {
    private final JournalTagStore store;
    private final Clock clock;

    public JournalTagService(JournalTagStore store, Clock clock) { this.store = store; this.clock = clock; }

    @Transactional
    public JournalTagView create(UUID userId, String name, String color) {
        if (name == null) throw new InvalidRequestException();
        String trimmed = Normalizer.normalize(name.trim(), Normalizer.Form.NFKC);
        if (trimmed.isBlank() || trimmed.codePointCount(0, trimmed.length()) > 40
                || color != null && !color.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))
            throw new InvalidRequestException();
        return store.create(userId, trimmed, trimmed.toLowerCase(Locale.ROOT), color, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<JournalTagView> list(UUID userId) { return store.list(userId); }

    @Transactional
    public void attach(UUID userId, UUID entryId, UUID tagId) {
        if (!store.attach(userId, entryId, tagId, clock.instant()))
            throw new ResourceNotFoundException("Không tìm thấy nhật ký hoặc tag.");
    }

    @Transactional
    public void detach(UUID userId, UUID entryId, UUID tagId) {
        if (!store.detach(userId, entryId, tagId))
            throw new ResourceNotFoundException("Không tìm thấy nhật ký hoặc tag.");
    }
}
