package org.rootbr

import org.camunda.bpm.engine.impl.cfg.TransactionState
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.history.event.HistoryEvent
import org.camunda.bpm.engine.impl.history.event.HistoryEventTypes
import org.camunda.bpm.engine.impl.history.handler.DbHistoryEventHandler

class AuditDbHistoryEventHandler : DbHistoryEventHandler() {
    override fun handleEvent(historyEvent: HistoryEvent) {
        if (historyEvent.isEventOfType(HistoryEventTypes.ACTIVITY_INSTANCE_END)
                || historyEvent.isEventOfType(HistoryEventTypes.ACTIVITY_INSTANCE_MIGRATE)
                || historyEvent.isEventOfType(HistoryEventTypes.ACTIVITY_INSTANCE_START)
                || historyEvent.isEventOfType(HistoryEventTypes.ACTIVITY_INSTANCE_UPDATE) ) {
            val processInstanceId = historyEvent.processInstanceId
            Context.getCommandContext().transactionContext.addTransactionListener(TransactionState.COMMITTED) {
                broadcastMessage(processInstanceId)
            }
        }
        super.handleEvent(historyEvent)
    }
}
