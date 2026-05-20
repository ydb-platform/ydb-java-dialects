package tech.ydb.hibernate.string;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "string_test")
public class StringEntity {
    @Id
    @Column(name = "id")
    public int id;

    @Column(name = "name")
    public String name;
}
