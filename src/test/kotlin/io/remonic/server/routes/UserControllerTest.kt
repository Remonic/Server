package io.remonic.server.routes

import io.remonic.server.database.User
import io.remonic.server.test
import org.eclipse.jetty.http.HttpMethod
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class UserControllerTest {
    private val dummyPassword = "xxxxxxxxxxx"
    
    @Test fun testRegister() {
        test(HttpMethod.POST, "user/register") {
            case("{}", InvalidRequestError::class)
            case("{'name': 'XXX'}", InvalidRequestError::class)
            case("{'name': 'XXX', 'email': 'xxx@remonic.io', 'password': 'xxx'}", PasswordTooShortError::class) {
                assertEquals(it.errorCode, 2)
            }

            case("{'name': 'XXX', 'email': 'xxx@remonic.io', 'password': '$dummyPassword'}", UserRegisterSuccess::class) {
                assertEquals(it.success, true)
                assertNotNull(it.sessionKey)
            }

            case("{'name': 'XXX', 'email': 'xxx@remonic.io', 'password': '$dummyPassword'}", UserExistsError::class) {
                assertEquals(it.errorCode, 1)
            }
        }
    }
    
    @Test fun testLogin() {
        transaction {
            User.new {
                name = "Login Test"
                email = "login_test@remonic.io"
                password = User.hashPassword(dummyPassword)
            }
        }

        test(HttpMethod.POST, "user/login") {
            case("{}", InvalidRequestError::class)
            
            case("{'email': 'sadsa@remonic.io', 'password': 'xxxxxxxxx'}", NoUserError::class)
            case("{'email': 'login_test@remonic.io', 'password': 'xxx'}", IncorrectPasswordError::class)
            
            case("{'email': 'login_test@remonic.io', 'password': '$dummyPassword'}", UserLoginSuccess::class) {
                assertNotNull(it.sessionKey)
            }
        }
    }
}