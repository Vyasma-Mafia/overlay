package com.stoum.overlay.controller

import com.google.gson.Gson
import com.stoum.overlay.model.GameInfo
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import java.util.*


@Controller
class OverlayController(
    val emitterService: EmitterService,
    val gameRepository: GameRepository,
) {
    @RequestMapping("/{id}/overlay")
    fun overlay(@PathVariable id: String, model: Model): String? {
        model.addAttribute("id", id)
        return "overlay"
    }

    @RequestMapping("/{id}/control")
    fun control(@PathVariable id: String, model: Model): String? {
        model.addAttribute("id", id)
        return "control-panel"
    }
}