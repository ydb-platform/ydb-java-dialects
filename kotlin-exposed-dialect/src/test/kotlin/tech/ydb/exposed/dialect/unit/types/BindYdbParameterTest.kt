package tech.ydb.exposed.dialect.unit.types

import org.jetbrains.exposed.v1.core.ArrayColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.bindYdbParameter
import tech.ydb.exposed.dialect.code.YdbJdbcCode
import java.io.InputStream

class BindYdbParameterTest {

    @Test
    fun `fails fast when statement is not JdbcPreparedStatementImpl`() {
        val fakeStmt = object : PreparedStatementApi {
            override fun set(index: Int, value: Any, columnType: IColumnType<*>) {}
            override fun setNull(index: Int, columnType: IColumnType<*>) {}
            override fun setInputStream(index: Int, inputStream: InputStream, setAsBlobObject: Boolean) {}
            override fun setArray(index: Int, type: ArrayColumnType<*, *>, array: Array<*>) {}
        }

        assertThrows(IllegalStateException::class.java) {
            bindYdbParameter(fakeStmt, 1, 42L, YdbJdbcCode.UINT64)
        }
    }
}
