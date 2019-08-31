package org.rootbr

import io.javalin.websocket.WsContext
import org.camunda.spin.Spin.JSON
import org.camunda.spin.json.SpinJsonNode
import org.rootbr.EventTypes.ACTIVITY_INSTANCE_UPDATE
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

const val SUBSCRIBE_TO_ALL = "all"

val processMap = ConcurrentHashMap<WsContext, String>()

private val logUpdateState = LoggerFactory.getLogger("update state")

fun updateState(wsContext: WsContext) {
    logUpdateState.info("update state ws {}", wsContext)
    val subscribed = processMap[wsContext]!!
    if (subscribed == SUBSCRIBE_TO_ALL) {
        wsContext.send(prepareMessage(ACTIVITY_INSTANCE_UPDATE, JSON(state())))
    } else {
        wsContext.send(prepareMessage(ACTIVITY_INSTANCE_UPDATE, JSON(state(subscribed))))
    }
}

fun broadcastMessage(processId: String) {
    logUpdateState.info("update state process {}", processId)
    val openSessions = processMap.filter { it.key.session.isOpen }

    val subscribedToOneProcess = openSessions.filter { it.value == processId }
    if (subscribedToOneProcess.isNotEmpty()) {
        val state = state(processId)
        subscribedToOneProcess.forEach { it.key.send(prepareMessage(ACTIVITY_INSTANCE_UPDATE, JSON(state))) }
    }

    val subscribedToAllProcesses = openSessions.filter { it.value == SUBSCRIBE_TO_ALL }
    if (subscribedToAllProcesses.isNotEmpty()) {
        val state = state()
        subscribedToAllProcesses.forEach { it.key.send(prepareMessage(ACTIVITY_INSTANCE_UPDATE, JSON(state))) }
    }
}

fun prepareMessage(type: EventTypes, payload: SpinJsonNode) = JSON("{}")
    .prop("type", type.toString())
    .prop("payload", payload)
    .unwrap()

fun broadcastWsMessage(type: EventTypes, payload: SpinJsonNode, processId: String) {
    processMap
        .filter { it.key.session.isOpen }
        .filter { it.value == processId || it.value == SUBSCRIBE_TO_ALL }
        .forEach { it.key.send(prepareMessage(type, payload)) }
}


enum class EventTypes {
    ACTIVITY_INSTANCE_UPDATE, TASK_INSTANCE_UPDATE, ERROR
}
