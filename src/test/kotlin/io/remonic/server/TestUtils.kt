package io.remonic.server

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.remonic.server.config.DatabaseConfig
import io.remonic.server.config.DatabaseType
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.util.StringContentProvider
import org.eclipse.jetty.http.HttpMethod
import kotlin.reflect.KClass

val testClient = HttpClient()
val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
var testPort = 0

class TestInit {
    @org.junit.jupiter.api.Test fun testServer() {
        loadDatabase(DatabaseConfig(
                databaseType = DatabaseType.H2,
                connectionData = mapOf(
                        Pair("path", "test"),
                        Pair("options", "DB_CLOSE_DELAY=-1")
                )
        ))
        testPort = initServer(0).port()
        System.out.println("Successfully initialized for tests")

        testClient.start()
    }
}

class Test(private val method: HttpMethod, private val path: String) {
    init {
        System.out.println("Testing $method /$path")
    }

    inline fun <T : Any> case(body: String, response: KClass<T>, assertions: (T) -> Unit) {
        assertions(case(body, response))
    }

    fun <T : Any> case(body: String, response: KClass<T>): T {
        System.out.println("Testing with $body to expect ${response.simpleName}")

        val res = testClient.newRequest("http://localhost:$testPort/$path")
                .method(method)
                .content(StringContentProvider(body.replace("'", "\"")))
                .send()
        val content = res.contentAsString

        System.out.println("Response: $content")

        try {
            return moshi.adapter(response.java).fromJson(content)!!
        } catch (ex: Exception) {
            throw AssertionError("Response $content could not be parsed to ${response.simpleName}", ex)
        }
    }
}

inline fun test(method: HttpMethod, path: String, init: Test.() -> Unit) {
    Test(method, path).init()
}