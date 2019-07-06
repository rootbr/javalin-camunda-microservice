package org.rootbr

import io.javalin.Javalin
import org.camunda.bpm.BpmPlatform
import org.camunda.bpm.application.ProcessApplication
import org.camunda.bpm.application.impl.EmbeddedProcessApplication
import org.camunda.bpm.container.RuntimeContainerDelegate
import org.camunda.bpm.engine.ProcessEngineConfiguration
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration
import org.camunda.bpm.engine.variable.Variables
import org.camunda.spin.plugin.impl.SpinProcessEnginePlugin

fun main(args: Array<String>) {
    val app = Javalin
        .create { config ->
            config.defaultContentType = "application/json"
            config.addStaticFiles("/public")
            config.addSinglePageRoot("/", "public/index.html")
            config.enableCorsForAllOrigins()
        }
        .events { event ->
            event.serverStarting {
                val processEngine = (ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                        as StandaloneInMemProcessEngineConfiguration).apply {
                    processEnginePlugins.add(SpinProcessEnginePlugin())
                    defaultSerializationFormat = Variables.SerializationDataFormats.JSON.name
                    databaseSchemaUpdate = ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE
                    jdbcUrl =
                        "jdbc:h2:tcp://localhost/~/tmp/h2dbs/camunda-h2-dbs/process-engine;MVCC=TRUE;TRACE_LEVEL_FILE=0;DB_CLOSE_ON_EXIT=FALSE"
                    isJobExecutorActivate = true
                }.buildProcessEngine()

                RuntimeContainerDelegate.INSTANCE.get().registerProcessEngine(processEngine)
                org.rootbr.ProcessApplication().deploy()
                val runtimeService = BpmPlatform.getDefaultProcessEngine().runtimeService
                val processInstance = runtimeService.startProcessInstanceByKey("Process_13nmxyw")
            }

        }
        .start(8080)

    app.get("/api/activities") { ctx ->
        ctx.json(BpmPlatform.getDefaultProcessEngine().taskService.createTaskQuery().list().groupingBy { it.taskDefinitionKey }.eachCount())
    }
}

@ProcessApplication(name = "process-application", deploymentDescriptors = ["processes.xml"])
class ProcessApplication : EmbeddedProcessApplication()
