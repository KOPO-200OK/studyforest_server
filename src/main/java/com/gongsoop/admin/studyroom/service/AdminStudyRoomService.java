package com.gongsoop.admin.studyroom.service;

import com.gongsoop.admin.studyroom.dto.request.UpdateAdminSeatActiveRequest;
import com.gongsoop.admin.studyroom.dto.response.AdminSeatResponse;
import com.gongsoop.global.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminStudyRoomService {

    private final JdbcTemplate jdbcTemplate;

    public AdminStudyRoomService(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 관리자 화면에 표시할 전체 좌석 목록을 조회합니다.
     *
     * 프론트 화면의 좌석 번호는 DB의 SEAT_NO와 연결합니다.
     */
    public List<AdminSeatResponse> getSeats() {
        return jdbcTemplate.query(
                """
                SELECT
                    seat.SEAT_ID,
                    room.STUDY_ROOM_ID,
                    room.ROOM_NAME,
                    room.MAP_NO,
                    seat.SEAT_NO,
                    seat.IS_ACTIVE,
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM SEAT_OCCUPANCY occupancy
                            WHERE occupancy.SEAT_ID = seat.SEAT_ID
                        ) THEN 1
                        ELSE 0
                    END AS IS_OCCUPIED
                FROM SEAT seat
                JOIN STUDY_ROOM room
                  ON room.STUDY_ROOM_ID = seat.STUDY_ROOM_ID
                ORDER BY room.MAP_NO ASC,
                         seat.SEAT_NO ASC
                """,
                (resultSet, rowNumber) ->
                        mapSeat(resultSet)
        );
    }

    /**
     * 좌석의 활성화 상태를 변경합니다.
     *
     * 현재 사용자가 착석 중인 좌석은 비활성화하지 않습니다.
     */
    @Transactional
    public AdminSeatResponse updateSeatActive(
            Long seatId,
            UpdateAdminSeatActiveRequest request
    ) {
        AdminSeatResponse currentSeat =
                getSeat(seatId);

        if (
                !Boolean.TRUE.equals(request.active())
                        && Boolean.TRUE.equals(currentSeat.occupied())
        ) {
            throw new BusinessException(
                    "SEAT_OCCUPIED",
                    "현재 사용 중인 좌석은 비활성화할 수 없습니다",
                    HttpStatus.CONFLICT
            );
        }

        int updatedCount = jdbcTemplate.update(
                """
                UPDATE SEAT
                SET IS_ACTIVE = ?
                WHERE SEAT_ID = ?
                """,
                Boolean.TRUE.equals(request.active())
                        ? "Y"
                        : "N",
                seatId
        );

        if (updatedCount == 0) {
            throw new BusinessException(
                    "SEAT_NOT_FOUND",
                    "좌석을 찾을 수 없습니다",
                    HttpStatus.NOT_FOUND
            );
        }

        return getSeat(seatId);
    }

    /**
     * 좌석 한 건을 조회합니다.
     */
    private AdminSeatResponse getSeat(
            Long seatId
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    seat.SEAT_ID,
                    room.STUDY_ROOM_ID,
                    room.ROOM_NAME,
                    room.MAP_NO,
                    seat.SEAT_NO,
                    seat.IS_ACTIVE,
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM SEAT_OCCUPANCY occupancy
                            WHERE occupancy.SEAT_ID = seat.SEAT_ID
                        ) THEN 1
                        ELSE 0
                    END AS IS_OCCUPIED
                FROM SEAT seat
                JOIN STUDY_ROOM room
                  ON room.STUDY_ROOM_ID = seat.STUDY_ROOM_ID
                WHERE seat.SEAT_ID = ?
                """,
                resultSet -> {
                    if (!resultSet.next()) {
                        throw new BusinessException(
                                "SEAT_NOT_FOUND",
                                "좌석을 찾을 수 없습니다",
                                HttpStatus.NOT_FOUND
                        );
                    }

                    return mapSeat(resultSet);
                },
                seatId
        );
    }

    /**
     * JDBC 조회 결과를 DTO로 변환합니다.
     */
    private AdminSeatResponse mapSeat(
            ResultSet resultSet
    ) throws SQLException {
        return new AdminSeatResponse(
                resultSet.getLong("SEAT_ID"),
                resultSet.getLong("STUDY_ROOM_ID"),
                resultSet.getString("ROOM_NAME"),
                resultSet.getInt("MAP_NO"),
                resultSet.getInt("SEAT_NO"),
                "Y".equalsIgnoreCase(
                        resultSet.getString("IS_ACTIVE")
                ),
                resultSet.getInt("IS_OCCUPIED") == 1
        );
    }
}