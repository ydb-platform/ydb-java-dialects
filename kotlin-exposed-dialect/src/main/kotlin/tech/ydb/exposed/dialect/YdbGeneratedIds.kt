package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import tech.ydb.exposed.dialect.types.ydbUuid
import java.security.SecureRandom
import java.util.UUID

private const val ULID_LENGTH = 26
private const val ULID_RANDOM_BYTES = 10
private val ULID_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray()
private val ULID_RANDOM = SecureRandom()

/**
 * Generates a [ULID](https://github.com/ulid/spec) encoded as a 26-character Crockford-base32 string.
 * Time component is taken from [nowMillis] (defaults to current wall clock), random component is
 * cryptographically random. Lexicographic ordering of ULIDs matches their time component.
 */
fun ydbUlid(nowMillis: Long = System.currentTimeMillis()): String {
    require(nowMillis >= 0) { "ULID timestamp must be non-negative" }

    val chars = CharArray(ULID_LENGTH)
    var timestamp = nowMillis

    for (i in 9 downTo 0) {
        chars[i] = ULID_ALPHABET[(timestamp and 31L).toInt()]
        timestamp = timestamp ushr 5
    }

    val bytes = ByteArray(ULID_RANDOM_BYTES)
    ULID_RANDOM.nextBytes(bytes)

    var bitBuffer = 0
    var bitCount = 0
    var charIndex = 10

    for (byte in bytes) {
        bitBuffer = (bitBuffer shl 8) or (byte.toInt() and 0xff)
        bitCount += 8

        while (bitCount >= 5 && charIndex < ULID_LENGTH) {
            bitCount -= 5
            chars[charIndex++] = ULID_ALPHABET[(bitBuffer ushr bitCount) and 31]
        }
    }

    if (charIndex < ULID_LENGTH) {
        chars[charIndex] = ULID_ALPHABET[(bitBuffer shl (5 - bitCount)) and 31]
    }

    return String(chars)
}

/**
 * IdTable with a native YDB `Uuid` primary key, auto-generated client-side via
 * [UUID.randomUUID].
 */
open class YdbUuidIdTable(name: String = "") : YdbIdTable<UUID>(name) {
    final override val id: Column<EntityID<UUID>> = ydbUuid("id")
        .clientDefault { UUID.randomUUID() }
        .entityId()

    final override val primaryKey = PrimaryKey(id)
}

/**
 * IdTable with a [ULID][ydbUlid] string primary key, auto-generated client-side.
 *
 * Pick this over [YdbUuidIdTable] when you want lexicographically-sortable identifiers
 * (e.g. range scans by id approximate time-of-creation order).
 */
open class YdbUlidTable(
    name: String = "",
    idLength: Int = ULID_LENGTH
) : YdbIdTable<String>(name) {
    final override val id: Column<EntityID<String>> = varchar("id", idLength)
        .clientDefault { ydbUlid() }
        .entityId()

    final override val primaryKey = PrimaryKey(id)
}
