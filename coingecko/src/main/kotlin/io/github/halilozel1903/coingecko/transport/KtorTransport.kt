package io.github.halilozel1903.coingecko.transport

import io.github.halilozel1903.coingecko.CoinGeckoException
import io.github.halilozel1903.coingecko.RetryPolicy
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/**
 * The default transport: GET requests with Ktor, the API key header, and retries by [retryPolicy] that honor
 * `Retry-After`. Closes the client only when it created it.
 */
internal class KtorTransport(
    httpClient: HttpClient?,
    engine: HttpClientEngine?,
    private val userAgent: String,
    private val extraHeaders: Map<String, String>,
    requestTimeoutMillis: Long,
    private val retryPolicy: RetryPolicy,
) : CoinGeckoTransport, AutoCloseable {

    private val ownsClient = httpClient == null
    private val client: HttpClient = httpClient ?: run {
        val setup: HttpClientConfig<*>.() -> Unit = {
            expectSuccess = false
            install(HttpTimeout) {
                this.requestTimeoutMillis = requestTimeoutMillis
            }
        }
        if (engine != null) HttpClient(engine, setup) else HttpClient(CIO, setup)
    }

    override suspend fun get(url: String): String {
        var attempt = 0
        while (true) {
            val response: HttpResponse = try {
                client.get(url) {
                    header(HttpHeaders.UserAgent, userAgent)
                    accept(ContentType.Application.Json)
                    extraHeaders.forEach { (name, value) -> header(name, value) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempt < retryPolicy.maxRetries) {
                    delay(retryPolicy.backoffMillis(attempt))
                    attempt++
                    continue
                }
                throw CoinGeckoException.Network(url, e)
            }

            val status = response.status.value
            if (response.status.isSuccess()) {
                return try {
                    response.bodyAsText()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    throw CoinGeckoException.Network(url, e)
                }
            }

            val body = runCatching { response.bodyAsText() }.getOrNull()
            val retryAfter = response.headers[HttpHeaders.RetryAfter]
            val retryable = status == 429 || status >= 500
            if (retryable && attempt < retryPolicy.maxRetries) {
                val wait = retryPolicy.delayMillis(attempt, retryAfter)
                if (wait != null) {
                    delay(wait)
                    attempt++
                    continue
                }
            }
            throw when (status) {
                404 -> CoinGeckoException.NotFound(url, body)
                429 -> CoinGeckoException.RateLimited(url, RetryPolicy.parseRetryAfter(retryAfter), body)
                else -> CoinGeckoException.Http(url, status, body)
            }
        }
    }

    override fun close() {
        if (ownsClient) client.close()
    }
}
