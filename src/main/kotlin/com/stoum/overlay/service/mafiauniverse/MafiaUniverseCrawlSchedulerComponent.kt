package com.stoum.overlay.service.mafiauniverse

import com.stoum.overlay.getLogger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty(
    value = ["app.mafiauniverse.enable", "app.mafiauniverse.crawlScheduler.enable"],
    havingValue = "true"
)
@EnableScheduling
class MafiaUniverseCrawlSchedulerComponent(val mafiaUniverseService: MafiaUniverseService) {

    @Scheduled(
        fixedRateString = "#{@mafiaUniverseCrawlScheduler.interval.toMillis()}",
        timeUnit = TimeUnit.MILLISECONDS
    )
    private fun update() {
        try {
            // getLogger().info("MafiaUniverse game updates crawl started")
            mafiaUniverseService.crawl()
        } catch (e: Exception) {
            getLogger().error("Error on MafiaUniverse crawling: " + e.message, e)
        }
    }
}
