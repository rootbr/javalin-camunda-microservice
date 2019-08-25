package org.rootbr

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import org.camunda.bpm.container.RuntimeContainerDelegate
import org.camunda.bpm.engine.ProcessEngineConfiguration
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.util.ReflectUtil
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
                        processEnginePlugins.add(SpinProcessEnginePlugin())
                        processEnginePlugins.add(AuditParseListenerPlugin)
                        historyEventHandler = AuditDbHistoryEventHandler()
                        defaultSerializationFormat = Variables.SerializationDataFormats.JSON.name
                        databaseSchemaUpdate = ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE
                        transactionContextFactory = StandaloneTransactionContextFactory()
                        jdbcUrl = "jdbc:h2:mem:camunda"
                        isJobExecutorActivate = true
                    }.buildProcessEngine()
                )
            }
            event.serverStarted { deploy(processFileName, ReflectUtil.getResourceAsStream(processFileName)) }
        }
        .start(8080)
        .routes {
            path("/api") {
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
                        correlateMessage(it.pathParam(":messageName"), it.queryParam("businessKey"), it.body())
                        it.status(204)
                    }
                }
            }
        }
        .ws("/events") { ws ->
            ws.onConnect { ctx ->
                logMain.info("success connect")
                processMap.put(ctx, "all")
                broadcastMessage()
            }
            ws.onMessage { ctx ->
                val prop = JSON(ctx.message()).prop("selectedProcessId")
                val process = if (prop.isNull) "all" else prop.stringValue()
                processMap.put(ctx, process)
                logMain.info("choose process {}", process)
                broadcastMessage(process)
            }
            ws.onClose { ctx ->
                logMain.info("success disconnect")
                processMap.remove(ctx)
            }
        }
}
