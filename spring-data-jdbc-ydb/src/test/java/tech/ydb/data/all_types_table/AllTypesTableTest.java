package tech.ydb.data.all_types_table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;

import tech.ydb.data.YdbBaseTest;
import tech.ydb.data.all_types_table.entity.AllTypesEntity;
import tech.ydb.data.all_types_table.repository.AllTypesEntityRepository;

/**
 * @author Madiyar Nurgazin
 */
public class AllTypesTableTest extends YdbBaseTest {

    @Autowired
    private AllTypesEntityRepository repository;

    @Autowired
    private JdbcAggregateOperations aggregateOperations;

    @Test
    public void allTypesTableCrudTest() {
        Assertions.assertEquals(3, repository.count());

        Optional<AllTypesEntity> entity1 = repository.findById(1);
        Assertions.assertTrue(entity1.isPresent());
        AllTypesEntity expected = new AllTypesEntity(
                1, "Madiyar Nurgazin", true, (byte) 1, (short) 2, 123123L, 1.123f, 1.123123d,
                new BigDecimal("1.123123000"), "binary".getBytes(), LocalDate.parse("2024-03-19"),
                LocalDateTime.parse("2024-03-20T10:30"), Instant.parse("2024-07-21T17:00:00Z"),
                "{\"a\" : \"b\"}", "{\"c\":\"d\"}", (byte) 3, (short) 4, 5, 12341234L
        );
        repository.save(expected);

        Assertions.assertEquals(expected.getDecimalColumn(), entity1.get().getDecimalColumn());
        Assertions.assertEquals(expected, entity1.get());
        Assertions.assertEquals(expected.getTextColumn(),
                repository.findAllByTextColumn("Madiyar Nurgazin").get(0).getTextColumn());

        AllTypesEntity entity2 = new AllTypesEntity(
                4, "text", false, (byte) 255, (short) 256, 1L, 1.0f, 123.123d,
                new BigDecimal("12.345678900"), "text".getBytes(), LocalDate.now().plusDays(2),
                LocalDateTime.now().minusDays(1), Instant.now().minus(10, ChronoUnit.HOURS),
                "{}", "{}", (byte) 3, (short) 4, 5, 12341234L
        );
        aggregateOperations.insert(entity2);
        Assertions.assertEquals(2, repository.countDistinctTextColumn());

        List<AllTypesEntity> entities = repository.findAll();
        Assertions.assertEquals(4, entities.size());

        repository.deleteById(1);
        Assertions.assertEquals(3, repository.count());
        Assertions.assertFalse(repository.existsById(1));

        AllTypesEntity entity3 = new AllTypesEntity(
                5, "text", true, (byte) 0, (short) 0, 0L, 0.0f, 0.0d,
                BigDecimal.ZERO, "".getBytes(), null, null, null, null, null, (byte) 0, (short) 0, 0, 0L
        );
        aggregateOperations.insert(entity3);

        entities = repository.findAllByDateColumnAfterNow();
        Assertions.assertEquals(1, entities.size());
        Assertions.assertEquals(Integer.valueOf(4), entities.get(0).getId());

        entity3.setJsonColumn("Not json");
        var ex = Assertions.assertThrows(DbActionExecutionException.class, () -> repository.save(entity3));
        Assertions.assertTrue(ex.getMessage().startsWith("Failed to execute DbAction.UpdateRoot"));

        entity3.setJsonColumn("{\"values\": [1, 2, 3]}");
        AllTypesEntity updated = repository.save(entity3);
        Assertions.assertEquals(entity3, updated);

        entity3.setJsonDocumentColumn("{\"value\" : 5}");
        AllTypesEntity saved = repository.save(entity3);

        entity3.setJsonDocumentColumn("{\"value\":5}");
        Assertions.assertEquals(entity3, saved);
    }
}
