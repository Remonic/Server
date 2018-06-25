package io.remonic.server.routes

import com.squareup.moshi.JsonDataException

open class SuccessfulResponse(val success: Boolean = true)
open class ErrorException(val error: ErrorResponse) : Exception()
open class ErrorResponse(@Transient val httpCode: Int, val errorCode: Int, val errorMessage: String, val success: Boolean = false)
class InvalidRequestError(ex: JsonDataException): ErrorResponse(400, -1, ex.message!!)

fun deliver(error: ErrorResponse) {
    throw ErrorException(error)
}