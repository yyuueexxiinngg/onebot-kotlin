package tech.mihoyo.mirai.util

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import java.io.InputStream

class HttpClient {
    companion object {
        private val http = io.ktor.client.HttpClient {
            install(HttpTimeout)
        }
        private val ua =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36"

        suspend fun getBytes(url: String, timeout: Long = 0L): ByteArray? {
            return try {
                http.request {
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
    }
}