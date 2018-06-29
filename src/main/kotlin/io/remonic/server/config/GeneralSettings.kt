package io.remonic.server.config

import io.remonic.server.config
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.transactions.transaction

enum class RemonicSettings(val defaultValue: Any) {
    REGISTRATION_PERMITTED(false);

    val setting: Setting?
        get() = Setting.find { Settings.key eq name }.firstOrNull()

    fun value(): String {
        // priority:
        // node setting > database > default value
        return config.overriddenNodeSettings[name] ?: setting?.value ?: defaultValue.toString()
    }

    fun asBoolean(): Boolean {
        return value().toBoolean()
    }

    fun asInt(): Int {
        return value().toInt()
    }

    fun setValue(newValue: Any) {
        if (!defaultValue.javaClass.isAssignableFrom(newValue.javaClass)) {
            throw IllegalArgumentException("${defaultValue.javaClass.simpleName} is not setting's type ${newValue.javaClass.simpleName}")
        }

        transaction {
            val currentSetting = setting

            if (currentSetting != null) {
                currentSetting.value = newValue.toString()
            } else {
                Setting.new {
                    key = name
                    value = newValue.toString()
                }
            }
        }
    }
}

object Settings: IntIdTable() {
    val key = varchar("key", 50).uniqueIndex()
    val value = varchar("value", 50)
}

class Setting(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<Setting>(Settings)

    var key by Settings.key
    var value by Settings.value

    fun asInt(): Int {
        return value.toInt()
    }

    fun asBoolean(): Boolean {
        return value.toBoolean()
    }
}