package com.mylog.checkin.application;

import com.mylog.checkin.application.command.PutCheckinCommand;
import com.mylog.checkin.application.query.CheckinView;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class CheckinService {
    private final CheckinStore store;
    private final Clock clock;

    public CheckinService(CheckinStore store, Clock clock) { this.store = store; this.clock = clock; }

    @Transactional
    public CheckinView put(UUID userId, LocalDate date, PutCheckinCommand command) {
        if (date == null || command == null || command.timezone() == null || command.timezone().length() > 64
                || date.isAfter(LocalDate.now(clock).plusDays(1))
                || command.moodCode() != null && !command.moodCode().matches("[a-z0-9-]{1,32}")
                || !score(command.moodScore()) || !score(command.stressScore()) || !score(command.energyScore())
                || command.sleepMinutes() != null && (command.sleepMinutes() < 0 || command.sleepMinutes() > 1440)
                || command.note() != null && command.note().length() > 4000
                || command.activities() != null && command.activities().size() > 20) throw new InvalidRequestException();
        try { ZoneId.of(command.timezone()); }
        catch (RuntimeException e) { throw new InvalidRequestException(); }
        var seen = new HashSet<String>();
        if (command.activities() != null) for (var activity : command.activities()) {
            if (activity == null || activity.code() == null || !activity.code().matches("[a-z0-9-]{1,40}")
                    || !seen.add(activity.code()) || activity.durationMinutes() != null
                    && (activity.durationMinutes() < 0 || activity.durationMinutes() > 1440)
                    || activity.intensity() != null && !List.of("LOW", "MODERATE", "HIGH").contains(activity.intensity()))
                throw new InvalidRequestException();
        }
        return store.put(userId, date, command, clock.instant());
    }

    @Transactional(readOnly = true)
    public CheckinView get(UUID userId, LocalDate date) {
        return store.find(userId, date).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy check-in."));
    }

    @Transactional(readOnly = true)
    public List<CheckinView> list(UUID userId, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || from.plusDays(366).isBefore(to))
            throw new InvalidRequestException();
        return store.list(userId, from, to);
    }

    private static boolean score(BigDecimal value) {
        return value == null || value.scale() <= 1 && value.compareTo(BigDecimal.ONE) >= 0
                && value.compareTo(BigDecimal.TEN) <= 0;
    }
}
