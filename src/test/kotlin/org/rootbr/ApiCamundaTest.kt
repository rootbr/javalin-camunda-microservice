package org.rootbr

import khttp.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.security.SecureRandom

class ApiCamundaTest {
    @Test
    @DisplayName("execute launch three times in parallel with same businessKey and all process will be launched")
    fun test0() = runBlocking {
        val businessKey = SecureRandom().nextInt(10000).toString()

        parallelExecute(businessKey, "START", repeat = 3)
    }

    @Test
    @DisplayName("execute update three times in parallel, but only one update will be success")
    fun test1() = runBlocking {
        val businessKey = SecureRandom().nextInt(10000).toString()

        parallelExecute(businessKey, "START")

        parallelExecute(businessKey, "UPDATE", repeat = 3)
    }
}

suspend fun parallelExecute(
    businessKey: String,
    messageName: String,
    variables: Map<String, String?>? = null,
    repeat: Int = 1
) {
    coroutineScope {
        repeat(repeat) {
            launch(Dispatchers.Default) {
                post(
                    "http://localhost:8080/api/message/$messageName",
                    params = mapOf("businessKey" to businessKey),
                    headers = mapOf("Content-Type" to "application/json"),
                    data = JSONObject(variables)
                )
            }
        }
    }
}

