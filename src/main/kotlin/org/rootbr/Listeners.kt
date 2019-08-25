package org.rootbr

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.cfg.TransactionState
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import org.slf4j.LoggerFactory
import java.util.*

private val logListeners = LoggerFactory.getLogger("listeners")

object AuditParseListenerPlugin : AbstractProcessEnginePlugin() {
    override fun preInit(processEngineConfiguration: ProcessEngineConfigurationImpl) {
        var preParseListeners: MutableList<BpmnParseListener>? = processEngineConfiguration.customPreBPMNParseListeners
        if (preParseListeners == null) {
            preParseListeners = ArrayList()
            processEngineConfiguration.customPreBPMNParseListeners = preParseListeners
        }
        preParseListeners.add(AuditParseListener)
    }
}

object AuditParseListener : AbstractBpmnParseListener() {
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
                addTaskListener(TaskListener.EVENTNAME_CREATE, AuditTaskListenerCreated)
                addTaskListener(TaskListener.EVENTNAME_DELETE, AuditTaskListenerFinished)
                addTaskListener(TaskListener.EVENTNAME_COMPLETE, AuditTaskListenerFinished)
            }
        }
    }
}

object AuditTaskListenerFinished : TaskListener {
    override fun notify(task: DelegateTask) {
        Context.getCommandContext().transactionContext.addTransactionListener(TransactionState.COMMITTED) {
            logListeners.info("task \"{}\" was finished", task.name)
        }
    }
}
object AuditTaskListenerCreated : TaskListener {
    override fun notify(task: DelegateTask) {
        Context.getCommandContext().transactionContext.addTransactionListener(TransactionState.COMMITTED) {
            logListeners.info("task \"{}\" was created", task.name)
        }
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
