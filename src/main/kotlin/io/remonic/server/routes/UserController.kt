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
            if (User.find { Users.email eq request.email}.count() >= 1) {
                deliver(UserExistsError())
            }

            if (request.password.length < 8) {
                deliver(PasswordTooShortError())
            }

            val createdUser = User.new {
                name = request.name
                email = request.email
                password = BCrypt.hashpw(request.password, BCrypt.gensalt(12))
            }

            val session = Session.new {
                user = createdUser
            }

            context.json(UserRegisterSuccess(session.token.value))
        }
    }
}

data class UserRegisterRequest(val name: String, val email: String, val password: String)
class UserRegisterSuccess(val sessionKey: String): SuccessfulResponse()
class UserExistsError: ErrorResponse(400, 1, "A user by that email already exists")
class PasswordTooShortError: ErrorResponse(400, 2, "Password provided is lower than 8 characters")