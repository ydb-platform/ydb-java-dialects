package tech.ydb.hibernate.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Kirill Kurdyukov
 */
@Data
@Entity
@Table(name = "backup/Users")
public class User {

    @Id
    @GeneratedValue(generator = "random-int-id")
    @GenericGenerator(name = "random-int-id", type = RandomIntGenerator.class)
    private int id;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "name")
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json")
    private Json json;

    @Data
    // @Embeddable TODO  Dialect does not support aggregateComponentAssignmentExpression: org.hibernate.dialect.aggregate.AggregateSupportImpl
    public static class Json {
        private int a;

        private String b;
    }

    public static class RandomIntGenerator implements IdentifierGenerator {

        @Override
        public Object generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o) {
            return ThreadLocalRandom.current().nextInt();
        }
    }
}
