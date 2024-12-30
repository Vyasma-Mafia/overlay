package com.stoum.overlay.controller

import com.stoum.overlay.service.polemica.PolemicaService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/polemica")
@ConditionalOnProperty(value = ["app.polemicaEnable"], havingValue = "true")
class PolemicaController(
    val polemicaService: PolemicaService
) {
    @PostMapping("/_force_recheck")
    fun forceRecheck() {
        polemicaService.crawl()
    }
}
