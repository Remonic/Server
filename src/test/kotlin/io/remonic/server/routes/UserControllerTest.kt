package io.remonic.server.routes

import io.remonic.server.test
import org.eclipse.jetty.http.HttpMethod
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class UserControllerTest {
    @Test fun testRegister() {
        test(HttpMethod.POST, "user/register") {
            case("{}", InvalidRequestError::class.java)
            case("{'name': 'XXX'}", InvalidRequestError::class.java)
            case("{'name': 'XXX', 'email': 'xxx@gmail.com', 'password': 'xxx'}", PasswordTooShortError::class.java) {
                assertEquals(it.errorCode, 2)
            }

            case("{'name': 'XXX', 'email': 'xxx@gmail.com', 'password': 'xxxxxxxxxxx'}", SuccessfulResponse::class.java) {
                assertEquals(it.success, true)
            }

            case("{'name': 'XXX', 'email': 'xxx@gmail.com', 'password': 'xxxxxxxxxxx'}", UserExistsError::class.java) {
                assertEquals(it.errorCode, 1)
            }
        }
    }
}