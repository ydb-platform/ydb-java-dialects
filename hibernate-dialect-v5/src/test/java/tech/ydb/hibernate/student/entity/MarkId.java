package tech.ydb.hibernate.student.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@Embeddable
public class MarkId implements Serializable {

    @Column(name = "StudentId")
    private int studentId;

    @Column(name = "CourseId")
    private int courseId;
}

