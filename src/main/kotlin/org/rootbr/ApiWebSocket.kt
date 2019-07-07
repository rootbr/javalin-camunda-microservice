package org.rootbr

import io.javalin.websocket.WsContext
import org.camunda.spin.Spin.JSON
import java.util.concurrent.ConcurrentHashMap

val processMap = ConcurrentHashMap<WsContext, String>()

val WsContext.processId: String get() = this.pathParam("process-id")

fun broadcastMessage(processId: String = "all") = processMap
    .filter { it.key.session.isOpen }
    .filter { it.value == processId }
    .forEach {
        it.key.send(JSON(if (processId == "all") state() else state(processId)).unwrap())
    }
