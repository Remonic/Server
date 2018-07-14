package io.remonic.server.routes

import io.javalin.Context
import io.remonic.server.config.RemonicSettings
import io.remonic.server.database.Session
import io.remonic.server.database.User
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.concurrent.TimeUnit

class UserController {
    fun register(context: Context) {
        val request = context.bodyAsClass(UserRegisterRequest::class.java)

        transaction {
            if (!RemonicSettings.REGISTRATION_PERMITTED.asBoolean()) {
                deliver(RegistrationNotPermitted())
            }

            if (!request.email.contains('@')) {
                deliver(InvalidEmailError())
            }

            if (User.findByEmail(request.email) != null) {
                deliver(UserExistsError())
            }

            if (request.password.length < 8) {
                deliver(PasswordTooShortError())
            }

            if (request.name.isBlank()) {
                deliver(InvalidNameError())
            }

            val createdUser = User.new {
                name = request.name
                email = request.email
                password = User.hashPassword(request.password)
            }

            val session = Session.new {
                user = createdUser
            }

            context.json(UserRegisterSuccess(session.token.value))
        }
    }

    fun login(context: Context) {
        val request = context.bodyAsClass(UserLoginRequest::class.java)

        transaction {
            val currentUser = User.findByEmail(request.email) ?: throw NoUserError().ex()

            if (!BCrypt.checkpw(request.password, currentUser.password))
                deliver(IncorrectPasswordError())

            val session = Session.new {
                user = currentUser
            }

            context.cookie("user_session", session.token.value, TimeUnit.DAYS.toSeconds(90).toInt())
            context.json(UserLoginSuccess(session.token.value))
        }
    }
}

data class UserRegisterRequest(val name: String, val email: String, val password: String)
class UserRegisterSuccess(val sessionKey: String): SuccessfulResponse()
class UserExistsError: ErrorResponse(400, 1, "A user by that email already exists")
class PasswordTooShortError: ErrorResponse(400, 2, "Password provided is lower than 8 characters")
class InvalidEmailError: ErrorResponse(400, 3, "Email provided is invalid!")
class InvalidNameError: ErrorResponse(400, 4, "Must contain non-whitespace characters")
class RegistrationNotPermitted: ErrorResponse(400, 5, "Registration is not permitted")

data class UserLoginRequest(val email: String, val password: String)
class UserLoginSuccess(val sessionKey: String): SuccessfulResponse()
class NoUserError: ErrorResponse(400, 1, "No user by that email exists")
class IncorrectPasswordError: ErrorResponse(400, 2, "Incorrect password")