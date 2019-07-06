package org.rootbr

import io.javalin.http.Context
import org.camunda.bpm.BpmPlatform
import org.camunda.spin.Spin

object ApiCamunda {
    val taskService = BpmPlatform.getDefaultProcessEngine().taskService
    val repositoryService = BpmPlatform.getDefaultProcessEngine().repositoryService
    val runtimeService = BpmPlatform.getDefaultProcessEngine().runtimeService

    fun activities(ctx: Context) {
        ctx.json(taskService.createTaskQuery().list().groupingBy { it.taskDefinitionKey }.eachCount())
    }

    fun process(ctx: Context) {
        ctx.result(repositoryService.getProcessModel(processDefinition().id))
    }

    fun message(ctx: Context) {
        val messageName = ctx.pathParam(":messageName")
        runtimeService.createMessageCorrelation(messageName)
            .setVariable(messageName, Spin.JSON(ctx.body()))
            .correlateAll()
        ctx.status(204)
    }

    private fun processDefinition() =
        repositoryService.createProcessDefinitionQuery().processDefinitionKey("process").singleResult()
}
