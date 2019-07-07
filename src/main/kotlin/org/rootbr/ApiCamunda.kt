package org.rootbr

import io.javalin.http.Context
import org.camunda.bpm.BpmPlatform
import org.camunda.spin.Spin
import org.camunda.spin.json.SpinJsonNode
import org.slf4j.LoggerFactory

object ApiCamunda {
    val log = LoggerFactory.getLogger(ApiCamunda.javaClass)
    val taskService = BpmPlatform.getDefaultProcessEngine().taskService
    val repositoryService = BpmPlatform.getDefaultProcessEngine().repositoryService
    val runtimeService = BpmPlatform.getDefaultProcessEngine().runtimeService
    val historyService = BpmPlatform.getDefaultProcessEngine().historyService

    fun activities(ctx: Context) {
        ctx.json(taskService.createTaskQuery().list().groupingBy { it.taskDefinitionKey }.eachCount())
    }

    fun state(ctx: Context) {
        val activities =
            historyService.createHistoricActivityStatisticsQuery(processDefinition().id).includeFinished()
                .includeCanceled().list()
        val processes = runtimeService.createProcessInstanceQuery().list().map {
            ProcessInstanceDto(
                it.processInstanceId,
                it.businessKey
            )
        }
        val columns = mutableListOf<Column>()
        columns.add(Column("id", "id", true))
        columns.add(Column("businessKey", "businessKey"))
        ctx.json(
            mapOf(
                "activities" to activities,
                "rows" to processes,
                "columns" to columns
            )
        )
    }

    fun stateProcess(ctx: Context) {
        val processId = ctx.pathParam("processId")
        val activites = historyService
            .createHistoricActivityInstanceQuery().processInstanceId(processId).list()
            .groupBy { it.activityId }
            .mapValues {
                it.value.groupingBy {
                    if (it.removalTime != null) Scope.CANCELED
                    else if (it.endTime != null) Scope.FINISHED
                    else Scope.ACTIVE
                }.eachCount()
            }
            .map { HistoricActivities(it.key,
                it.value[Scope.ACTIVE] ?: 0,
                it.value[Scope.FINISHED] ?: 0,
                it.value[Scope.CANCELED] ?: 0
            ) }
            .toList()
        val variables = runtimeService.getVariables(processId)
            .map { VariablesDto(it.key, (it.value as SpinJsonNode).unwrap()) }
            .toList()
        val columns = mutableListOf<Column>()
        columns.add(Column("variable", "id", true))
        columns.add(Column("value", "value"))
        ctx.json(
            mapOf(
                "activities" to activites,
                "rows" to variables,
                "columns" to columns
            )
        )
    }

    fun activitiesProcess(ctx: Context) {
        val processId = ctx.pathParam(":processId")
        ctx.json(runtimeService.getActiveActivityIds(processId).groupingBy { it }.eachCount())
    }

    fun variables(ctx: Context) {
        ctx.json(
            runtimeService.getVariables(ctx.pathParam(":processId"))
                .map { VariablesDto(it.key, (it.value as SpinJsonNode).unwrap()) }
                .toList()
        )
    }

    fun process(ctx: Context) {
        ctx.result(repositoryService.getProcessModel(processDefinition().id))
    }

    fun deploy(ctx: Context) {
        val uploadedFile = ctx.uploadedFile("process.bpmn")!!
        val result = repositoryService.createDeployment()
            .addInputStream(uploadedFile.filename, uploadedFile.content)
            .name("process")
            .enableDuplicateFiltering(true)
            .deployWithResult()
        result.deployedProcessDefinitions?.let {
            log.info("Deploy resource \"{}\", version {}", it[0].key, it[0].version)
        }
    }

    fun processes(ctx: Context) {
        ctx.json(runtimeService.createProcessInstanceQuery().list().map {
            ProcessInstanceDto(
                it.processInstanceId,
                it.businessKey
            )
        })
    }

    fun message(ctx: Context) {
        val messageName = ctx.pathParam(":messageName")
        runtimeService.createMessageCorrelation(messageName).apply {
            ctx.queryParam("businessKey")?.let { this.processInstanceBusinessKey(it) }
            setVariable(messageName, Spin.JSON(ctx.body()))
            correlateAll()
        }
        ctx.status(204)
    }

    private fun processDefinition() =
        repositoryService.createProcessDefinitionQuery().processDefinitionKey("process").orderByProcessDefinitionVersion().desc().list().first()
}

data class ProcessInstanceDto(val id: String, val businessKey: String?)
data class VariablesDto(val id: String, val value: Any?)
data class Column(val label: String, val name: String, val uniqueId: Boolean = false, val sort: Boolean = true)
data class HistoricActivities(
    val id: String,
    val instances: Int = 0,
    val finished: Int = 0,
    val canceled: Int = 0
)

enum class Scope { FINISHED, CANCELED, ACTIVE }
