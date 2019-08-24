package org.rootbr

import org.camunda.bpm.engine.delegate.ExecutionListener
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import java.util.*

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

//    override fun parseEndEvent(endEventElement: Element?, scope: ScopeImpl?, activity: ActivityImpl?) {
//        activity!!.addExecutionListener(ExecutionListener.EVENTNAME_END, AuditExecutionListener)
//    }

    override fun parseUserTask(userTaskElement: Element?, scope: ScopeImpl?, activity: ActivityImpl) {
        val activityBehavior = activity.activityBehavior
        if (activityBehavior is UserTaskActivityBehavior) {
            activityBehavior.taskDefinition.apply {
                addTaskListener(TaskListener.EVENTNAME_CREATE, AuditTaskListener)
                addTaskListener(TaskListener.EVENTNAME_DELETE, AuditTaskListener)
                addTaskListener(TaskListener.EVENTNAME_COMPLETE, AuditTaskListener)
            }
        }
    }
}
