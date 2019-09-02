package org.rootbr

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import org.camunda.bpm.container.RuntimeContainerDelegate
import org.camunda.bpm.engine.ProcessEngineConfiguration
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.util.ReflectUtil
import org.camunda.bpm.engine.rest.dto.externaltask.CompleteExternalTaskDto
import org.camunda.bpm.engine.rest.dto.externaltask.FetchExternalTasksDto
import org.camunda.bpm.engine.variable.Variables
import org.camunda.spin.Spin.JSON
import org.camunda.spin.plugin.impl.SpinProcessEnginePlugin
import org.slf4j.LoggerFactory

private val logMain = LoggerFactory.getLogger("main")
private const val processFileName = "process.bpmn"

fun main() {
    Javalin
        .create { config ->
            config.defaultContentType = "application/json"
            config.addStaticFiles("/public")
            config.addSinglePageRoot("/", "public/index.html")
            config.enableCorsForAllOrigins()
        }
        .events { event ->
            event.serverStarting {
                RuntimeContainerDelegate.INSTANCE.get().registerProcessEngine(
                    (ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration()
                            as ProcessEngineConfigurationImpl).apply {

                        processEnginePlugins.add(AuditParseListenerPlugin)
                        historyEventHandler = AuditDbHistoryEventHandler()
                        transactionContextFactory = StandaloneTransactionContextFactory()

                        processEnginePlugins.add(SpinProcessEnginePlugin())
                        defaultSerializationFormat = Variables.SerializationDataFormats.JSON.name

                        databaseSchemaUpdate = ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE
                        jdbcUrl = "jdbc:h2:mem:camunda"
                        isJobExecutorActivate = true
                        jdbcMaxActiveConnections = 25
                    }.buildProcessEngine()
                )
            }
            event.serverStarted { deploy(processFileName, ReflectUtil.getResourceAsStream(processFileName)) }
        }
        .start(8080)
        .routes {
            path("/api") {
                path("/external-task") {
                    path("/fetchAndLock") {
                        post { ctx ->
                            ctx.json(
                                rest.getExternalTaskRestService(null)
                                    .fetchAndLock(ctx.body<FetchExternalTasksDto>()))
                        }
                    }
                    path("/:task-id/complete") {
                        post { ctx ->
                            rest.getExternalTaskRestService(null)
                                .getExternalTask(ctx.pathParam(":task-id"))
                                .complete(ctx.body<CompleteExternalTaskDto>())
                            ctx.status(204)
                        }
                    }
                }
                path("/state") {
                    get { it.json(state()) }
                    path("/:processId") {
                        get { it.json(state(it.pathParam("processId"))) }
                    }
                }
                path("/process") {
                    get { it.result(processModel()) }
                    path("/deployment/create") {
                        post {
                            val file = it.uploadedFile(processFileName)!!
                            deploy(file.filename, file.content)
                        }
                    }
                }
                path("/message/:messageName") {
                    post {
                        it.status(204)
                    }
                }
            }
            after("/api/message/:messageName") {
                correlateMessage(it.pathParam(":messageName"), it.queryParam("businessKey"), it.body())
            }
        }
        .ws("/events") { ws ->
            ws.onConnect { ctx ->
                logMain.info("success connect")
                wsConnections[ctx] = SUBSCRIBE_TO_ALL
                updateState(ctx)
            }
            ws.onMessage { ctx ->
                val prop = JSON(ctx.message()).prop("selectedProcessId")
                val process = if (prop.isNull) SUBSCRIBE_TO_ALL else prop.stringValue()
                wsConnections[ctx] = process
                logMain.info("choose process {}", process)
                updateState(ctx)
            }
            ws.onClose { ctx ->
                logMain.info("success disconnect")
                wsConnections.remove(ctx)
            }
        }
}
