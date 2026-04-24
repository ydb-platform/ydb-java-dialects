package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import tech.ydb.exposed.dialect.types.ydbUuid
import java.security.SecureRandom
import java.util.UUID

object YdbGeneratedIds {
    private const val ULID_LENGTH = 26
    private const val RANDOM_BYTES = 10
    private val alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray()
    private val random = SecureRandom()

    fun uuid(): UUID = UUID.randomUUID()

    fun uuidString(): String = uuid().toString()

    fun ulid(nowMillis: Long = System.currentTimeMillis()): String {
        require(nowMillis >= 0) { "ULID timestamp must be non-negative" }

        val chars = CharArray(ULID_LENGTH)
        var timestamp = nowMillis

        for (i in 9 downTo 0) {
            chars[i] = alphabet[(timestamp and 31L).toInt()]
            timestamp = timestamp ushr 5
        }

        val bytes = ByteArray(RANDOM_BYTES)
        random.nextBytes(bytes)

        var bitBuffer = 0
        var bitCount = 0
        var charIndex = 10

        for (byte in bytes) {
            bitBuffer = (bitBuffer shl 8) or (byte.toInt() and 0xff)
            bitCount += 8

            while (bitCount >= 5 && charIndex < ULID_LENGTH) {
                bitCount -= 5
                chars[charIndex++] = alphabet[(bitBuffer ushr bitCount) and 31]
            }
        }

        if (charIndex < ULID_LENGTH) {
            chars[charIndex] = alphabet[(bitBuffer shl (5 - bitCount)) and 31]
        }

        return String(chars)
    }
}

open class YdbUuidIdTable(name: String = "") : YdbIdTable<UUID>(name) {
    final override val id: Column<EntityID<UUID>> = ydbUuid("id")
        .clientDefault { YdbGeneratedIds.uuid() }
        .entityId()

    final override val primaryKey = PrimaryKey(id)
}

open class YdbUuidStringIdTable(
    name: String = "",
    idLength: Int = 36
) : YdbIdTable<String>(name) {
    final override val id: Column<EntityID<String>> = varchar("id", idLength)
        .clientDefault { YdbGeneratedIds.uuidString() }
        .entityId()

    final override val primaryKey = PrimaryKey(id)
}

open class YdbUlidTable(
    name: String = "",
    idLength: Int = 26
) : YdbIdTable<String>(name) {
    final override val id: Column<EntityID<String>> = varchar("id", idLength)
        .clientDefault { YdbGeneratedIds.ulid() }
        .entityId()

    final override val primaryKey = PrimaryKey(id)
}