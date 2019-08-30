package org.rootbr

import io.javalin.websocket.WsContext
import org.camunda.spin.Spin.JSON
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

const val SUBSCRIBE_TO_ALL = "all"

val processMap = ConcurrentHashMap<WsContext, String>()

private val logUpdateState = LoggerFactory.getLogger("update state")

fun updateState(wsContext: WsContext) {
    logUpdateState.info("update state ws {}", wsContext)
    val subscribed = processMap[wsContext]!!
    if (subscribed == SUBSCRIBE_TO_ALL) {
        wsContext.send(JSON(state()).unwrap())
    } else {
        wsContext.send(JSON(state(subscribed)).unwrap())
    }
}

fun broadcastMessage(processId: String) {
    logUpdateState.info("update state process {}", processId)
    val openSessions = processMap.filter { it.key.session.isOpen }

    val subscribedToOneProcess = openSessions.filter { it.value == processId }
    if (subscribedToOneProcess.isNotEmpty()) {
        val state = state(processId)
        subscribedToOneProcess.forEach { it.key.send(JSON(state).unwrap()) }
    }

    val subscribedToAllProcesses = openSessions.filter { it.value == SUBSCRIBE_TO_ALL }
    if (subscribedToAllProcesses.isNotEmpty()) {
        val state = state()
        subscribedToAllProcesses.forEach { it.key.send(JSON(state).unwrap()) }
    }
}
