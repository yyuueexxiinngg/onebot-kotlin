package com.github.yyuueexxiinngg.onebot.util

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import com.github.yyuueexxiinngg.onebot.PluginSettings
import com.github.yyuueexxiinngg.onebot.logger
import java.io.InputStream

class HttpClient {
    companion object {
        private val http = io.ktor.client.HttpClient(OkHttp) {
            install(HttpTimeout)
            engine {
                config {
                    retryOnConnectionFailure(true)
                }
            }
        }
        var httpProxied: io.ktor.client.HttpClient? = null
        private val ua =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36"

        suspend fun getBytes(url: String, timeout: Long = 0L, useProxy: Boolean = false): ByteArray? {
            return try {
                (if (useProxy && httpProxied != null) httpProxied else http)?.request {
                    url(url)
                    headers {
                        append("User-Agent", ua)
                    }
                    if (timeout > 0L) {
                        timeout {
                            socketTimeoutMillis = timeout
                        }
                    }
                    method = HttpMethod.Get
                }
            } catch (e: Exception) {
                when (e) {
                    is SocketTimeoutException -> logger.warning("Timeout when getting $url, timeout was set to $timeout milliseconds")
                    else -> logger.warning("Error when getting $url, ${e.message}")
                }
                null
            }
        }

        suspend fun getInputStream(url: String): InputStream {
            return http.request {
                url(url)
                headers {
                    append("User-Agent", ua)
                }
                method = HttpMethod.Get
            }
        }

        @OptIn(KtorExperimentalAPI::class)
        @Suppress("DuplicatedCode")
        fun initHTTPClientProxy() {
            if (PluginSettings.proxy != "") {
                val parts = PluginSettings.proxy.split("=", limit = 2)
                if (parts.size == 2) {
                    when (parts[0].trim()) {
                        "http" -> {
                            logger.debug("创建HTTP Proxied HTTP客户端中: ${parts[1].trim()}")
                            val httpProxy = ProxyBuilder.http(parts[1].trim())
                            if (httpProxied != null) {
                                httpProxied?.close()
                                httpProxied = null
                            }
                            httpProxied = io.ktor.client.HttpClient(OkHttp) {
                                install(HttpTimeout)
                                engine {
                                    proxy = httpProxy
                                    config {
                                        retryOnConnectionFailure(true)
                                    }
                                }
                            }
                        }
                        "sock" -> {
                            logger.debug("创建Sock Proxied HTTP客户端中: ${parts[1].trim()}")
                            val proxyParts = parts[1].trim().split(":")
                            if (proxyParts.size == 2) {
                                val sockProxy = ProxyBuilder.socks(proxyParts[0].trim(), proxyParts[1].trim().toInt())
                                if (httpProxied != null) {
                                    httpProxied?.close()
                                    httpProxied = null
                                }
                                httpProxied = io.ktor.client.HttpClient(OkHttp) {
                                    install(HttpTimeout)
                                    engine {
                                        proxy = sockProxy
                                        config {
                                            retryOnConnectionFailure(true)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}