package tech.ydb.hibernate.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Kirill Kurdyukov
 */
@Getter
@Setter
@Entity
@Table(name = "Lecturers")
public class Lecturer {

    @Id
    @Column(name = "LecturerId")
    private int id;

    @Column(name = "LecturerName")
    private String name;
}
