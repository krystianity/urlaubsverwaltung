package org.synyx.urlaubsverwaltung.application.vacationtype;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory.HOLIDAY;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory.OVERTIME;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationCategory.SPECIALLEAVE;
import static org.synyx.urlaubsverwaltung.application.vacationtype.VacationTypeColor.YELLOW;

@ExtendWith(MockitoExtension.class)
class VacationTypeServiceImplTest {

    @Mock
    private VacationTypeRepository vacationTypeRepository;

    private VacationTypeServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new VacationTypeServiceImpl(vacationTypeRepository);
    }

    @Test
    void getActiveVacationTypesFilteredBy() {

        final VacationTypeEntity holiday = new VacationTypeEntity();
        holiday.setId(1L);
        holiday.setCategory(HOLIDAY);
        holiday.setActive(true);

        final VacationTypeEntity overtime = new VacationTypeEntity();
        overtime.setId(2L);
        overtime.setCategory(OVERTIME);
        overtime.setActive(true);

        final VacationTypeEntity overtimeActive = new VacationTypeEntity();
        overtimeActive.setId(3L);
        overtimeActive.setCategory(OVERTIME);
        overtimeActive.setActive(true);

        when(vacationTypeRepository.findByActiveIsTrueOrderById()).thenReturn(List.of(holiday, overtimeActive, overtime));

        final List<VacationType> typesWithoutCategory = sut.getActiveVacationTypesWithoutCategory(OVERTIME);
        assertThat(typesWithoutCategory).hasSize(1);
        assertThat(typesWithoutCategory.get(0).getId()).isEqualTo(1);
    }

    @Test
    void getActiveVacationTypes() {

        final VacationTypeEntity holiday = new VacationTypeEntity();
        holiday.setId(1L);
        holiday.setCategory(HOLIDAY);
        holiday.setActive(true);

        final VacationTypeEntity overtimeActive = new VacationTypeEntity();
        overtimeActive.setId(2L);
        overtimeActive.setCategory(OVERTIME);
        overtimeActive.setActive(true);

        when(vacationTypeRepository.findByActiveIsTrueOrderById()).thenReturn(List.of(holiday, overtimeActive));

        final List<VacationType> activeVacationTypes = sut.getActiveVacationTypes();
        assertThat(activeVacationTypes).hasSize(2);
        assertThat(activeVacationTypes.get(0).getId()).isEqualTo(1);
        assertThat(activeVacationTypes.get(1).getId()).isEqualTo(2);
    }

    @Test
    void getAllVacationTypes() {
        final VacationTypeEntity holiday = new VacationTypeEntity();
        holiday.setId(1L);
        holiday.setCategory(HOLIDAY);
        holiday.setActive(true);

        final VacationTypeEntity overtime = new VacationTypeEntity();
        overtime.setId(2L);
        overtime.setCategory(OVERTIME);
        overtime.setActive(false);

        when(vacationTypeRepository.findAll(Sort.by("id"))).thenReturn(List.of(holiday, overtime));

        final List<VacationType> allVacationTypes = sut.getAllVacationTypes();
        assertThat(allVacationTypes).hasSize(2);
        assertThat(allVacationTypes.get(0).getId()).isEqualTo(1);
        assertThat(allVacationTypes.get(1).getId()).isEqualTo(2);
    }

    @Test
    void ensureUpdateVacationTypesUpdatesTheGivenVacationTypes() {
        final VacationTypeEntity holidayEntity = new VacationTypeEntity();
        holidayEntity.setId(1L);
        holidayEntity.setCategory(HOLIDAY);
        holidayEntity.setMessageKey("holiday.message.key");
        holidayEntity.setActive(false);
        holidayEntity.setRequiresApprovalToApply(false);
        holidayEntity.setVisibleToEveryone(false);

        final VacationTypeEntity overtimeEntity = new VacationTypeEntity();
        overtimeEntity.setId(2L);
        overtimeEntity.setCategory(OVERTIME);
        overtimeEntity.setMessageKey("overtime.message.key");
        overtimeEntity.setActive(false);
        overtimeEntity.setRequiresApprovalToApply(false);
        overtimeEntity.setVisibleToEveryone(false);

        final VacationTypeEntity specialLeaveEntity = new VacationTypeEntity();
        specialLeaveEntity.setId(3L);
        specialLeaveEntity.setCategory(SPECIALLEAVE);
        specialLeaveEntity.setMessageKey("specialleave.message.key");
        specialLeaveEntity.setActive(true);
        specialLeaveEntity.setRequiresApprovalToApply(false);
        specialLeaveEntity.setVisibleToEveryone(true);

        when(vacationTypeRepository.findAllById(Set.of(1L, 2L, 3L))).thenReturn(List.of(holidayEntity, overtimeEntity, specialLeaveEntity));

        sut.updateVacationTypes(List.of(
            new VacationTypeUpdate(1L, true, false, false, YELLOW, false),
            new VacationTypeUpdate(2L, false, true, true, YELLOW, false),
            new VacationTypeUpdate(3L, true, true, true, YELLOW, true)
        ));

        @SuppressWarnings("unchecked") final ArgumentCaptor<List<VacationTypeEntity>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(vacationTypeRepository).saveAll(argumentCaptor.capture());

        final List<VacationTypeEntity> persistedList = argumentCaptor.getValue();
        assertThat(persistedList.get(0).getId()).isEqualTo(1);
        assertThat(persistedList.get(0).getCategory()).isEqualTo(HOLIDAY);
        assertThat(persistedList.get(0).getMessageKey()).isEqualTo("holiday.message.key");
        assertThat(persistedList.get(0).isActive()).isTrue();
        assertThat(persistedList.get(0).isRequiresApprovalToApply()).isFalse();
        assertThat(persistedList.get(0).isVisibleToEveryone()).isFalse();
        assertThat(persistedList.get(1).getId()).isEqualTo(2);
        assertThat(persistedList.get(1).getCategory()).isEqualTo(OVERTIME);
        assertThat(persistedList.get(1).getMessageKey()).isEqualTo("overtime.message.key");
        assertThat(persistedList.get(1).isActive()).isFalse();
        assertThat(persistedList.get(1).isRequiresApprovalToApply()).isTrue();
        assertThat(persistedList.get(1).isVisibleToEveryone()).isFalse();
        assertThat(persistedList.get(2).getId()).isEqualTo(3);
        assertThat(persistedList.get(2).getCategory()).isEqualTo(SPECIALLEAVE);
        assertThat(persistedList.get(2).getMessageKey()).isEqualTo("specialleave.message.key");
        assertThat(persistedList.get(2).isActive()).isTrue();
        assertThat(persistedList.get(2).isRequiresApprovalToApply()).isTrue();
        assertThat(persistedList.get(2).isVisibleToEveryone()).isTrue();

    }

    @Test
    void ensureUpdateVacationTypesForEmptyList() {
        when(vacationTypeRepository.findAllById(emptySet())).thenReturn(emptyList());

        sut.updateVacationTypes(List.of());

        verify(vacationTypeRepository).saveAll(emptyList());
    }
}
