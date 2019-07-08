package org.rootbr

import khttp.post
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONObject
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
    @Test
    fun test1() {
        post(
            "http://localhost:8080/api/message/START",
            params = mapOf("businessKey" to "3"),
            headers = mapOf("Content-Type" to "application/json"),
            data = JSONObject(mapOf("name" to "value"))
        )
        GlobalScope.async {
            post(
                "http://localhost:8080/api/message/UPDATE",
                params = mapOf("businessKey" to "3"),
                headers = mapOf("Content-Type" to "application/json"),
                data = JSONObject(mapOf("name" to "value"))
            )
        }
        GlobalScope.async {
            post(
                "http://localhost:8080/api/message/UPDATE",
                params = mapOf("businessKey" to "3"),
                headers = mapOf("Content-Type" to "application/json"),
                data = JSONObject(mapOf("name" to "value"))
            )
        }

    }
}

data class HistoryActivity(val activityId: String, val isCanceled: Boolean, val isComplete: Boolean)
