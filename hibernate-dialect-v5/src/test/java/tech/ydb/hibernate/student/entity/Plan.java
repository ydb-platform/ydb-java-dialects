package tech.ydb.hibernate.student.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Kirill Kurdyukov
 */
@Getter
@Setter
@Entity
@Table(name = "Plan")
public class Plan {

    @EmbeddedId
    private PlanId planId;
}
