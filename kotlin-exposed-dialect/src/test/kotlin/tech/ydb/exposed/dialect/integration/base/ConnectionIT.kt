package tech.ydb.exposed.dialect.integration.base

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ConnectionIT : BaseYdbTest() {

    @Test
    fun `should connect to ydb with explicit dialect`() = tx {
        assertNotNull(connection)
    }
}