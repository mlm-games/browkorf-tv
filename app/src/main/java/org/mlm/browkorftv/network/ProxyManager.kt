package org.mlm.browkorftv.network

import android.util.Log
import android.util.Base64
import androidx.webkit.ProxyConfig as WebKitProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import org.mlm.browkorftv.settings.AppSettings
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URL
import java.util.concurrent.Executor

object ProxyManager {
    private const val TAG = "ProxyManager"

    @Volatile
    private var currentConfig: ProxyConfig? = null

    private val proxyAuthenticator = object : Authenticator() {
        @Volatile
        var config: ProxyConfig? = null

        override fun getPasswordAuthentication(): PasswordAuthentication? {
            val cfg = config ?: return null
            val user = cfg.username ?: return null
            // Only respond to proxy auth (not origin server auth). Check host/port match.
            if (requestingPort != -1 && requestingPort != cfg.port) return null
            val host = requestingHost
            if (host != null && !host.equals(cfg.host, ignoreCase = true)) {
                val siteAddr = requestingSite?.hostAddress
                if (siteAddr != null) {
                    val cfgAddrs = runCatching { InetAddress.getAllByName(cfg.host) }.getOrNull()
                    if (cfgAddrs == null || cfgAddrs.none { it.hostAddress == siteAddr }) {
                        if (host != null || requestingSite != null) return null
                    }
                } else {
                    return null
                }
            }
            // For SOCKS, ensure protocol is SOCKS; for HTTP, allow any (CONNECT)
            if (cfg.scheme == "socks5") {
                val protocol = requestingProtocol?.substringBefore('/')?.lowercase()
                if (protocol != null && protocol != "socks" && protocol != "socks5") return null
            }
            return PasswordAuthentication(user, (cfg.password ?: "").toCharArray())
        }
    }.also {
        Authenticator.setDefault(it)
    }

    fun current(): ProxyConfig? = currentConfig

    fun apply(settings: AppSettings) {
        val parsed = try {
            parseProxyConfig(settings)
        } catch (e: ProxyConfigurationException) {
            Log.w(TAG, "Invalid proxy config: ${e.message}")
            null
        }
        applyConfig(parsed)
    }

    fun applyConfig(config: ProxyConfig?) {
        currentConfig = config
        proxyAuthenticator.config = config
        applySystemProperties(config)
        applyWebViewProxy(config)
        tryApplyGeckoProxy(config)
    }

    private fun applySystemProperties(config: ProxyConfig?) {
        try {
            if (config == null) {
                System.clearProperty("http.proxyHost")
                System.clearProperty("http.proxyPort")
                System.clearProperty("https.proxyHost")
                System.clearProperty("https.proxyPort")
                System.clearProperty("http.nonProxyHosts")
                System.clearProperty("https.nonProxyHosts")
                System.clearProperty("socksProxyHost")
                System.clearProperty("socksProxyPort")
                System.clearProperty("http.proxyUser")
                System.clearProperty("http.proxyPassword")
                System.clearProperty("https.proxyUser")
                System.clearProperty("https.proxyPassword")
            } else if (config.scheme == "http") {
                System.setProperty("http.proxyHost", config.host)
                System.setProperty("http.proxyPort", config.port.toString())
                System.setProperty("https.proxyHost", config.host)
                System.setProperty("https.proxyPort", config.port.toString())
                // Avoid proxying loopback where Clash itself runs
                System.setProperty("http.nonProxyHosts", "localhost|127.0.0.1|::1")
                System.setProperty("https.nonProxyHosts", "localhost|127.0.0.1|::1")
                System.clearProperty("socksProxyHost")
                System.clearProperty("socksProxyPort")
            } else { // socks5
                System.setProperty("socksProxyHost", config.host)
                System.setProperty("socksProxyPort", config.port.toString())
                System.clearProperty("http.proxyHost")
                System.clearProperty("http.proxyPort")
                System.clearProperty("https.proxyHost")
                System.clearProperty("https.proxyPort")
                System.clearProperty("http.nonProxyHosts")
                System.clearProperty("https.nonProxyHosts")
                if (config.username != null) {
                    System.setProperty("java.net.socks.username", config.username)
                    System.setProperty("java.net.socks.password", config.password ?: "")
                } else {
                    System.clearProperty("java.net.socks.username")
                    System.clearProperty("java.net.socks.password")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set system proxy properties", e)
        }
    }

    private fun applyWebViewProxy(config: ProxyConfig?) {
        try {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                Log.i(TAG, "WebView PROXY_OVERRIDE not supported, relying on system properties")
                return
            }
            val controller = ProxyController.getInstance()
            val executor = Executor { it.run() }
            if (config == null) {
                controller.clearProxyOverride(executor) { Log.i(TAG, "WebView proxy cleared") }
                return
            }

            if (config.scheme != "http") {
                Log.i(TAG, "SOCKS proxy: WebView ProxyController only supports HTTP; clearing override and relying on system properties")
                controller.clearProxyOverride(executor) {}
                return
            }

            // Use host:port (without scheme) as in Google sample "localhost:port".
            val proxyUrl = "${config.host}:${config.port}"
            Log.i(TAG, "Setting WebView proxy to $proxyUrl")

            val builder = WebKitProxyConfig.Builder()
                .addProxyRule(proxyUrl)
                .addBypassRule("127.0.0.1")
                .addBypassRule("localhost")
                .addBypassRule("::1")

            if (config.username != null) {
                Log.w(TAG, "WebView proxy auth not natively supported by ProxyController; browsing may prompt for credentials")
            }

            controller.setProxyOverride(builder.build(), executor) {
                Log.i(TAG, "WebView proxy override set")
            }
        } catch (e: Exception) {
            Log.w(TAG, "WebView ProxyController not available: ${e.message}")
        } catch (e: NoClassDefFoundError) {
            Log.w(TAG, "WebView ProxyController class not found", e)
        }
    }

    private fun tryApplyGeckoProxy(config: ProxyConfig?) {
        try {
            Class.forName("org.mlm.browkorftv.webengine.gecko.GeckoWebEngine")
            // GeckoView has no official proxy API (bug 1525486), but its internal
            // org.mozilla.gecko.util.ProxySelector reads http.proxyHost/https.proxyHost/socksProxyHost
            // per-request, so System.setProperty changes apply without restart.
            if (config != null) {
                Log.i(TAG, "Proxy active (${config.sanitizedUrl}). GeckoView will use system properties via ProxySelector")
            } else {
                Log.i(TAG, "Proxy cleared")
            }
        } catch (_: ClassNotFoundException) {
            // nothing to do
        } catch (e: Exception) {
            Log.w(TAG, "Gecko proxy check failed", e)
        }
    }

    fun proxy(): Proxy = currentConfig?.proxy ?: Proxy.NO_PROXY

    fun openConnection(url: URL): HttpURLConnection {
        val cfg = currentConfig
        val conn = if (cfg == null) {
            url.openConnection() as HttpURLConnection
        } else {
            url.openConnection(cfg.proxy) as HttpURLConnection
        }
        if (cfg != null && cfg.scheme == "http" && !cfg.username.isNullOrBlank()) {
            val user = cfg.username ?: ""
            val cred = basicAuth(user, cfg.password ?: "")
            if (url.protocol.equals("http", ignoreCase = true)) {
                conn.setRequestProperty("Proxy-Authorization", cred)
            }
        }
        return conn
    }

    fun openConnection(urlString: String): HttpURLConnection = openConnection(URL(urlString))

    fun proxyAuthorizationHeader(): String? {
        val cfg = currentConfig ?: return null
        if (cfg.scheme != "http") return null
        if (cfg.username.isNullOrBlank()) return null
        val user = cfg.username ?: return null
        return basicAuth(user, cfg.password ?: "")
    }

    private fun basicAuth(username: String, password: String): String {
        val cred = "$username:$password"
        val encoded = Base64.encodeToString(cred.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "Basic $encoded"
    }
}
