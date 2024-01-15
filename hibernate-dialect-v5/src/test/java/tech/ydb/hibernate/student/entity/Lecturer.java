package tech.ydb.hibernate.student.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
