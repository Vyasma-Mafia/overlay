package com.stoum.overlay.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Tournament settings not found")
class TournamentSettingsNotFoundException : RuntimeException("Tournament settings not found")