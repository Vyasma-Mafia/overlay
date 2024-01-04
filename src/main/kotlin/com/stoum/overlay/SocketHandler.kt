package com.stoum.overlay

import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger


@Component
class SocketHandler : TextWebSocketHandler() {
    var sessions: MutableList<WebSocketSession> = CopyOnWriteArrayList()
    val log = Logger.getAnonymousLogger()

    public override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        log.info("Got message: $message")
        for (webSocketSession in sessions) {
            webSocketSession.sendMessage(TextMessage("Hello "))
        }
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("registering session ${session.id}")

        sessions.add(session)
    }
}
