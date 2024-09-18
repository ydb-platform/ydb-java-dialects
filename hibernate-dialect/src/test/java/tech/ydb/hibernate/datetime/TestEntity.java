package tech.ydb.hibernate.datetime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @author Kirill Kurdyukov
 */
@Entity
@Data
@Table(name = "hibernate_test")
public class TestEntity {

    @Id
    private Integer id;

    @Column(name = "c_Date", nullable = false)
    private LocalDate date;

    @Column(name = "c_Datetime", nullable = false)
    private LocalDateTime datetime;

    @Column(name = "c_Timestamp", nullable = false)
    private Instant timestamp;
}
