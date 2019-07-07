package org.rootbr

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.websocket.WsContext
import org.camunda.bpm.BpmPlatform
import org.camunda.bpm.container.RuntimeContainerDelegate
import org.camunda.bpm.engine.ProcessEngineConfiguration
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import org.camunda.bpm.engine.variable.Variables
import org.camunda.spin.plugin.impl.SpinProcessEnginePlugin
import org.slf4j.LoggerFactory
import java.util.*

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
                        processEnginePlugins.add(UserTaskParseListenerPlugin)
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
                path("/state") {
                    get(ApiCamunda::state)
                    path("/:processId") {
                        get(ApiCamunda::stateProcess)
                    }
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
        .ws("/events/:process-id") { ws ->
            ws.onConnect { ctx ->
                log.info("success connect {}", ctx.processId)
                ctx.send(ctx.processId)
            }
            ws.onMessage { ctx ->
                log.info("receive message {}", ctx.message())
            }
            ws.onClose { ctx ->
                log.info("success disconnect {}", ctx.processId)
            }
        }
}

val WsContext.processId: String get() = this.pathParam("process-id")

object UserTaskParseListenerPlugin : AbstractProcessEnginePlugin() {
    override fun preInit(processEngineConfiguration: ProcessEngineConfigurationImpl) {
        var preParseListeners: MutableList<BpmnParseListener>? = processEngineConfiguration.customPreBPMNParseListeners
        if (preParseListeners == null) {
            preParseListeners = ArrayList()
            processEngineConfiguration.customPreBPMNParseListeners = preParseListeners
        }
        preParseListeners.add(UserTaskParseListener)
    }
}

object UserTaskParseListener : AbstractBpmnParseListener() {
    override fun parseProcess(processElement: Element?, processDefinition: ProcessDefinitionEntity) {
        processDefinition.addListener(
            ExecutionListener.EVENTNAME_START,
            UniquenessBusinessKeyProcessStartEventListener
        )
    }

    override fun parseUserTask(userTaskElement: Element?, scope: ScopeImpl?, activity: ActivityImpl) {
        val activityBehavior = activity.activityBehavior
        if (activityBehavior is UserTaskActivityBehavior) {
            activityBehavior.taskDefinition.apply {
                addTaskListener(TaskListener.EVENTNAME_CREATE, AuditTaskListener)
                addTaskListener(TaskListener.EVENTNAME_DELETE, AuditTaskListener)
            }
        }
    }
}

object AuditTaskListener : TaskListener {
    val log = LoggerFactory.getLogger(AuditTaskListener::class.java)
    override fun notify(delegateTask: DelegateTask) {
        log.info(delegateTask.eventName)
    }
}

object UniquenessBusinessKeyProcessStartEventListener : ExecutionListener {
    override fun notify(execution: DelegateExecution) {
        val businessKey = execution.processBusinessKey
        val runtimeService = execution.processEngineServices.runtimeService
        if (businessKey != null) {
            val processDefinitionId = execution.processDefinitionId
            val count = runtimeService
                .createExecutionQuery()
                .processDefinitionId(processDefinitionId)
                .processInstanceBusinessKey(businessKey)
                .count()
            if (count > 0)
                throw NotUniqueBusinessKeyException("Business key [$businessKey] not unique in runtime for definition ID [$processDefinitionId].")
        }
    }
}

class NotUniqueBusinessKeyException : RuntimeException {
    protected constructor() : super() {}
    constructor(message: String) : super(message) {}

    companion object {
        private val serialVersionUID = -1L
    }
}
