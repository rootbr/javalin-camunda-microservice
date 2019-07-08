package org.rootbr

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.cfg.TransactionState
import org.camunda.bpm.engine.impl.context.Context
import org.slf4j.LoggerFactory

object AuditTaskListener : TaskListener {
    val log = LoggerFactory.getLogger(AuditTaskListener::class.java)
    override fun notify(task: DelegateTask) {
        val processInstanceId = task.processInstanceId
        Context.getCommandContext().transactionContext.addTransactionListener(TransactionState.COMMITTED) {
            broadcastMessage(processInstanceId)
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




