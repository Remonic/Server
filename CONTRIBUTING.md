# Contributing

This document is a directory to the resources you'll need to
contribute to this project, so best of luck!

### Language

First and foremost, Remonic Server uses Kotlin. If it's new to
you (maybe you're coming from Java?), you can find the language
reference [here](https://kotlinlang.org/docs/reference/coding-conventions.html#idiomatic-use-of-language-features)

### Use of Expose

We use expose to interact with Remonic Server's SQL-based database,
and we strictly use the DAO pattern. You can learn how to use it
[here](https://github.com/JetBrains/Exposed/wiki/DAO)

### Use of Javalin

[This section](https://javalin.io/documentation#context) of Javalin's
documentation will be the most useful in knowing how to manage the Context
object at an endpoint. Make sure to define your path in [RemonicServer.kt](src/main/kotlin/io/remonic/server/RemonicServer.kt)
as the rest of them are. If you're unsure how something is done, just take
a look at some of the other code as it's a great place to start. I'll tell
you some of the most basic elements of this upfront though:

#### 1. You have to define your types

You have to define what can come in, and what type of output can come out.
This means errors, success, etc. This both helps other developers figure out
what's going on, as well as aid input validation for you. So for example, our
`/user/register` method types might look something like this:

```kotlin
data class UserRegisterRequest(val name: String, val email: String, val password: String)
class UserRegisterSuccess: SuccessfulResponse()
class UserExistsError: ErrorResponse(400, 1, "A user by that email already exists")
class PasswordTooShortError: ErrorResponse(400, 2, "Password provided is lower than 8 characters")
```

The register request is what will be coming in and by our definition: none of
the fields can be null. If we make them nullable by adding a `?`, then they
will become nullable to the validator! Easy as that.

#### 2. Errors are passed through the `deliver` method

This will halt execution and you won't need to return
