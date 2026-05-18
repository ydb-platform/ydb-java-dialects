package tech.ydb.hibernate.exception;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "exception_test_dup")
@NoArgsConstructor
@AllArgsConstructor
public class DupTestEntity {

    @Id
    private int id;

    private String name;
}
