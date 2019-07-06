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

    fun activities(ctx: Context) {
        ctx.json(taskService.createTaskQuery().list().groupingBy { it.taskDefinitionKey }.eachCount())
    }

    fun activitiesProcess(ctx: Context) {
        val processId = ctx.pathParam(":processId")
        ctx.json(taskService.createTaskQuery().processInstanceId(processId).list().groupingBy { it.taskDefinitionKey }.eachCount())
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
        val repositoryService = BpmPlatform.getDefaultProcessEngine().repositoryService
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
