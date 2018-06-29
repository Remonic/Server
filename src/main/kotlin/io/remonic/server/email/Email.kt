package io.remonic.server.email

import io.remonic.server.config.RemonicSettings
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

inline fun sendEmail(body: String, build: EmailBuilder.() -> Unit) {
    val builder = EmailBuilder()
    builder.build()

    val currentConfig = RemonicSettings.EMAIL.asJson(EmailConfig::class)
    val message = MimeMessage(Session.getInstance(currentConfig.toProperties(), currentConfig.getAuthenticator()))

    message.addHeader("Content-Type", "text/HTML; charset=UTF-8")

    message.replyTo = InternetAddress.parse(currentConfig.emailReplyTo, false)
    message.subject = builder.subject
    message.sentDate = Date()
    message.setRecipients(Message.RecipientType.TO, builder.recipients)

    message.setFrom(InternetAddress(currentConfig.emailFrom, "Remonic No Reply"))
    message.setText("") // todo generate body from scopes w/ resources (mustache?)

    Transport.send(message)
}

class EmailBuilder(val scopes: MutableMap<String, Any> = hashMapOf(), var subject: String = "", var recipients: Array<InternetAddress> = arrayOf()) {
    fun recipients(recipients: String) {
        this.recipients = InternetAddress.parse(recipients, false)
    }

    fun scope(key: String, value: Any) {
        scopes[key] = value
    }
}


data class EmailConfig(
        val emailEnabled: Boolean? = false,
        val smtpAddress: String? = null,
        val smtpPort: Int? = 465,
        val username: String? = null,
        val password: String? = null,
        val tls: Boolean? = false,
        val ssl: Boolean? = false,
        val emailFrom: String? = username,
        val emailReplyTo: String? = emailFrom
) {
    fun toProperties(): Properties {
        return properties {
            put("mail.smtp.host", smtpAddress)
            put("mail.smtp.port", smtpPort)

            // this may look dumb, but tls can be null
            if (tls == true) {
                put("mail.smtp.auth", true)
                put("mail.smtp.starttls.enable", true)
            } else if (ssl == true) {
                put("mail.smtp.auth", true)
                put("mail.smtp.ssl.enable", true)
                put("mail.smtp.socketFactory.port", smtpPort)
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            }
        }
    }

    fun getAuthenticator(): Authenticator? {
        if (tls == true || ssl == true) {
            return object: Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password)
                }
            }
        }

        return null
    }

    private class PropertiesBuilder(val props: Properties = Properties()) {
        fun put(key: String, value: Any?) {
            if (value != null) {
                props[key] = value
            }
        }
    }

    private inline fun properties(init: PropertiesBuilder.() -> Unit): Properties {
        val builder = PropertiesBuilder()

        builder.init()
        return builder.props
    }
}