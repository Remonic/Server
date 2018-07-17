package io.remonic.server.routes

import io.javalin.Context
import io.remonic.server.config
import io.remonic.server.config.DatabaseConfig
import io.remonic.server.config.FileConfig
import io.remonic.server.configFile
import io.remonic.server.loadDatabase
import io.remonic.server.saveConfig
import org.jetbrains.exposed.sql.transactions.transaction

class SetupController {
    fun post(context: Context) {
        val user = authenticate(context)
        val request = context.bodyAsClass(SetupRequest::class.java)

        if (config.setupComplete) {
            deliver(SetupAlreadyCompleteError())
        }

        config = FileConfig(
                config.port,
                request.database,
                config.overriddenNodeSettings,
                true
        )
        saveConfig(config, configFile)
        loadDatabase(config.database)

        transaction {
            user.admin = true
        }

        // todo email config (SMTP server) if unconfigured

        context.json(SetupCompleteResponse())
    }

    fun get(context: Context) {
        context.json(SetupStatusResponse(config.setupComplete))
    }
}

data class SetupRequest(val database: DatabaseConfig)
class SetupCompleteResponse: SuccessfulResponse()
class SetupAlreadyCompleteError: ErrorResponse(400, 1, "Setup has already been performed")

class SetupStatusResponse(val setup: Boolean): SuccessfulResponse()