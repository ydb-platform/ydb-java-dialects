package tech.ydb.slo;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tech.ydb.retry.YdbTransactional;

@Service
public class SloService {

    private static final Logger log = LoggerFactory.getLogger(SloService.class);
    private static final String TABLE_NAME = "slo_test_table";
    private static final String SELECT_MAX_ID_SQL = "SELECT MAX(id) FROM " + TABLE_NAME;
    private static final int SECOND_UPSERT_ID_OFFSET = 1;

    private final JdbcTemplate jdbcTemplate;

    public SloService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @YdbTransactional(idempotent = true)
    public void upsert(
            String guid,
            int id,
            String payloadStr,
            double payloadDouble,
            LocalDateTime payloadTimestamp) {
        jdbcTemplate.update(
                "UPSERT INTO "
                        + TABLE_NAME
                        + " (guid, id, payload_str, payload_double, payload_timestamp) VALUES (?, ?, ?, ?, ?)",
                guid,
                id,
                payloadStr,
                payloadDouble,
                Timestamp.valueOf(payloadTimestamp));
    }

    @YdbTransactional(idempotent = true)
    public void upsert2(
            String guid,
            int id,
            String payloadStr,
            double payloadDouble,
            LocalDateTime payloadTimestamp) {
        jdbcTemplate.update(
                "UPSERT INTO "
                        + TABLE_NAME
                        + " (guid, id, payload_str, payload_double, payload_timestamp) VALUES (?, ?, ?, ?, ?)",
                guid,
                id,
                payloadStr,
                payloadDouble,
                Timestamp.valueOf(payloadTimestamp));

        jdbcTemplate.update(
                "UPSERT INTO "
                        + TABLE_NAME
                        + " (guid, id, payload_str, payload_double, payload_timestamp) VALUES (?, ?, ?, ?, ?)",
                guid,
                id + SECOND_UPSERT_ID_OFFSET,
                payloadStr,
                payloadDouble,
                Timestamp.valueOf(payloadTimestamp));
    }

    @YdbTransactional(idempotent = true, readOnly = true)
    public String select(String guid, int id) {
        return jdbcTemplate.queryForObject(
                "SELECT payload_str FROM " + TABLE_NAME + " WHERE guid = ? AND id = ?",
                String.class,
                guid,
                id);
    }

    @YdbTransactional(idempotent = true, readOnly = true)
    public int selectMaxId() {
        Integer result = jdbcTemplate.queryForObject(SELECT_MAX_ID_SQL, Integer.class);
        return result != null ? result : 0;
    }
}
