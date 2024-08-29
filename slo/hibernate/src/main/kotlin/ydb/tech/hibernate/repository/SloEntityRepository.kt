package tech.ydb.slo.hibernate.repository

import org.springframework.data.repository.CrudRepository
import tech.ydb.slo.hibernate.entity.SloEntity


/**
 * @author Kirill Kurdyukov
 */
interface SloEntityRepository : CrudRepository<SloEntity, Int>