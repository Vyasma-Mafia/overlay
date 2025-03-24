package com.stoum.overlay.service.polemica

import com.stoum.overlay.getLogger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty(
    value = ["app.polemicaEnable", "app.crawlScheduler.enable"],
    havingValue = "true"
)
@EnableScheduling
class PolemicaCrawlSchedulerComponent(val polemicaService: PolemicaService) {

    @Scheduled(fixedRateString = "#{@crawlScheduler.interval.toMillis()}", timeUnit = TimeUnit.MILLISECONDS)
    private fun update() {
        try {
            getLogger().info("Game updates crawl started")
            polemicaService.crawl()
        } catch (e: Exception) {
            getLogger().error("Error on crawling: " + e.message, e)
        }
    }
}
