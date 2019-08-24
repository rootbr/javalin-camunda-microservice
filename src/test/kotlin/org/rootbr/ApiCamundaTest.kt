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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


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

    @Test
    @DisplayName("execute update sub process three times in parallel, but only one update will be success")
    fun test2() {
        val businessKey = "4"; //SecureRandom().nextInt(10000).toString()

        post(
            "http://localhost:8080/api/message/START",
            params = mapOf("businessKey" to businessKey),
            headers = mapOf("Content-Type" to "application/json")
        )

        val readyThreadCounter = CountDownLatch(2)
        val callingThreadBlocker = CountDownLatch(1)
        val completedThreadCounter = CountDownLatch(2)
        repeat(Runtime.getRuntime().availableProcessors()) {
            WaitingWorker(
                businessKey = businessKey,
                messageName = "UPDATE_SUBPROCESS",
                callingThreadBlocker = callingThreadBlocker,
                completedThreadCounter = completedThreadCounter,
                readyThreadCounter = readyThreadCounter,
                variables = mapOf("v" to it)
            ).start()
        }
        readyThreadCounter.await()
        callingThreadBlocker.countDown()
        completedThreadCounter.await(3L, TimeUnit.SECONDS)
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

class WaitingWorker(
    private val businessKey: String,
    private val messageName: String,
    private val readyThreadCounter: CountDownLatch,
    private val callingThreadBlocker: CountDownLatch,
    private val completedThreadCounter: CountDownLatch,
    private val variables: Map<String, Any>? = null
) : Thread() {

    override fun run() {
        readyThreadCounter.countDown()
        try {
            callingThreadBlocker.await()
            post(
                "http://localhost:8080/api/message/$messageName",
                params = mapOf("businessKey" to businessKey),
                headers = mapOf("Content-Type" to "application/json"),
                data = JSONObject(variables)
            )
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            completedThreadCounter.countDown()
        }
    }
}

