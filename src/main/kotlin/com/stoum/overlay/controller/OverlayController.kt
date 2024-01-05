package com.stoum.overlay.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping




@Controller
class OverlayController {
    @RequestMapping("/{id}/overlay")
    fun overlay(@PathVariable id: String, model: Model): String? {
        model.addAttribute("id", id)
        return "overlay"
    }
}