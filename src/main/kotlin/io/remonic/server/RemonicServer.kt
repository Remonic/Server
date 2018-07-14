package io.remonic.server

import com.google.gson.GsonBuilder
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import io.javalin.ApiBuilder.path
import io.javalin.ApiBuilder.post
import io.javalin.Javalin
import io.remonic.server.routes.UserController
import io.javalin.translator.json.JavalinJsonPlugin
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.javalin.Context
import io.javalin.translator.json.JsonToObjectMapper
import io.javalin.translator.json.ObjectToJsonMapper
import io.remonic.server.config.DatabaseConfig
import io.remonic.server.config.FileConfig
import io.remonic.server.config.Settings
import io.remonic.server.database.Sessions
import io.remonic.server.database.UserPermissions
import io.remonic.server.database.Users
import io.remonic.server.routes.ErrorException
import io.remonic.server.routes.ErrorResponse
import io.remonic.server.routes.InvalidRequestError
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileReader
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.util.*

val configJson = GsonBuilder()
        .setPrettyPrinting()
        .create()!!
var config = loadConfig(FileConfig(), File("config.json"))

fun main(args: Array<String>) {
    loadDatabase(config.database)
    initServer(config.port)
}

fun initServer(port: Int): Javalin {
    val app = Javalin.create()
            .port(port)
            .enableCorsForAllOrigins()
            .enableStandardRequestLogging()
            .start()
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
        handleError(InvalidRequestError(ex.message ?: "JSON did not match request schema"), context)
    }

    app.exception(JsonEncodingException::class.java) { ex, context ->
        handleError(InvalidRequestError("Invalid JSON"), context)
    }

    app.exception(ErrorException::class.java) { ex, context ->
        handleError(ex.error, context)
    }

    app.routes {
        path("user") {
            post("register", userController::register)
            post("login", userController::login)
        }
    }

    return app
}

fun handleError(error: ErrorResponse, context: Context) {
    context.status(error.httpCode ?: 400)
    context.json(error)
}

fun loadDatabase(config: DatabaseConfig) {
    // for now
    config.databaseType.createConnection(config.connectionData)

    // create our tables
    transaction {
        create(Users)
        create(Sessions)
        create(UserPermissions)
        create(Settings)
    }
}

fun <T : Any> loadConfig(defaultConfig: T, file: File): T {
    fun saveConfig(config: T) {
        Files.write(file.toPath(), Collections.singleton(configJson.toJson(config)))
    }

    if (!file.exists()) {
        file.createNewFile()
        saveConfig(defaultConfig)
        return defaultConfig
    }

    val config = configJson.fromJson(FileReader(file), defaultConfig.javaClass)

    saveConfig(config)
    return config
}