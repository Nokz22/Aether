package com.aether.habits.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aether.shared.ApiException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 20);
    private static final UUID USER = UUID.randomUUID();

    @Mock
    private HabitRepository habits;

    @Mock
    private HabitCheckinRepository checkins;

    private HabitService service() {
        return new HabitService(habits, checkins);
    }

    private Habit habitOfUser() {
        return new Habit(USER, "Ler");
    }

    private static List<HabitCheckin> checkinsOn(Habit habit, LocalDate... dates) {
        return List.of(dates).stream()
                .map(date -> new HabitCheckin(habit.id(), date))
                .toList();
    }

    @Test
    void streakCountsConsecutiveDaysEndingToday() {
        Habit habit = habitOfUser();
        when(habits.findAllByUserIdOrderByCreatedAt(USER)).thenReturn(List.of(habit));
        when(checkins.findAllByIdHabitId(habit.id())).thenReturn(
                checkinsOn(habit, TODAY, TODAY.minusDays(1), TODAY.minusDays(2)));

        HabitResponse response = service().list(USER, TODAY).getFirst();

        assertThat(response.currentStreak()).isEqualTo(3);
        assertThat(response.doneToday()).isTrue();
    }

    @Test
    void openTodayDoesNotBreakTheStreak() {
        Habit habit = habitOfUser();
        when(habits.findAllByUserIdOrderByCreatedAt(USER)).thenReturn(List.of(habit));
        when(checkins.findAllByIdHabitId(habit.id())).thenReturn(
                checkinsOn(habit, TODAY.minusDays(1), TODAY.minusDays(2)));

        HabitResponse response = service().list(USER, TODAY).getFirst();

        assertThat(response.currentStreak()).isEqualTo(2);
        assertThat(response.doneToday()).isFalse();
    }

    @Test
    void gapBreaksTheStreak() {
        Habit habit = habitOfUser();
        when(habits.findAllByUserIdOrderByCreatedAt(USER)).thenReturn(List.of(habit));
        when(checkins.findAllByIdHabitId(habit.id())).thenReturn(
                checkinsOn(habit, TODAY, TODAY.minusDays(2), TODAY.minusDays(3)));

        HabitResponse response = service().list(USER, TODAY).getFirst();

        assertThat(response.currentStreak()).isEqualTo(1);
    }

    @Test
    void weekStripCoversTheLastSevenDaysInOrder() {
        Habit habit = habitOfUser();
        when(habits.findAllByUserIdOrderByCreatedAt(USER)).thenReturn(List.of(habit));
        when(checkins.findAllByIdHabitId(habit.id())).thenReturn(
                checkinsOn(habit, TODAY, TODAY.minusDays(6)));

        List<HabitDay> week = service().list(USER, TODAY).getFirst().week();

        assertThat(week).hasSize(7);
        assertThat(week.getFirst()).isEqualTo(new HabitDay(TODAY.minusDays(6), true));
        assertThat(week.getLast()).isEqualTo(new HabitDay(TODAY, true));
        assertThat(week.stream().filter(HabitDay::done)).hasSize(2);
    }

    @Test
    void futureCheckinIsRejected() {
        Habit habit = habitOfUser();
        when(habits.findByIdAndUserId(habit.id(), USER)).thenReturn(Optional.of(habit));

        assertThatThrownBy(() -> service().checkIn(USER, habit.id(), TODAY.plusDays(1), TODAY))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("future_checkin"));
        verify(checkins, never()).save(any());
    }

    @Test
    void checkinIsIdempotent() {
        Habit habit = habitOfUser();
        when(habits.findByIdAndUserId(habit.id(), USER)).thenReturn(Optional.of(habit));
        when(checkins.existsById(new HabitCheckinId(habit.id(), TODAY))).thenReturn(true);

        service().checkIn(USER, habit.id(), TODAY, TODAY);

        verify(checkins, never()).save(any());
    }

    @Test
    void someoneElsesHabitReadsAsNotFound() {
        UUID foreignHabit = UUID.randomUUID();
        when(habits.findByIdAndUserId(foreignHabit, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(USER, foreignHabit))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("habit_not_found"));
    }

    @Test
    void removingACheckinDeletesByCompositeId() {
        Habit habit = habitOfUser();
        when(habits.findByIdAndUserId(habit.id(), USER)).thenReturn(Optional.of(habit));

        service().removeCheckin(USER, habit.id(), TODAY);

        verify(checkins).deleteById(new HabitCheckinId(habit.id(), TODAY));
    }
}
