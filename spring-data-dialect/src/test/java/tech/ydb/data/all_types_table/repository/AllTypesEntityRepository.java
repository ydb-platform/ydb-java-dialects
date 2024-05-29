package tech.ydb.data.all_types_table.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import tech.ydb.data.all_types_table.entity.AllTypesEntity;
import tech.ydb.data.repository.YdbListCrudRepository;

/**
 * @author Madiyar Nurgazin
 */
public interface AllTypesEntityRepository extends YdbListCrudRepository<AllTypesEntity, Integer> {
    @Query("select count(distinct text_column) from all_types_table")
    long countDistinctTextColumn();

    @Query("select * from all_types_table where date_column > CurrentUtcDate()")
    List<AllTypesEntity> findAllByDateColumnAfterNow();
}
