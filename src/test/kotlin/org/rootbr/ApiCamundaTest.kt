package org.rootbr

import org.junit.jupiter.api.Test

class ApiCamundaTest {
    @Test
    fun test0() {
        val list = listOf(
            HistoryActivity("activity_01", false, false),
            HistoryActivity("activity_02", false, false),
            HistoryActivity("activity_02", true, false),
            HistoryActivity("activity_02", false, true),
            HistoryActivity("activity_02", false, true),
            HistoryActivity("activity_02", false, true)
        )


        val m = list.groupBy { it.activityId }.mapValues { it.value.groupingBy { if(it.isComplete)Scope.FINISHED else if(it.isCanceled) Scope.CANCELED else Scope.ACTIVE}.eachCount() }


        println()
    }
}

data class HistoryActivity(val activityId: String, val isCanceled: Boolean, val isComplete: Boolean)


