package org.rootbr

import khttp.post
import org.json.JSONObject
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@DisplayName("запустить параллельно")
class ApiCamundaTest {
    @Test
    @DisplayName("businessKey 1 - старт процесса с одинаковым ключом")
    fun test0() {
        val businessKey = "1"

        concurrentSendMessage(
            businessKey = businessKey,
            messageName = "START",
            repeatTimes = Runtime.getRuntime().availableProcessors()
        )
    }

    @Test
    @DisplayName("businessKey 2 - UPDATE формы")
    fun test1() {
        val businessKey = "2"

        concurrentSendMessage(
            businessKey = businessKey,
            messageName = "START"
        )

        concurrentSendMessage(
            businessKey = businessKey,
            messageName = "UPDATE",
            repeatTimes = Runtime.getRuntime().availableProcessors()
        )
    }

    @Test
    @DisplayName("businessKey 3 - старт подпроцессов")
    fun test2() {
        val businessKey = "3"

        concurrentSendMessage(
            businessKey = businessKey,
            messageName = "START"
        )

        concurrentSendMessage(
            businessKey = businessKey,
            messageName = "UPDATE_SUBPROCESS",
            needPayload = false,
            repeatTimes = Runtime.getRuntime().availableProcessors()
        )
    }
}

fun concurrentSendMessage(
    businessKey: String,
    messageName: String,
    needPayload: Boolean = false,
    repeatTimes: Int = 1
) {
    val readyThreadCounter = CountDownLatch(repeatTimes)
    val callingThreadBlocker = CountDownLatch(1)
    val completedThreadCounter = CountDownLatch(repeatTimes)
    repeat(repeatTimes) {
        WaitingWorker(
            businessKey = businessKey,
            messageName = messageName,
            callingThreadBlocker = callingThreadBlocker,
            completedThreadCounter = completedThreadCounter,
            readyThreadCounter = readyThreadCounter,
            variables = if (needPayload) mapOf("property" to it) else null
        ).start()
    }
    readyThreadCounter.await()
    callingThreadBlocker.countDown()
    completedThreadCounter.await(2L, TimeUnit.SECONDS)
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
                headers = if (variables != null) mapOf("Content-Type" to "application/json") else mapOf(),
                data = if (variables != null) JSONObject(variables) else null
            )
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            completedThreadCounter.countDown()
        }
    }
}

