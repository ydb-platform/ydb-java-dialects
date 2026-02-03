package tech.ydb.hibernate.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class MarkId implements Serializable {

    @Column(name = "StudentId")
    private int studentId;

    @Column(name = "CourseId")
    private int courseId;
}

