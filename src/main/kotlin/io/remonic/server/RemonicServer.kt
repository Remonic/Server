package io.remonic.server

import com.google.gson.GsonBuilder
import com.squareup.moshi.JsonDataException
import io.javalin.ApiBuilder.path
import io.javalin.ApiBuilder.post
import io.javalin.Javalin
import io.remonic.server.routes.UserController
import org.jetbrains.exposed.sql.Database
import io.javalin.translator.json.JavalinJsonPlugin
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.javalin.Context
import io.javalin.translator.json.JsonToObjectMapper
import io.javalin.translator.json.ObjectToJsonMapper
import io.remonic.server.database.Users
import io.remonic.server.routes.ErrorException
import io.remonic.server.routes.ErrorResponse
import io.remonic.server.routes.InvalidRequestError
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.reflect.Modifier


fun main(args: Array<String>) {
    loadConfigs()
    loadDatabase()
    initServer()
}

fun initServer() {
    val app = Javalin.start(8080)
    val userController = UserController()

    val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    val gson = GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT)
            .disableHtmlEscaping()
            .create()

    JavalinJsonPlugin.jsonToObjectMapper = object: JsonToObjectMapper {
        override fun <T> map(json: String, targetClass: Class<T>): T {
            return moshi.adapter(targetClass)!!.fromJson(json)!!
        }
    }

    JavalinJsonPlugin.objectToJsonMapper = object: ObjectToJsonMapper {
        override fun map(obj: Any): String {
            return gson.toJson(obj)
        }
    }

    app.exception(JsonDataException::class.java) { ex, context ->
        handleError(InvalidRequestError(ex), context)
    }

    app.exception(ErrorException::class.java) { ex, context ->
        handleError(ex.error, context)
    }

    app.routes {
        path("user") {
            post("register", userController::register)
        }
    }
}

fun handleError(error: ErrorResponse, context: Context) {
    context.status(error.httpCode)
    context.json(error)
}

fun loadDatabase() {
    // for now
    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

    // create our tables
    transaction {
        create(Users)
    }
}

fun loadConfigs() {
    //
}