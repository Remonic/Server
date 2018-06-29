package io.remonic.server.routes

import io.javalin.Context
import io.remonic.server.database.Session
import io.remonic.server.database.User

open class SuccessfulResponse(val success: Boolean = true)
open class ErrorException(val error: ErrorResponse) : Exception()
open class ErrorResponse(@Transient val httpCode: Int?, val errorCode: Int, val errorMessage: String, val success: Boolean = false) {
    fun ex(): ErrorException {
        return ErrorException(this)
    }
}
class InvalidRequestError(errorMessage: String): ErrorResponse(400, -1, errorMessage)
class UnauthenticatedRequestError: ErrorResponse(401, -2, "No token found")

fun authenticate(context: Context): User {
    val sessionToken = context.cookie("user_session") ?: ""
    return Session.findById(sessionToken)?.user ?: throw UnauthenticatedRequestError().ex()
}

fun deliver(error: ErrorResponse) {
    throw error.ex()
}