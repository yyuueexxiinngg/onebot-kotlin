package tech.mihoyo.mirai.util

import io.ktor.client.request.*
import io.ktor.http.*

class HttpClient {
    companion object {
        private val http = io.ktor.client.HttpClient()

        suspend fun getBytes(url: String): ByteArray {
            return http.request {
                url(url)
                headers {
                    append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36")
                }
                method = HttpMethod.Get
            }
        }
    }
}