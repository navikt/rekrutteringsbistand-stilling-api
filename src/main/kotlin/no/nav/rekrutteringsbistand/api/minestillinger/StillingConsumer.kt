package no.nav.rekrutteringsbistand.api.minestillinger

import no.nav.pam.stilling.ext.avro.Ad
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.errors.WakeupException
import rekrutteringsbistand.stilling.indekser.behandling.StillingMottattService
import rekrutteringsbistand.stilling.indekser.utils.Liveness
import rekrutteringsbistand.stilling.indekser.utils.log
import java.io.Closeable
import java.time.Duration

class StillingConsumer(
        private val consumer: Consumer<String, Ad>,
        private val stillingMottattService: StillingMottattService,
) : Closeable {

    fun start(indeksNavn: String) {
        try {
            consumer.subscribe(listOf(stillingstopic))
            log.info(
                "Starter å konsumere topic ${stillingstopic} med groupId ${consumer.groupMetadata().groupId()}, " +
                        "indekserer på indeks '$indeksNavn'"
            )

            while (true) {
                val records: ConsumerRecords<String, Ad> = consumer.poll(Duration.ofSeconds(5))
                if (records.count() == 0) continue

                val stillinger = records.map { it.value() }
                stillingMottattService.behandleStillingerMedRetry(stillinger, indeksNavn)
                consumer.commitSync()

                log.info("Committet offset ${records.last().offset()} til Kafka")
            }
        } catch (exception: WakeupException) {
            log.info("Fikk beskjed om å lukke consument med groupId ${consumer.groupMetadata().groupId()}")
        } catch (exception: Exception) {
            Liveness.kill("Noe galt skjedde i konsument", exception)
        } finally {
            consumer.close()
        }
    }

    override fun close() {
        // Vil kaste WakeupException i konsument slik at den stopper, thread-safe.
        consumer.wakeup()
    }
}
