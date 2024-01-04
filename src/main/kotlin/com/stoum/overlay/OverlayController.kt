package com.stoum.overlay

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping




@Controller
class OverlayController {
    @RequestMapping("/overlay")
    fun overlay(): String? {
        return "overlay"
    }
}