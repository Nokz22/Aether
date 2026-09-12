package com.aether.habits.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aether.habits.HabitView;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HabitsApiAdapterTest {

    private static final UUID USER = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 12);

    @Mock
    private HabitService habits;

    @Test
    void todayHabitsMapsStateAndStreak() {
        UUID id = UUID.randomUUID();
        when(habits.list(USER, TODAY)).thenReturn(List.of(
                new HabitResponse(id, "Ler", true, 4, List.of())));

        HabitView view = new HabitsApiAdapter(habits).todayHabits(USER, TODAY).getFirst();

        assertThat(view).isEqualTo(new HabitView(id, "Ler", true, 4));
    }

    @Test
    void createdHabitStartsUndoneWithNoStreak() {
        UUID id = UUID.randomUUID();
        when(habits.create(USER, "Correr")).thenReturn(new HabitSummary(id, "Correr"));

        assertThat(new HabitsApiAdapter(habits).createHabit(USER, "Correr"))
                .isEqualTo(new HabitView(id, "Correr", false, 0));
    }

    @Test
    void checkInDelegatesToTheService() {
        UUID id = UUID.randomUUID();

        new HabitsApiAdapter(habits).checkIn(USER, id, TODAY, TODAY);

        verify(habits).checkIn(USER, id, TODAY, TODAY);
    }
}
