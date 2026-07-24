package tech.ydb.hibernate.index;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Account table that is joined several times under different aliases and different columns,
 * reproducing the real "one table, many joins" scenario the index hints are built for.
 * <p>
 * Three secondary indexes let a test pin a different {@code VIEW} per join.
 */
@Entity
@Table(
        name = "bank_account",
        indexes = {
                @Index(name = "bank_account_code_idx", columnList = "code"),
                @Index(name = "bank_account_parent_idx", columnList = "parent"),
                @Index(name = "bank_account_combo_idx", columnList = "code, parent")
        }
)
public class BankAccount {

    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "code")
    private String code;

    @Column(name = "parent")
    private String parent;

    @Column(name = "name")
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
