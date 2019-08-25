package org.rootbr

import io.javalin.websocket.WsContext
import org.camunda.spin.Spin.JSON
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

val processMap = ConcurrentHashMap<WsContext, String>()

private val logUpdateState = LoggerFactory.getLogger("update state")

fun broadcastMessage(processId: String = "all") {
    logUpdateState.info("update state {}", processId)
    processMap
        .filter { it.key.session.isOpen }
        .filter { it.value == processId || it.value == "all"}
        .forEach {
            it.key.send(JSON(if (it.value == "all") state() else state(processId)).unwrap())
        }
}
