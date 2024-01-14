package tech.ydb.hibernate.student.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class MarkId implements Serializable {

    private Long studentId;

    private Long courseId;
}

