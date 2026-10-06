package tech.ydb.data.composite_key.repository;

import org.springframework.data.repository.CrudRepository;

import tech.ydb.data.composite_key.entity.CompositeKeyEntity;
import tech.ydb.data.composite_key.entity.CompositeKeyId;

public interface CompositeKeyRepository extends CrudRepository<CompositeKeyEntity, CompositeKeyId> {
}
