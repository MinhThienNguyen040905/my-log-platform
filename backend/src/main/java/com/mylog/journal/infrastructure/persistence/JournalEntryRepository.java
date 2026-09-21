package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.domain.JournalEntry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @Query("""
            select entry from JournalEntry entry
            where entry.userId = :userId
              and entry.deletedAt is null
              and entry.entryDate >= :fromDate
              and entry.entryDate <= :toDate
              and (entry.createdAt < :cursorCreatedAt
                   or (entry.createdAt = :cursorCreatedAt and entry.id < :cursorId))
            order by entry.createdAt desc, entry.id desc
            """)
    List<JournalEntry> findHistory(
            @Param("userId") UUID userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);
}
