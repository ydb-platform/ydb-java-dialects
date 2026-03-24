package tech.ydb.exposed.dialect.types

import org.jetbrains.exposed.v1.core.vendors.DataTypeProvider

class YdbDataTypeProvider : DataTypeProvider() {
    override fun binaryType(): String = "String"

    override fun hexToDb(hexString: String): String = "'$hexString'"

    override fun integerType(): String = "Int32"

    override fun longType(): String = "Int64"

    override fun booleanType(): String = "Bool"

    override fun uuidType(): String = "Uuid"

    override fun textType(): String = "String"

    override fun varcharType(colLength: Int): String = "Utf8"

    override fun binaryType(length: Int): String = "String"

    override fun dateType(): String = "Date"

    override fun floatType(): String = "Float"

    override fun doubleType(): String = "Double"

    override fun dateTimeType(): String = "Datetime"

    override fun shortType(): String = "Int16"

    override fun jsonType(): String = "Json"
}