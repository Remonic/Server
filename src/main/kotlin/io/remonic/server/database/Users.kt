package io.remonic.server.database

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Users: IntIdTable() {
    val email = varchar("email", 50).uniqueIndex()
    val name = varchar("name", 32)
    val password = varchar("password", 60)
}

class User(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var email by Users.email
    var name by Users.name
    var password by Users.password
}