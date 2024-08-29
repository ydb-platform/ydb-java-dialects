package tech.ydb.slo.hibernate.entity

import jakarta.annotation.Nonnull
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * @author Kirill Kurdyukov
 */
@Entity
@Table(name = "slo-hibernate")
class SloEntity {

    @Id
    var id: Int = 0

    @Nonnull
    @Column(name = "payload_str")
    lateinit var payloadStr: String

    @Nonnull
    @Column(name = "payload_double")
    var payloadDouble: Double = 0.0

    @Nonnull
    @Column(name = "payload_timestamp")
    lateinit var payloadTimestamp: Instant
}