package tech.ydb.slo.hibernate.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import tech.ydb.slo.hibernate.entity.SloEntity
import tech.ydb.slo.hibernate.repository.SloEntityRepository
import tech.ydb.slo.hibernate.retry.YdbRetryable
import java.time.Instant
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Kirill Kurdyukov
 */
@Service
class SloService {
    private val id = AtomicInteger(1)

    @Autowired
    private lateinit var sloEntityRepository: SloEntityRepository

    @YdbRetryable
    fun save() = sloEntityRepository.save(
        SloEntity().apply {
            id = this@SloService.id.getAndIncrement()
            payloadStr = UUID.randomUUID().toString()
            payloadDouble = ThreadLocalRandom.current().nextDouble()
            payloadTimestamp = Instant.now()
        }
    )

    @YdbRetryable
    fun find(): Optional<SloEntity> =
        sloEntityRepository.findById(ThreadLocalRandom.current().nextInt(id.get()))
}