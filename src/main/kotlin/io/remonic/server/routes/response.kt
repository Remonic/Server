package io.remonic.server.routes

open class SuccessfulResponse(val success: Boolean = true)
open class ErrorException(val error: ErrorResponse) : Exception()
open class ErrorResponse(@Transient val httpCode: Int?, val errorCode: Int, val errorMessage: String, val success: Boolean = false) {
    fun ex(): ErrorException {
        return ErrorException(this)
    }
}
class InvalidRequestError(errorMessage: String): ErrorResponse(400, -1, errorMessage)

fun deliver(error: ErrorResponse) {
    throw error.ex()
}