package com.mylog.selfcare.infrastructure.persistence;

import com.mylog.platform.crypto.SensitiveDataCipher.Encrypted;
import com.mylog.selfcare.application.SelfCareStore;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaSelfCareStore implements SelfCareStore {
    private final EntityManager em;
    JpaSelfCareStore(EntityManager em) { this.em = em; }

    public void createGoal(Goal goal) {
        SelfCareGoal row = new SelfCareGoal();
        row.id = goal.id(); row.userId = goal.userId(); row.category = goal.category();
        row.encryptedTitle = goal.title().ciphertext(); row.titleIv = goal.title().iv();
        row.titleWrappedKey = goal.title().wrappedKey(); row.titleKeyVersion = goal.title().keyVersion();
        if (goal.description() != null) {
            row.encryptedDescription = goal.description().ciphertext(); row.descriptionIv = goal.description().iv();
            row.descriptionWrappedKey = goal.description().wrappedKey(); row.descriptionKeyVersion = goal.description().keyVersion();
        }
        row.status = goal.status(); row.startDate = goal.startDate(); row.targetDate = goal.targetDate();
        row.createdAt = goal.createdAt(); row.updatedAt = goal.updatedAt(); row.completedAt = goal.completedAt();
        row.rowVersion = goal.version(); em.persist(row);
    }
    public Optional<Goal> goal(UUID userId, UUID id) {
        return em.createQuery("select g from SelfCareGoal g where g.userId=:user and g.id=:id", SelfCareGoal.class)
                .setParameter("user", userId).setParameter("id", id).getResultStream().findFirst().map(this::goal);
    }
    public List<Goal> goals(UUID userId) {
        return em.createQuery("select g from SelfCareGoal g where g.userId=:user order by g.createdAt desc, g.id desc", SelfCareGoal.class)
                .setParameter("user", userId).getResultList().stream().map(this::goal).toList();
    }
    public boolean updateGoal(Goal goal, long expectedVersion) {
        int changed = em.createQuery("update SelfCareGoal g set g.encryptedTitle=:title, g.titleIv=:titleIv, "
                        + "g.titleWrappedKey=:titleKey, g.titleKeyVersion=:titleVersion, "
                        + "g.encryptedDescription=:description, g.descriptionIv=:descriptionIv, "
                        + "g.descriptionWrappedKey=:descriptionKey, g.descriptionKeyVersion=:descriptionVersion, "
                        + "g.status=:status, g.startDate=:startDate, g.targetDate=:targetDate, "
                        + "g.completedAt=:completedAt, g.updatedAt=:updatedAt, g.rowVersion=:next "
                        + "where g.userId=:user and g.id=:id and g.rowVersion=:expected")
                .setParameter("title", goal.title().ciphertext()).setParameter("titleIv", goal.title().iv())
                .setParameter("titleKey", goal.title().wrappedKey()).setParameter("titleVersion", goal.title().keyVersion())
                .setParameter("description", goal.description() == null ? null : goal.description().ciphertext())
                .setParameter("descriptionIv", goal.description() == null ? null : goal.description().iv())
                .setParameter("descriptionKey", goal.description() == null ? null : goal.description().wrappedKey())
                .setParameter("descriptionVersion", goal.description() == null ? null : goal.description().keyVersion())
                .setParameter("status", goal.status()).setParameter("startDate", goal.startDate())
                .setParameter("targetDate", goal.targetDate()).setParameter("completedAt", goal.completedAt())
                .setParameter("updatedAt", goal.updatedAt()).setParameter("next", expectedVersion + 1)
                .setParameter("user", goal.userId()).setParameter("id", goal.id())
                .setParameter("expected", expectedVersion).executeUpdate();
        em.clear();
        return changed == 1;
    }
    public void createHabit(Habit habit) {
        com.mylog.selfcare.infrastructure.persistence.Habit row = new com.mylog.selfcare.infrastructure.persistence.Habit();
        row.id = habit.id(); row.goalId = habit.goalId(); row.userId = habit.userId();
        row.encryptedTitle = habit.title().ciphertext(); row.titleIv = habit.title().iv();
        row.titleWrappedKey = habit.title().wrappedKey(); row.titleKeyVersion = habit.title().keyVersion();
        row.targetValue = habit.targetValue(); row.unit = habit.unit(); row.frequencyType = habit.frequencyType();
        row.frequencyConfig = habit.frequencyConfig(); row.timezone = habit.timezone(); row.status = habit.status();
        row.createdAt = habit.createdAt(); row.updatedAt = habit.updatedAt(); row.rowVersion = habit.version();
        em.persist(row);
    }
    public Optional<Habit> habit(UUID userId, UUID id) {
        return em.createQuery("select h from Habit h where h.userId=:user and h.id=:id", com.mylog.selfcare.infrastructure.persistence.Habit.class)
                .setParameter("user", userId).setParameter("id", id).getResultStream().findFirst().map(this::habit);
    }
    public List<Habit> habits(UUID userId, UUID goalId) {
        return em.createQuery("select h from Habit h where h.userId=:user and h.goalId=:goal order by h.createdAt, h.id", com.mylog.selfcare.infrastructure.persistence.Habit.class)
                .setParameter("user", userId).setParameter("goal", goalId).getResultList().stream().map(this::habit).toList();
    }
    public List<Completion> completions(UUID userId, UUID habitId, LocalDate from, LocalDate to) {
        return em.createQuery("select c from HabitCompletion c where c.userId=:user and c.habitId=:habit "
                        + "and c.localDate between :from and :to order by c.localDate", HabitCompletion.class)
                .setParameter("user", userId).setParameter("habit", habitId).setParameter("from", from)
                .setParameter("to", to).getResultList().stream().map(this::completion).toList();
    }
    @Override public List<Completion> exportAllCompletions(UUID userId) {
        return em.createQuery("select c from HabitCompletion c where c.userId=:user order by c.localDate",HabitCompletion.class)
                .setParameter("user",userId).getResultList().stream().map(this::completion).toList();
    }
    public void putCompletion(Completion completion) {
        em.createNativeQuery("insert into habit_completions (id, habit_id, user_id, local_date, value, source, created_at, updated_at) "
                        + "values (:id, :habit, :user, :date, :value, :source, :now, :now) "
                        + "on conflict (habit_id, local_date) do update set value=excluded.value, "
                        + "updated_at=excluded.updated_at where habit_completions.user_id=excluded.user_id "
                        + "and habit_completions.value is distinct from excluded.value")
                .setParameter("id", completion.id()).setParameter("habit", completion.habitId())
                .setParameter("user", completion.userId()).setParameter("date", completion.localDate())
                .setParameter("value", completion.value()).setParameter("source", completion.source())
                .setParameter("now", completion.createdAt()).executeUpdate();
        em.clear();
    }
    public void deleteCompletion(UUID userId, UUID habitId, LocalDate date) {
        em.createQuery("delete from HabitCompletion c where c.userId=:user and c.habitId=:habit and c.localDate=:date")
                .setParameter("user", userId).setParameter("habit", habitId).setParameter("date", date).executeUpdate();
    }
    private Goal goal(SelfCareGoal g) {
        return new Goal(g.id, g.userId, g.category,
                new Encrypted(g.encryptedTitle, g.titleIv, g.titleWrappedKey, g.titleKeyVersion),
                g.encryptedDescription == null ? null : new Encrypted(g.encryptedDescription, g.descriptionIv,
                        g.descriptionWrappedKey, g.descriptionKeyVersion), g.status, g.startDate, g.targetDate,
                g.createdAt, g.updatedAt, g.completedAt, g.rowVersion);
    }
    private Habit habit(com.mylog.selfcare.infrastructure.persistence.Habit h) {
        return new Habit(h.id, h.goalId, h.userId,
                new Encrypted(h.encryptedTitle, h.titleIv, h.titleWrappedKey, h.titleKeyVersion), h.targetValue,
                h.unit, h.frequencyType, h.frequencyConfig, h.timezone, h.status, h.createdAt, h.updatedAt, h.rowVersion);
    }
    private Completion completion(HabitCompletion c) {
        return new Completion(c.id, c.habitId, c.userId, c.localDate, c.value, c.source, c.createdAt, c.updatedAt);
    }
}
