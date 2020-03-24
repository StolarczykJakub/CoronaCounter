package com.jst.coronacounter.inject

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okhttp3.internal.platform.Platform
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import okio.EOFException
import okio.GzipSource
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit

/**
 * Improving visual representation of request and response logs.
 * JSON arrays and JSON objects can be formatted with chosen indent spaces.
 * Interceptor is also able to show additional headers like "Request, Headers, Body"
 * @param level - NONE, BASIC, HEADERS, BODY
 * @param jsonIndentSpaces - json body indent spaces
 * @param showExtraHeaders - enables additional log headers
 * @author Jakub Stolarczyk 22.03.2020
 */
class HttpLogsBeautifier(
    private val level: HttpLoggingInterceptor.Level,
    private val jsonIndentSpaces: Int = 2,
    private val showExtraHeaders: Boolean = false
) : Interceptor {

    @Volatile
    private var headersToRedact = emptySet<String>()
    private val platform = Platform.get()

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
        if (level == HttpLoggingInterceptor.Level.NONE) {
            return chain.proceed(request)
        }

        val logBody = level == HttpLoggingInterceptor.Level.BODY
        val logHeaders = logBody || level == HttpLoggingInterceptor.Level.HEADERS
        val requestBody = request.body
        val connection = chain.connection()

        showExtraHeader("╔════════  Request ${request.method}  ═════════════════════════════")
        var requestStartMessage =
            ("  🠊 ${request.method} ${request.url}${if (connection != null) " ${connection.protocol()}" else ""}")
        if (!logHeaders && requestBody != null) {
            requestStartMessage += " (${requestBody.contentLength()}-byte body)"
        }
        platform.log(requestStartMessage)

        if (logHeaders) {
            val headers = request.headers

            if (requestBody != null) {
                // Request body headers are only present when installed as a network interceptor. When not
                // already present, force them to be included (if available) so their values are known.
                requestBody.contentType()?.let {
                    if (headers["Content-Type"] == null) {
                        platform.log("Content-Type: $it")
                    }
                }
                if (requestBody.contentLength() != -1L) {
                    if (headers["Content-Length"] == null) {
                        platform.log("Content-Length: ${requestBody.contentLength()}")
                    }
                }
            }

            printAllHeaders(headers)
            if (!logBody || requestBody == null) {
                platform.log("  🠊 END ${request.method}")
            } else if (bodyHasUnknownEncoding(request.headers)) {
                platform.log("  🠊 END ${request.method} (encoded body omitted)")
            } else if (requestBody.isDuplex()) {
                platform.log("  🠊 END ${request.method} (duplex request body omitted)")
            } else if (requestBody.isOneShot()) {
                platform.log("  🠊 END ${request.method} (one-shot body omitted)")
            } else {
                val buffer = Buffer()
                requestBody.writeTo(buffer)

                val contentType = requestBody.contentType()
                val charset: Charset = contentType?.charset(UTF_8) ?: UTF_8

                platform.log("")
                if (buffer.isProbablyUtf8()) {
                    platform.log(buffer.readString(charset))
                    platform.log("  🠊 END ${request.method} (${requestBody.contentLength()}-byte body)")
                } else {
                    platform.log(
                        "  🠊 END ${request.method} (binary ${requestBody.contentLength()}-byte body omitted)"
                    )
                }
            }
        }

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            platform.log("  \u2BBD HTTP FAILED: $e")
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body!!
        val contentLength = responseBody.contentLength()
        val bodySize = if (contentLength != -1L) "$contentLength-byte" else "unknown-length"
        platform.log(" ")
        showExtraHeader("╔════════  RESPONSE  ════════════════════════════════")
        platform.log(
            "  \uD83E\uDC08 ${response.code}${if (response.message.isEmpty()) "" else ' ' + response.message} ${response.request.url} (${tookMs}ms${if (!logHeaders) ", $bodySize body" else ""})"
        )

        if (logHeaders) {
            val headers = response.headers
            printAllHeaders(headers)

            if (!logBody || !response.promisesBody()) {
                platform.log("  🠈 END HTTP")
            } else if (bodyHasUnknownEncoding(response.headers)) {
                platform.log("  🠈 END HTTP (encoded body omitted)")
            } else {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE) // Buffer the entire body.
                var buffer = source.buffer

                if (!buffer.isProbablyUtf8()) {
                    platform.log("")
                    platform.log("  🠈 END HTTP (binary ${buffer.size}-byte body omitted)")
                    return response
                }

                val contentType = responseBody.contentType()
                val charset: Charset = contentType?.charset(UTF_8) ?: UTF_8
                platform.log(" ")
                showExtraHeader("─────────  BODY  ───────────────────────────────────")
                if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
                    val gzippedLength: Long = buffer.size
                    GzipSource(buffer.clone()).use { gzippedResponseBody ->
                        buffer = Buffer()
                        buffer.writeAll(gzippedResponseBody)
                    }
                    platform.log(buffer.clone().readString(charset))
                    platform.log("  🠈 END HTTP (${buffer.size}-byte, $gzippedLength-gzipped-byte body)")
                } else {
                    platform.log(formattedBody(buffer.clone().readString(charset)))
                    platform.log("  🠈 END HTTP (${buffer.size}-byte body)")
                }
            }
        }

        return response
    }

    private fun printAllHeaders(headers: Headers) {
        if (headers.size > 0) {
            platform.log(" ")
            showExtraHeader("─────────  HEADERS  ─────────────────────────────────")
        }
        for (i in 0 until headers.size) {
            val value = if (headers.name(i) in headersToRedact) "██" else headers.value(i)
            platform.log("  " + headers.name(i) + ": " + value)
        }
    }

    private fun showExtraHeader(message: String) {
        if (showExtraHeaders) {
            platform.log(message)
        }
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals(
            "identity",
            ignoreCase = true
        ) && !contentEncoding.equals("gzip", ignoreCase = true)
    }

    private fun Buffer.isProbablyUtf8(): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = size.coerceAtMost(64)
            copyTo(prefix, 0, byteCount)
            for (i in 0 until 16) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            return true
        } catch (_: EOFException) {
            return false // Truncated UTF-8 sequence.
        }
    }


    private fun formattedBody(printableBody: String): String =
        try {
            when {
                printableBody.trimStart()[0] == '{' -> formatAsJsonObject(printableBody)
                printableBody.trimStart()[0] == '[' -> formatAsJsonArray(printableBody)
                else -> printableBody
            }
        } catch (e: JSONException) {
            printableBody
        } catch (e: StringIndexOutOfBoundsException) {
            ""
        }

    private fun formatAsJsonObject(msg: String): String = JSONObject(msg).toString(jsonIndentSpaces)
    private fun formatAsJsonArray(msg: String): String = JSONArray(msg).toString(jsonIndentSpaces)
}