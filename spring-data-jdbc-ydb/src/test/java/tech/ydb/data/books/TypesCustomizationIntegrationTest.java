package tech.ydb.data.books;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.UncategorizedSQLException;

import tech.ydb.data.YdbBaseTest;
import tech.ydb.data.core.convert.YdbType;

public class TypesCustomizationIntegrationTest extends YdbBaseTest {

    @Autowired
    private SimpleEntityRepository simpleEntityRepository;

    @Test
    void testCustomTypes() {
        Assertions
          .assertThatThrownBy(() -> simpleEntityRepository.findByWrongType(12))
          .isInstanceOf(UncategorizedSQLException.class)
          .hasMessageContaining("Cannot cast [class java.lang.Integer: 12] to [Uuid]");

        Assertions
          .assertThatCode(() -> simpleEntityRepository.findBySpecificType(12))
          .doesNotThrowAnyException();
    }

    interface SimpleEntityRepository extends CrudRepository<SimpleEntity, Long> {

        //language=sql
        @Query("SELECT * FROM simple_entity WHERE with_specific_type = :withSpecificType")
        Optional<SimpleEntity> findBySpecificType(@Param("withSpecificType") @YdbType("Uint32") int withSpecificType);

        //language=sql
        @Query("SELECT * FROM simple_entity WHERE with_specific_type = :withSpecificType")
        Optional<SimpleEntity> findByWrongType(@Param("withSpecificType") @YdbType("Uuid") int withSpecificType);
    }

    @Table
    static class SimpleEntity {

        @Id
        private Long id;

        private String name;

        private Integer withSpecificType;
    }
}
