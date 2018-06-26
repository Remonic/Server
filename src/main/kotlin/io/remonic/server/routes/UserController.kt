package io.remonic.server.routes

import io.javalin.Context
import io.remonic.server.database.Session
import io.remonic.server.database.User
import io.remonic.server.database.Users
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

class UserController {
    fun register(context: Context) {
        val request = context.bodyAsClass(UserRegisterRequest::class.java)

        transaction {
            if (User.findByEmail(request.email) != null) {
                deliver(UserExistsError())
            }

            if (request.password.length < 8) {
                deliver(PasswordTooShortError())
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

            context.json(UserLoginSuccess(session.token.value))
        }
    }
}

data class UserRegisterRequest(val name: String, val email: String, val password: String)
class UserRegisterSuccess(val sessionKey: String): SuccessfulResponse()
class UserExistsError: ErrorResponse(400, 1, "A user by that email already exists")
class PasswordTooShortError: ErrorResponse(400, 2, "Password provided is lower than 8 characters")

data class UserLoginRequest(val email: String, val password: String)
class UserLoginSuccess(val sessionKey: String): SuccessfulResponse()
class NoUserError: ErrorResponse(400, 3, "No user by that email exists")
class IncorrectPasswordError: ErrorResponse(400, 4, "Incorrect password")