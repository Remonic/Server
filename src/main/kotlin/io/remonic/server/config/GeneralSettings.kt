package io.remonic.server.config

import com.google.gson.GsonBuilder
import io.remonic.server.config
import io.remonic.server.email.EmailConfig
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

val settingsGson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT)
        .disableHtmlEscaping()
        .serializeNulls()
        .create()!!

enum class RemonicSettings(val defaultValue: Any) {
    REGISTRATION_PERMITTED(false),
    EMAIL(EmailConfig());

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

    fun <T : Any> asJson(clazz: KClass<T>): T {
        if (!defaultValue.javaClass.isAssignableFrom(clazz.java)) {
            throw IllegalArgumentException("${clazz.simpleName} is not setting's type ${defaultValue.javaClass.simpleName}")
        }

        return settingsGson.fromJson(value(), clazz.java)
    }

    fun setValue(newValue: Any) {
        if (!defaultValue.javaClass.isAssignableFrom(newValue.javaClass)) {
            throw IllegalArgumentException("${newValue.javaClass.simpleName} is not setting's type ${defaultValue.javaClass.simpleName}")
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
}