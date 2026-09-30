package com.mylog.checkin.infrastructure.persistence;

import com.mylog.checkin.application.CheckinStore;
import com.mylog.checkin.application.command.PutCheckinCommand;
import com.mylog.checkin.application.query.CheckinView;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaCheckinStore implements CheckinStore {
    private final EntityManager em;
    private final IdGenerator ids;
    private final SensitiveDataCipher cipher;

    JpaCheckinStore(EntityManager em, IdGenerator ids, SensitiveDataCipher cipher) {
        this.em = em; this.ids = ids; this.cipher = cipher;
    }

    @Override public CheckinView put(UUID userId, LocalDate date, PutCheckinCommand command, Instant now) {
        var checkin = locked(userId, date).orElse(null);
        if (checkin == null) {
            em.createNativeQuery("""
                    INSERT INTO daily_checkins(id,user_id,local_date,timezone,source,created_at,updated_at,row_version)
                    VALUES (?1,?2,?3,?4,'USER',?5,?5,0)
                    ON CONFLICT (user_id,local_date) DO NOTHING
                    """).setParameter(1, ids.next()).setParameter(2, userId).setParameter(3, date)
                    .setParameter(4, command.timezone()).setParameter(5, now).executeUpdate();
            checkin = locked(userId, date).orElseThrow();
        }
        checkin.timezone = command.timezone();
        checkin.moodCode = command.moodCode();
        checkin.moodScore = command.moodScore();
        checkin.stressScore = command.stressScore();
        checkin.energyScore = command.energyScore();
        checkin.sleepMinutes = command.sleepMinutes() == null ? null : command.sleepMinutes().shortValue();
        if (command.note() == null || command.note().isBlank()) {
            checkin.encryptedNote = null; checkin.noteIv = null;
            checkin.noteWrappedKey = null; checkin.noteKeyVersion = null;
        } else {
            var encrypted = cipher.encrypt("daily_checkins.note", userId, checkin.id, command.note());
            checkin.encryptedNote = encrypted.ciphertext();
            checkin.noteIv = encrypted.iv();
            checkin.noteWrappedKey = encrypted.wrappedKey();
            checkin.noteKeyVersion = encrypted.keyVersion();
        }
        checkin.source = "USER";
        checkin.updatedAt = now;
        checkin.rowVersion++;
        em.createQuery("delete from CheckinActivity a where a.checkinId=:id")
                .setParameter("id", checkin.id).executeUpdate();
        if (command.activities() != null) for (var item : command.activities()) {
            var activity = new CheckinActivity();
            activity.id = ids.next(); activity.checkinId = checkin.id;
            activity.activityCode = item.code();
            activity.durationMinutes = item.durationMinutes() == null ? null : item.durationMinutes().shortValue();
            activity.intensity = item.intensity(); activity.createdAt = now;
            em.persist(activity);
        }
        em.flush();
        em.clear();
        return find(userId, date).orElseThrow();
    }

    @Override public Optional<CheckinView> find(UUID userId, LocalDate date) {
        return em.createQuery("select c from DailyCheckin c where c.userId=:userId and c.localDate=:date", DailyCheckin.class)
                .setParameter("userId", userId).setParameter("date", date).getResultStream().findFirst()
                .map(this::view);
    }

    @Override public List<CheckinView> list(UUID userId, LocalDate from, LocalDate to) {
        return em.createQuery("""
                select c from DailyCheckin c where c.userId=:userId and c.localDate between :from and :to
                order by c.localDate desc
                """, DailyCheckin.class).setParameter("userId", userId)
                .setParameter("from", from).setParameter("to", to).getResultList().stream().map(this::view).toList();
    }

    private Optional<DailyCheckin> locked(UUID userId, LocalDate date) {
        return em.createQuery("select c from DailyCheckin c where c.userId=:userId and c.localDate=:date", DailyCheckin.class)
                .setParameter("userId", userId).setParameter("date", date)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
    }

    private CheckinView view(DailyCheckin c) {
        String note = c.encryptedNote == null ? null : cipher.decrypt("daily_checkins.note", c.userId, c.id,
                new SensitiveDataCipher.Encrypted(c.encryptedNote, c.noteIv, c.noteWrappedKey, c.noteKeyVersion));
        var activities = em.createQuery("select a from CheckinActivity a where a.checkinId=:id order by a.activityCode", CheckinActivity.class)
                .setParameter("id", c.id).getResultList().stream()
                .map(a -> new CheckinView.ActivityView(a.activityCode,
                        a.durationMinutes == null ? null : a.durationMinutes.intValue(), a.intensity)).toList();
        return new CheckinView(c.id, c.localDate, c.timezone, c.moodCode, c.moodScore, c.stressScore,
                c.energyScore, c.sleepMinutes == null ? null : c.sleepMinutes.intValue(),
                note, activities, c.rowVersion, c.createdAt, c.updatedAt);
    }
}
