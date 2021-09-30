package no.nav.rekrutteringsbistand.api.inkluderingsmuligheter

import no.nav.pam.stilling.ext.avro.Ad
import org.apache.kafka.clients.consumer.Consumer
import org.springframework.stereotype.Component
import java.io.Closeable
import javax.annotation.PreDestroy

@Component
class StillingConsumer(
    private val consumer: Consumer<String, Ad>,
    private val inkluderingsmuligheterService: InkluderingsmuligheterService
) : Closeable {

//    @Scheduled(fixedRate = Long.MAX_VALUE) // Kjøres kun en gang, i egen tråd/task, ved startup
//    fun start() {
//        try {
//            consumer.subscribe(listOf(stillingstopic))
//
//            LOG.info("Starter å konsumere topic $stillingstopic med groupId ${consumer.groupMetadata().groupId()}")
//
//            while (true) {
//                val records: ConsumerRecords<String, Ad> = consumer.poll(Duration.ofSeconds(5))
//                if (records.count() == 0) continue
//
//                val stilling = records.map { it.value() }
//                val stillingsIder = stilling.map { it.uuid }
//                LOG.info("Stillinger mottatt: $stillingsIder")
//
//                stilling.forEach {
//                    inkluderingsmuligheterService.lagreInkluderingsmuligheter(it)
//                }
//                consumer.commitSync()
//
//                LOG.info("Committet offset ${records.last().offset()} til Kafka")
//            }
//        } catch (exception: WakeupException) {
//            LOG.info("Fikk beskjed om å lukke consument med groupId ${consumer.groupMetadata().groupId()}")
//        } catch (exception: Exception) {
//            LOG.error("Noe galt skjedde i konsument, stopper stilling-konsument", exception)
//        } finally {
//            consumer.close()
//        }
//    }
//
    @PreDestroy
    override fun close() {
        // Vil kaste WakeupException i konsument slik at den stopper, thread-safe.
//        LOG.info("Kaller wakeup for topic $stillingstopic")
//        consumer.wakeup()
    }
}
