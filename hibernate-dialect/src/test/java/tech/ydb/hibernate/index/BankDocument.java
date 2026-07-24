package tech.ydb.hibernate.index;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A document that references {@link BankAccount} three times, each association joining the
 * same table on different column(s). Hibernate generates joins such as:
 * <pre>
 *   join bank_account a1_0 on a1_0.code=d1_0.acc_dt_code                              (accDt)
 *   join bank_account a2_0 on a2_0.parent=d1_0.acc_kt_parent                          (accKt)
 *   join bank_account a3_0 on a3_0.code=d1_0.acc_combo_code and a3_0.parent=...        (accCombo)
 * </pre>
 * which is exactly what the column-aware {@code use_index} hint needs to distinguish.
 */
@Entity
@Table(name = "bank_document")
public class BankDocument {

    @Id
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acc_dt_code", referencedColumnName = "code")
    private BankAccount accDt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acc_kt_parent", referencedColumnName = "parent")
    private BankAccount accKt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "acc_combo_code", referencedColumnName = "code"),
            @JoinColumn(name = "acc_combo_parent", referencedColumnName = "parent")
    })
    private BankAccount accCombo;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public BankAccount getAccDt() {
        return accDt;
    }

    public void setAccDt(BankAccount accDt) {
        this.accDt = accDt;
    }

    public BankAccount getAccKt() {
        return accKt;
    }

    public void setAccKt(BankAccount accKt) {
        this.accKt = accKt;
    }

    public BankAccount getAccCombo() {
        return accCombo;
    }

    public void setAccCombo(BankAccount accCombo) {
        this.accCombo = accCombo;
    }
}
