package org.rootbr

import org.camunda.bpm.BpmPlatform
import org.camunda.spin.Spin
import org.camunda.spin.json.SpinJsonNode
import org.slf4j.LoggerFactory
import java.io.InputStream

private val logCamunda = LoggerFactory.getLogger("camunda")
private val runtimeService = BpmPlatform.getDefaultProcessEngine().runtimeService
private val historyService = BpmPlatform.getDefaultProcessEngine().historyService
private val repositoryService = BpmPlatform.getDefaultProcessEngine().repositoryService

private val columnsProcesses = listOf(ColumnDto("id", "id", true), ColumnDto("businessKey", "businessKey"))
private val columnsProcess = listOf(ColumnDto("variable", "id", true), ColumnDto("value", "value"))

fun state(processId: String): Map<String, List<Any>> {
    val variables = runtimeService.getVariables(processId)
        .map { VariablesDto(it.key, (it.value as SpinJsonNode).unwrap()) }
        .toList()
    return mapOf(
        "activities" to historicActivities(processId),
        "rows" to variables,
        "columns" to columnsProcess
    )
}

fun state(): Map<String, List<Any>> {
    val processes = runtimeService.createProcessInstanceQuery().list().map {
        ProcessInstanceDto(it.processInstanceId, it.businessKey)
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
    return query.list()
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

fun correlateMessage(messageName: String, businessKey: String?, body: String?) = runtimeService
    .createMessageCorrelation(messageName).apply {
        businessKey?.let { this.processInstanceBusinessKey(it) }
        if (!body.isNullOrEmpty()) {
            val json = Spin.JSON(body)
            if (!json.isNull) setVariable(messageName, json)
        }
        correlateAll()
    }

data class ProcessInstanceDto(val id: String, val businessKey: String?)
data class VariablesDto(val id: String, val value: Any?)
data class ColumnDto(val label: String, val name: String, val uniqueId: Boolean = false, val sort: Boolean = true)
data class HistoricActivitiesStatDto(
    val id: String,
    val instances: Int = 0,
    val finished: Int = 0,
    val canceled: Int = 0
)

enum class Scope { FINISHED, CANCELED, ACTIVE }
