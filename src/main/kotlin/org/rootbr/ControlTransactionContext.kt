package org.rootbr

import org.camunda.bpm.engine.impl.cfg.TransactionContext
import org.camunda.bpm.engine.impl.cfg.TransactionContextFactory
import org.camunda.bpm.engine.impl.cfg.TransactionListener
import org.camunda.bpm.engine.impl.cfg.TransactionState
import org.camunda.bpm.engine.impl.cfg.standalone.StandaloneTransactionContext
import org.camunda.bpm.engine.impl.interceptor.CommandContext
import java.util.*

class StandaloneTransactionContextFactory : TransactionContextFactory {
    override fun openTransactionContext(commandContext: CommandContext): TransactionContext {
        return StandaloneTransactionContextWithSetListeners(commandContext)
    }
}

class StandaloneTransactionContextWithSetListeners(commandContext: CommandContext) : StandaloneTransactionContext(
    commandContext
) {
    var listeners: MutableMap<TransactionState, MutableSet<TransactionListener>?>? = null

    override fun addTransactionListener(transactionState: TransactionState, transactionListener: TransactionListener) {
        if (listeners == null) {
            listeners = EnumMap(TransactionState::class.java)
        }
        var transactionListeners: MutableSet<TransactionListener>? = listeners!![transactionState]
        if (transactionListeners == null) {
            transactionListeners = mutableSetOf()
            listeners!![transactionState] = transactionListeners
        }
        transactionListeners.add(transactionListener)
    }

    override fun fireTransactionEvent(transactionState: TransactionState) {
        this.setLastTransactionState(transactionState)
        if (listeners == null) {
            return
        }
        val transactionListeners = listeners!![transactionState] ?: return
        for (transactionListener in transactionListeners) {
            transactionListener.execute(commandContext)
        }
    }
}
