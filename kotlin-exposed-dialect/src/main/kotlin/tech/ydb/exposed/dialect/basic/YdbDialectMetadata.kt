package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.jdbc.vendors.DatabaseDialectMetadata

class YdbDialectMetadata : DatabaseDialectMetadata()

//package tech.ydb.exposed.dialect.basic
//
//import org.jetbrains.exposed.v1.core.Index
//import org.jetbrains.exposed.v1.core.Table
//import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
//import org.jetbrains.exposed.v1.jdbc.vendors.DatabaseDialectMetadata
//import java.sql.DatabaseMetaData
//
//class YdbDialectMetadata : DatabaseDialectMetadata() {
//
//    override fun existingIndices(vararg tables: Table): Map<Table, List<Index>> {
//        return jdbcMeta { meta ->
//            tables.associateWith { table ->
//                readIndices(meta, table)
//            }
//        }
//    }
//
//    private fun <T> jdbcMeta(block: (DatabaseMetaData) -> T): T =
//        TransactionManager.current().connection.metadata { meta ->
//            block(meta)
//        }
//
//    private fun readIndices(meta: DatabaseMetaData, table: Table): List<Index> {
//        return emptyList()
//    }
//}