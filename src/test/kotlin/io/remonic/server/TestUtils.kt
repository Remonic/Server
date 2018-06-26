package io.remonic.server

import com.google.gson.GsonBuilder
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.util.StringContentProvider
import org.eclipse.jetty.http.HttpMethod
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

val testClient = HttpClient()
val gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT)
        .disableHtmlEscaping()
        .create()
var testPort = 0

class TestInit {
    @org.junit.jupiter.api.Test fun testServer() {
        loadDatabase()
        testPort = initServer(0).port()
        System.out.println("Successfully initialized for tests")

        testClient.start()
    }
}

class Test(private val method: HttpMethod, private val path: String) {
    inline fun <T : Any> case(body: String, response: KClass<T>, assertions: (T) -> Unit) {
        assertions(case(body, response))
    }

    fun <T : Any> case(body: String, response: KClass<T>): T {
        val res = testClient.newRequest("http://localhost:$testPort/$path")
                .method(method)
                .content(StringContentProvider(body.replace("'", "\"")))
                .send()
        val content = res.contentAsString

        try {
            return gson.fromJson(content, response.java)
        } catch (ex: Exception) {
            throw AssertionError("Response $content could not be parsed to ${response.simpleName}", ex)
        }
    }
}

inline fun test(method: HttpMethod, path: String, init: Test.() -> Unit) {
    Test(method, path).init()
}