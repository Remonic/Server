package io.remonic.server.database

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.and
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom

object Users: IntIdTable() {
    val email = varchar("email", 50).uniqueIndex()
    val name = varchar("name", 32)
    val password = varchar("password", 60)
}

class User(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<User>(Users) {
        fun findByEmail(email: String): User? {
            return User.find { Users.email eq email }.firstOrNull()
        }

        fun hashPassword(password: String): String {
            return BCrypt.hashpw(password, BCrypt.gensalt(12))
        }
    }

    var email by Users.email
    var name by Users.name
    var password by Users.password

    val sessions by Session referrersOn Sessions.user
    val permissions by UserPermission referrersOn UserPermissions.user

    fun hasPermission(permission: String): Boolean {
        return permissions.any {
            it.permission.equals(permission, true)
        }
    }
}

object Sessions: IdTable<String>() {
    override val id = varchar("token", 32).primaryKey().clientDefault {
        val keyIndex = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val key = StringBuilder()
        val random = SecureRandom()

        for (i in 1..32) {
            key.append(keyIndex[random.nextInt(keyIndex.length)])
        }

        key.toString()
    }.entityId()
    val user = reference("user", Users)
}

class Session(id: EntityID<String>): Entity<String>(id) {
    companion object : EntityClass<String, Session>(Sessions)

    var token by Sessions.id
    var user by User referencedOn Sessions.user
}

object UserPermissions: IntIdTable() {
    val user = reference("user", Users)
    val permission = varchar("permission", 32)

    init {
        index(true, user, permission)
    }
}

class UserPermission(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<UserPermission>(UserPermissions) {
        fun findByUser(user: User, permission: String): SizedIterable<UserPermission> {
            return UserPermission.find {
                (UserPermissions.user eq user.id) and (UserPermissions.permission eq permission)
            }
        }
    }

    var user by User referencedOn UserPermissions.user
    var permission by UserPermissions.permission
}
