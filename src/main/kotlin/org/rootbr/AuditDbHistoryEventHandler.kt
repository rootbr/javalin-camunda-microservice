package org.rootbr

import org.camunda.bpm.engine.impl.cfg.TransactionListener
import org.camunda.bpm.engine.impl.cfg.TransactionState
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.history.event.HistoryEvent
import org.camunda.bpm.engine.impl.history.event.HistoryEventTypes
import org.camunda.bpm.engine.impl.history.handler.DbHistoryEventHandler
import org.camunda.bpm.engine.impl.interceptor.CommandContext

class AuditDbHistoryEventHandler : DbHistoryEventHandler() {
    override fun handleEvent(historyEvent: HistoryEvent) {
        if (historyEvent.isEventOfType(HistoryEventTypes.ACTIVITY_INSTANCE_END)
                || historyEvent.isEventOfType(HistoryEventTypes.ACTIVITY_INSTANCE_MIGRATE)
                || historyEvent.isEventOfType(HistoryEventTypes.ACTIVITY_INSTANCE_START)
                || historyEvent.isEventOfType(HistoryEventTypes.ACTIVITY_INSTANCE_UPDATE) ) {
            Context.getCommandContext().transactionContext.addTransactionListener(
                TransactionState.COMMITTED,
                BroadcastListener(historyEvent.processInstanceId)
            )
        }
        super.handleEvent(historyEvent)
    }
}

class BroadcastListener(val processInstanceId: String): TransactionListener{
    override fun execute(commandContext: CommandContext?) {
        broadcastMessage(processInstanceId)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BroadcastListener
        if (processInstanceId != other.processInstanceId) return false
        return true
    }

    override fun hashCode(): Int {
        return processInstanceId.hashCode()
    }
}
