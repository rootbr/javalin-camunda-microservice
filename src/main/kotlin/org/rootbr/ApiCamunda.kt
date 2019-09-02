package org.rootbr

import io.javalin.plugin.json.JavalinJackson
import org.camunda.bpm.BpmPlatform
import org.camunda.bpm.engine.OptimisticLockingException
import org.camunda.bpm.engine.rest.impl.AbstractProcessEngineRestServiceImpl
import org.camunda.spin.Spin.JSON
import org.camunda.spin.json.SpinJsonNode
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI

private val logCamunda = LoggerFactory.getLogger("camunda")
private val runtimeService = BpmPlatform.getDefaultProcessEngine().runtimeService
private val historyService = BpmPlatform.getDefaultProcessEngine().historyService
private val repositoryService = BpmPlatform.getDefaultProcessEngine().repositoryService

val rest = DefaultProcessEngineRestServiceImpl()
class DefaultProcessEngineRestServiceImpl : AbstractProcessEngineRestServiceImpl() {
    override fun getRelativeEngineUri(engineName: String?): URI {
        return URI.create("/")
    }
    override fun getObjectMapper()= JavalinJackson.getObjectMapper()
}

private val columnsProcesses = listOf(
    ColumnDto("id", "id", true),
    ColumnDto("businessKey", "businessKey"),
    ColumnDto("state", "state")
)
private val columnsProcess = listOf(
    ColumnDto("variable", "id", true),
    ColumnDto("value", "value")
)

fun state(processId: String): Map<String, List<Any>> {
    val variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processId).list()
        .map {
            VariablesDto(
                it.name,
                if (it.value == null) null else
                    if (it.value is SpinJsonNode) (it.value as SpinJsonNode).unwrap() else
                        it.value
            )
        }
        .toList()
    return mapOf(
        "activities" to historicActivities(processId),
        "rows" to variables,
        "columns" to columnsProcess
    )
}

fun state(): Map<String, List<Any>> {

    val processes = historyService.createHistoricProcessInstanceQuery().list().map {
        ProcessInstanceDto(it.id, it.businessKey, it.state)
    }
    return mapOf(
        "activities" to historicActivities(),
        "rows" to processes,
        "columns" to columnsProcesses
    )
}

fun historicActivities(processId: String? = null): List<HistoricActivitiesStatDto> {
    val query = historyService.createHistoricActivityInstanceQuery()
    if (!processId.isNullOrEmpty()) query.processInstanceId(processId)
    val list = query.list()
    return list
        .groupBy { it.activityId }
        .mapValues {
            it.value.groupingBy {
                if (it.isCanceled) Scope.CANCELED
                else if (it.endTime != null) Scope.FINISHED
                else Scope.ACTIVE
            }.eachCount()
        }
        .map {
            HistoricActivitiesStatDto(
                it.key,
                it.value[Scope.ACTIVE] ?: 0,
                it.value[Scope.FINISHED] ?: 0,
                it.value[Scope.CANCELED] ?: 0
            )
        }
        .toList()
}

fun processDefinition() = repositoryService
    .createProcessDefinitionQuery()
    .processDefinitionKey("process")
    .orderByProcessDefinitionVersion().desc()
    .list().first()

fun processModel() = repositoryService.getProcessModel(processDefinition().id)

fun deploy(filename: String, content: InputStream) = repositoryService.createDeployment()
    .addInputStream(filename, content)
    .name("process")
    .enableDuplicateFiltering(true)
    .deployWithResult()
    .deployedProcessDefinitions?.let { logCamunda.info("Deploy resource \"${it[0].key}\", version ${it[0].version}") }

private fun messageEventReceived(messageName: String, executionId: String, body: SpinJsonNode?, processId: String?) {
    try {
        if (body != null && !body.isNull) {
            runtimeService.messageEventReceived(messageName, executionId, mapOf(messageName to body))
        } else {
            runtimeService.messageEventReceived(messageName, executionId)
        }
    } catch (e: OptimisticLockingException) {
        logCamunda.warn(e.message, e)
        broadcastWsMessage(
            EventTypes.ERROR,
            JSON("{}")
                .prop("message", e.message)
                .prop("type", "error"),
            processId ?: SUBSCRIBE_TO_ALL
        )
    } catch (e: NotUniqueBusinessKeyException) {
        logCamunda.warn(e.message)
        broadcastWsMessage(
            EventTypes.ERROR,
            JSON("{}")
                .prop("message", e.message)
                .prop("type", "error"),
            processId ?: SUBSCRIBE_TO_ALL
        )
    } catch (e: RuntimeException) {
        broadcastWsMessage(
            EventTypes.ERROR,
            JSON("{}")
                .prop("message", e.message)
                .prop("type", "error"),
            processId ?: SUBSCRIBE_TO_ALL
        )
    }
}

private fun messageEventReceivedForStart(messageName: String, businessKey: String?, body: SpinJsonNode?) {
    try {
        if (body != null && !body.isNull) {
            if (businessKey.isNullOrEmpty())
                runtimeService.startProcessInstanceByMessage(messageName, mapOf(messageName to body))
            else
                runtimeService.startProcessInstanceByMessage(messageName, businessKey, mapOf(messageName to body))
        } else {
            if (businessKey.isNullOrEmpty())
                runtimeService.startProcessInstanceByMessage(messageName)
            else
                runtimeService.startProcessInstanceByMessage(messageName, businessKey)
        }
    } catch (e: OptimisticLockingException) {
        logCamunda.warn(e.message, e)
        broadcastWsMessage(
            EventTypes.ERROR,
            JSON("{}")
                .prop("message", e.message)
                .prop("type", "error"),
            SUBSCRIBE_TO_ALL
        )
    } catch (e: NotUniqueBusinessKeyException) {
        logCamunda.warn(e.message)
        broadcastWsMessage(
            EventTypes.ERROR,
            JSON("{}")
                .prop("message", e.message)
                .prop("type", "error"),
            SUBSCRIBE_TO_ALL
        )
    } catch (e: RuntimeException) {
        broadcastWsMessage(
            EventTypes.ERROR,
            JSON("{}")
                .prop("message", e.message)
                .prop("type", "error"),
            SUBSCRIBE_TO_ALL
        )
    }
}

fun correlateMessage(messageName: String, businessKey: String?, body: String?) {
    val json = if (!body.isNullOrEmpty()) JSON(body) else null

    val list = runtimeService.createEventSubscriptionQuery()
        .eventType("message")
        .eventName(messageName)
        .list()

    if (list.isNotEmpty()) {
        list.forEach {
            if (it.processInstanceId == null) {
                messageEventReceivedForStart(messageName, businessKey, json)
            } else {
                val processInstance =
                    runtimeService.createProcessInstanceQuery().processInstanceId(it.processInstanceId).singleResult()
                if (processInstance.businessKey == businessKey) {
                    messageEventReceived(messageName, it.executionId, json, it.processInstanceId)
                }
            }
        }
    }
}

data class ProcessInstanceDto(val id: String, val businessKey: String?, val state: String)
data class VariablesDto(val id: String, val value: Any?)
data class ColumnDto(val label: String, val name: String, val uniqueId: Boolean = false, val sort: Boolean = true)
data class HistoricActivitiesStatDto(
    val id: String,
    val instances: Int = 0,
    val finished: Int = 0,
    val canceled: Int = 0
)

enum class Scope { FINISHED, CANCELED, ACTIVE }
