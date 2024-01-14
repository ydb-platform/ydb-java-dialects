package tech.ydb.hibernate.student.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * @author Kirill Kurdyukov
 */
@Data
@Entity
@Table(name = "Lecturers")
public class Lecturer {

    @Id
    private Long LecturerId;

    private String LecturerName;
}
