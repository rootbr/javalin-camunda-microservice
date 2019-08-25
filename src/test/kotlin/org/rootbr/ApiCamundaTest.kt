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
    fun test1() {
        val businessKey = "1"

        concurrentSendMessage(
            businessKey = businessKey,
            messageName = "START",
            repeatTimes = Runtime.getRuntime().availableProcessors()
        )
    }

    @Test
    @DisplayName("businessKey 2 - UPDATE формы")
    fun test2() {
        val businessKey = "2"

        concurrentSendMessage(
            businessKey = businessKey,
            messageName = "START"
        )

        concurrentSendMessage(
            businessKey = businessKey,
            messageName = "UPDATE",
            repeatTimes = Runtime.getRuntime().availableProcessors(),
            needPayload = true
        )
    }

    @Test
    @DisplayName("businessKey 3 - старт подпроцессов")
    fun test3() {
        val businessKey = "3"

        concurrentSendMessage(
            messageName = "START",
            businessKey = businessKey
        )

        concurrentSendMessage(
            businessKey = businessKey,
            messageName = "UPDATE_SUBPROCESS",
            repeatTimes = Runtime.getRuntime().availableProcessors(),
            needPayload = true
        )
    }

    @Test
    @DisplayName("businessKey 4 - старт подпроцессов без переменных")
    fun test4() {
        val businessKey = "4"

        concurrentSendMessage(
            messageName = "START",
            businessKey = businessKey
        )

        concurrentSendMessage(
            businessKey = businessKey,
            messageName = "UPDATE_SUBPROCESS",
            repeatTimes = Runtime.getRuntime().availableProcessors(),
            needPayload = false
        )
    }

    @Test
    @DisplayName("businessKey 5 - старт подпроцессов с повтором до успеха")
    fun test5() {
        val businessKey = "5"

        concurrentSendMessage(
            messageName = "START",
            businessKey = businessKey
        )

        concurrentSendMessage(
            businessKey = businessKey,
            messageName = "UPDATE_SUBPROCESS",
            repeatTimes = Runtime.getRuntime().availableProcessors(),
            needPayload = true,
            needRepeatUntilSuccess = true
        )
    }
}

fun concurrentSendMessage(
    businessKey: String,
    messageName: String,
    needPayload: Boolean = false,
    repeatTimes: Int = 1,
    needRepeatUntilSuccess: Boolean = false
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
            variables = if (needPayload) mapOf("property" to it) else null,
            needRepeatUntilSuccess = needRepeatUntilSuccess
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
    private val variables: Map<String, Any>? = null,
    private val needRepeatUntilSuccess: Boolean = false
) : Thread() {

    override fun run() {
        readyThreadCounter.countDown()
        try {
            callingThreadBlocker.await()
            do {
                val response = post(
                    "http://localhost:8080/api/message/$messageName",
                    params = mapOf("businessKey" to businessKey),
                    headers = if (variables != null) mapOf("Content-Type" to "application/json") else mapOf(),
                    data = if (variables != null) JSONObject(variables) else null
                )
            } while (needRepeatUntilSuccess && response.statusCode >= 300)

        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            completedThreadCounter.countDown()
        }
    }
}

