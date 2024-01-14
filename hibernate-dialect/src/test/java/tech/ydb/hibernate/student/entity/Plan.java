package tech.ydb.hibernate.student.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * @author Kirill Kurdyukov
 */
@Data
@Entity
@Table(name = "Plan")
@IdClass(PlanId.class)
public class Plan {

    @Id
    private Long GroupId;

    @Id
    private Long CourseId;

    @Id
    private Long LecturerId;
}
