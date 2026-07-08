package com.gongsoop.studyspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class StudySpaceEntityMappingTest {

    @Test
    void mapsEntitiesToOracleTables() {
        assertTable(StudyRoom.class, "STUDY_ROOM");
        assertTable(StudyChannel.class, "STUDY_CHANNEL");
        assertTable(Seat.class, "SEAT");
        assertTable(StudySession.class, "STUDY_SESSION");
        assertTable(SeatOccupancy.class, "SEAT_OCCUPANCY");
    }

    @Test
    void usesExistingOracleSequences() {
        assertSequence(StudyRoom.class, "SEQ_STUDY_ROOM");
        assertSequence(StudyChannel.class, "SEQ_STUDY_CHANNEL");
        assertSequence(Seat.class, "SEQ_SEAT");
        assertSequence(StudySession.class, "SEQ_STUDY_SESSION");
        assertSequence(SeatOccupancy.class, "SEQ_SEAT_OCCUPANCY");
    }

    @Test
    void mapsSessionAndOccupancyForeignKeys() throws NoSuchFieldException {
        assertJoinColumn(StudySession.class, "member", "MEMBER_ID");
        assertJoinColumn(StudySession.class, "studyChannel", "STUDY_CHANNEL_ID");
        assertJoinColumn(StudySession.class, "seat", "SEAT_ID");
        assertJoinColumn(SeatOccupancy.class, "member", "MEMBER_ID");
        assertJoinColumn(SeatOccupancy.class, "studyChannel", "STUDY_CHANNEL_ID");
        assertJoinColumn(SeatOccupancy.class, "seat", "SEAT_ID");
        assertJoinColumn(SeatOccupancy.class, "studySession", "STUDY_SESSION_ID");
    }

    @Test
    void mapsExpectedPrimaryKeyColumns() throws NoSuchFieldException {
        assertColumn(StudyRoom.class, "id", "STUDY_ROOM_ID");
        assertColumn(StudyChannel.class, "id", "STUDY_CHANNEL_ID");
        assertColumn(Seat.class, "id", "SEAT_ID");
        assertColumn(StudySession.class, "id", "STUDY_SESSION_ID");
        assertColumn(SeatOccupancy.class, "id", "SEAT_OCCUPANCY_ID");
    }

    private void assertTable(Class<?> entityType, String expectedName) {
        Table table = entityType.getAnnotation(Table.class);
        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo(expectedName);
    }

    private void assertSequence(Class<?> entityType, String expectedName) {
        Field idField;
        try {
            idField = entityType.getDeclaredField("id");
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
        SequenceGenerator sequence = idField.getAnnotation(SequenceGenerator.class);
        assertThat(sequence).isNotNull();
        assertThat(sequence.sequenceName()).isEqualTo(expectedName);
        assertThat(sequence.allocationSize()).isEqualTo(1);
    }

    private void assertJoinColumn(Class<?> entityType, String fieldName, String expectedName)
            throws NoSuchFieldException {
        JoinColumn joinColumn = entityType.getDeclaredField(fieldName).getAnnotation(JoinColumn.class);
        assertThat(joinColumn).isNotNull();
        assertThat(joinColumn.name()).isEqualTo(expectedName);
    }

    private void assertColumn(Class<?> entityType, String fieldName, String expectedName)
            throws NoSuchFieldException {
        Column column = entityType.getDeclaredField(fieldName).getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo(expectedName);
    }
}
