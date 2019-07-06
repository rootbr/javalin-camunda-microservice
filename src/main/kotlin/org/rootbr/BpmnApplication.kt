package org.rootbr

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import org.camunda.bpm.BpmPlatform
import org.camunda.bpm.container.RuntimeContainerDelegate
import org.camunda.bpm.engine.ProcessEngineConfiguration
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration
import org.camunda.bpm.engine.variable.Variables
import org.camunda.spin.plugin.impl.SpinProcessEnginePlugin
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger("main")

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
                    (ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                            as StandaloneInMemProcessEngineConfiguration).apply {
                        processEnginePlugins.add(SpinProcessEnginePlugin())
                        defaultSerializationFormat = Variables.SerializationDataFormats.JSON.name
                        databaseSchemaUpdate = ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE
                        jdbcUrl =
                            "jdbc:h2:tcp://localhost/~/tmp/h2dbs/camunda-h2-dbs/process-engine;MVCC=TRUE;TRACE_LEVEL_FILE=0;DB_CLOSE_ON_EXIT=FALSE"
                        isJobExecutorActivate = true
                    }.buildProcessEngine()
                )
            }
            event.serverStarted {
                val repositoryService = BpmPlatform.getDefaultProcessEngine().repositoryService
                val result = repositoryService.createDeployment()
                    .addClasspathResource("process.bpmn")
                    .name("process")
                    .enableDuplicateFiltering(true)
                    .deployWithResult()
                result.deployedProcessDefinitions?.let {
                    log.info("Deploy resource \"{}\", version {}", it[0].key, it[0].version)
                }
            }
        }
        .start(8080)
        .routes {
            path("/api") {
                path("/activities") {
                    get(ApiCamunda::activities)
                    path("/:processId") {
                        get(ApiCamunda::activitiesProcess)
                    }
                }
                path("/variables/:processId") {
                    get(ApiCamunda::variables)
                }
                path("/processes") {
                    get(ApiCamunda::processes)
                }
                path("/process") {
                    get(ApiCamunda::process)
                    path("/deployment/create") {
                        post(ApiCamunda::deploy)
                    }
                }
                path("/message/:messageName") {
                    post(ApiCamunda::message)
                }

            }
        }
}
