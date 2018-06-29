package io.remonic.server.config

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

data class FileConfig(
        val port: Int = 8080,
        val database: DatabaseConfig = DatabaseConfig(),
        val overriddenNodeSettings: Map<String, String> = hashMapOf()
)

data class DatabaseConfig(
        val databaseType: DatabaseType = DatabaseType.SQLITE,
        val connectionData: Map<String, String> = mapOf(
                Pair("path", "database.db")
        )
)

enum class DatabaseType(val driver: String) {
    MYSQL("com.mysql.jdbc.Driver"),
    POSTGRES("org.postgresql.Driver"),
    H2("org.h2.Driver") {
        override fun createConnection(data: Map<String, String>): Database {
            return Database.connect(
                    url = "jdbc:h2:${data["method"] ?: "mem"}:${data["path"] ?: ""};${data["options"] ?: ""}",
                    driver = driver
            )
        }
    },
    SQLITE("org.sqlite.JDBC") {
        override fun createConnection(data: Map<String, String>): Database {
            val database = Database.connect(
                    url = "jdbc:sqlite:${data["path"]}",
                    driver = driver
            )
            // SQLite supports only TRANSACTION_SERIALIZABLE and TRANSACTION_READ_UNCOMMITTED
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

            return database
        }
    },
    // note: this technically won't work until
    // we figure out how to package their driver
    // legally. *rolls eyes*
    ORACLE("oracle.jdbc.OracleDriver") {
        override fun createConnection(data: Map<String, String>): Database {
            var url = "jdbc:oracle:thin:@"

            if (data.containsKey("service")) {
                url += "//${getHost(data)}/${data["service"] ?: ""}"
            } else {
                url += "${getHost(data)}/${data["tns"] ?: ""}"
            }

            return Database.connect(
                    url = url,
                    driver = driver,
                    user = data["user"] ?: "",
                    password = data["password"] ?: ""
            )
        }
    },
    SQL_SERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver") {
        override fun createConnection(data: Map<String, String>): Database {
            return Database.connect(
                    url = "jdbc:sqlserver:${getHost(data)};${data["options"] ?: ""}",
                    driver = driver
            )
        }
    }
    ;

    open fun createConnection(data: Map<String, String>): Database {
        return Database.connect(
                url = "jdbc:${name.toLowerCase()}://${getHost(data)}/${data["database"]}",
                driver = driver,
                user = data["user"] ?: "",
                password = data["password"] ?: ""
        )
    }

    fun getHost(data: Map<String, String>): String {
        return data["host"] + (if (data.containsKey("port")) ":${data["port"]}" else "")
    }
}