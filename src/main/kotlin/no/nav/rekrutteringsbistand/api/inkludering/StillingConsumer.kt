package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.rekrutteringsbistand.api.support.LOG
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.errors.WakeupException
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import rekrutteringsbistand.stilling.indekser.utils.Liveness
import java.io.Closeable
import java.time.Duration
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.concurrent.thread

@Component
class StillingConsumer(
        private val consumer: Consumer<String, Ad>,
        private val inkluderingService: InkluderingService
) : Closeable {

    @Scheduled(fixedRate = Long.MAX_VALUE)
    fun start() {

            try {
                consumer.subscribe(listOf(stillingstopic))
                LOG.info(
                        "Starter å konsumere topic $stillingstopic med groupId ${consumer.groupMetadata().groupId()} "
                )

                while (true) {
                    val records: ConsumerRecords<String, Ad> = consumer.poll(Duration.ofSeconds(5))
                    if (records.count() == 0) continue

                    val stillinger = records.map { it.value() }
                    LOG.info("Stillinger mottatt: " + stillinger.size.toString())
                    inkluderingService.lagreInkludering("testLagreIDb")
                    consumer.commitSync()

                    LOG.info("Committet offset ${records.last().offset()} til Kafka")
                }
            } catch (exception: WakeupException) {
                LOG.info("Fikk beskjed om å lukke consument med groupId ${consumer.groupMetadata().groupId()}")
            } catch (exception: Exception) {
                Liveness.kill("Noe galt skjedde i konsument", exception)
            } finally {
                consumer.close()
            }

    }

    @PreDestroy
    override fun close() {
        // Vil kaste WakeupException i konsument slik at den stopper, thread-safe.
        LOG.info("Kaller wakeup for topic $stillingstopic")
        consumer.wakeup()
    }
}
