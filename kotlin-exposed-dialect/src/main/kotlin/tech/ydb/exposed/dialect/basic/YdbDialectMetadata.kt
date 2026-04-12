package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.jdbc.vendors.DatabaseDialectMetadata

class YdbDialectMetadata : DatabaseDialectMetadata() {
    // Минимальная рабочая реализация:
    // - existingIndices(...)
    // - existingPrimaryKeys(...)
    // - maybe tableExists / columns metadata, если это требуется вашим тестам и SchemaUtils
}