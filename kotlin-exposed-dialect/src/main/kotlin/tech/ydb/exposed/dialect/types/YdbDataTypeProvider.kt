package tech.ydb.exposed.dialect.types

import org.jetbrains.exposed.v1.core.vendors.DataTypeProvider

class YdbDataTypeProvider : DataTypeProvider() {
    override fun byteType(): String = "Int8"
    override fun ubyteType(): String = "Uint8"

    override fun binaryType(): String = "String"
    override fun binaryType(length: Int): String = "String"

    override fun blobType(): String = binaryType()

    override fun hexToDb(hexString: String): String = "'$hexString'"

    override fun shortType(): String = "Int16"
    override fun ushortType(): String = "Uint16"

    override fun integerType(): String = "Int32"
    override fun uintegerType(): String = "Uint32"

    override fun integerAutoincType(): String =
        throw UnsupportedOperationException(
            "YDB does not support AUTO_INCREMENT. Use YdbUuidIdTable, YdbUuidStringIdTable, or YdbUlidTable instead."
        )

    override fun longType(): String = "Int64"
    override fun booleanType(): String = "Bool"

    override fun floatType(): String = "Float"
    override fun doubleType(): String = "Double"

    override fun varcharType(colLength: Int): String = "Utf8"

    override fun textType(): String = "Utf8" //"String"
    override fun mediumTextType(): String = textType()
    override fun largeTextType(): String = textType()

    override fun uuidType(): String = "Uuid"

    override fun dateType(): String = "Date"
    override fun dateTimeType(): String = "Datetime"
    override fun timestampType(): String = "Timestamp"

    override fun jsonType(): String = "Json"
}
