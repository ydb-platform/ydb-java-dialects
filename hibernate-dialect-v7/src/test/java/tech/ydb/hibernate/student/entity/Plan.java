package tech.ydb.hibernate.student.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Ainur Mukhtarov
 */
@Getter
@Setter
@Entity
@Table(name = "Plan")
public class Plan {

    @EmbeddedId
    private PlanId planId;
}
