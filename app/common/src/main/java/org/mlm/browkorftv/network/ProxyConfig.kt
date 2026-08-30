package org.mlm.browkorftv.network

import org.mlm.browkorftv.settings.AppSettings
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI

class ProxyConfigurationException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

data class ProxyConfig(
    val scheme: String,
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?,
) {
    val proxyType: Proxy.Type =
        if (scheme == "http") Proxy.Type.HTTP else Proxy.Type.SOCKS

    val proxy: Proxy =
        Proxy(proxyType, InetSocketAddress(host, port))

    val cacheKey: String
        get() = buildString {
            append(scheme)
            append("://")
            if (username != null) {
                append(username)
                append(':')
                append(password ?: "")
                append('@')
            }
            append(host.lowercase())
            append(':')
            append(port)
        }

    val sanitizedUrl: String
        get() = buildString {
            append(scheme)
            append("://")
            if (username != null) {
                append(username)
                if (password != null) append(":***")
                append('@')
            }
            val needsBrackets = host.contains(':') && !host.startsWith("[") && !host.endsWith("]")
            append(if (needsBrackets) "[$host]" else host)
            append(':')
            append(port)
        }
}

fun parseProxyConfig(settings: AppSettings): ProxyConfig? {
    if (!settings.proxyEnabled) return null
    val raw = settings.proxyUrl.trim()
    if (raw.isBlank()) return null

    val uri = try {
        URI(raw).parseServerAuthority()
    } catch (e: Exception) {
        throw ProxyConfigurationException("Invalid proxy URL", e)
    }

    val scheme = uri.scheme?.lowercase()
        ?: throw ProxyConfigurationException("Proxy URL must include a scheme")

    val normalizedScheme = when (scheme) {
        "http" -> "http"
        "https" -> "http"
        "socks", "socks5", "socks5h" -> "socks5"
        else -> throw ProxyConfigurationException("Supported proxy schemes: http, socks5")
    }

    // Reject path/query/fragment other than "/"
    if (!uri.rawPath.isNullOrBlank() && uri.rawPath != "/") {
        throw ProxyConfigurationException("Proxy URL must not include a path")
    }
    if (uri.rawQuery != null || uri.rawFragment != null) {
        throw ProxyConfigurationException("Proxy URL must not include query or fragment")
    }

    val host = uri.host?.takeIf { it.isNotBlank() }
        ?: throw ProxyConfigurationException("Proxy URL must include a host")

    val port = uri.port.takeIf { it in 1..65535 }
        ?: throw ProxyConfigurationException("Proxy URL must include a valid port")

    val userInfo = uri.userInfo
    val username: String?
    val password: String?

    if (userInfo.isNullOrEmpty()) {
        username = null
        password = null
    } else {
        val idx = userInfo.indexOf(':')
        if (idx < 0) {
            username = userInfo
            password = ""
        } else {
            username = userInfo.substring(0, idx)
            password = userInfo.substring(idx + 1)
        }
    }

    return ProxyConfig(
        scheme = normalizedScheme,
        host = host,
        port = port,
        username = username?.takeIf { it.isNotEmpty() },
        password = password,
    )
}
