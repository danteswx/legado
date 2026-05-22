package io.legado.app.help.ai

import io.legado.app.data.entities.BaseSource
import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.CookieManager.cookieJarHeader
import io.legado.app.help.http.addHeaders
import io.legado.app.help.http.newCallResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.postJson
import io.legado.app.ui.main.ai.AiChatException
import io.legado.app.ui.main.ai.AiModelConfig
import io.legado.app.ui.main.ai.AiProviderConfig
import io.legado.app.utils.EncoderUtils
import io.legado.app.utils.NetworkUtils
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object AiCaptchaService {

    private const val DEFAULT_PROMPT =
        "Read the captcha image. Return only the captcha text, with no explanation."

    data class Config(
        val provider: AiProviderConfig,
        val model: AiModelConfig
    )

    fun requireConfiguredProvider(
        provider: AiProviderConfig?,
        model: AiModelConfig?
    ): Config {
        if (provider?.baseUrl.isNullOrBlank() || model?.modelId.isNullOrBlank()) {
            throw AiChatException(
                message = "AI helper is not configured",
                debugLog = "AI captcha requires a provider base URL and model."
            )
        }
        return Config(provider, model)
    }

    fun buildRequestBody(
        model: String,
        imageDataUri: String,
        prompt: String?
    ): String {
        val userPrompt = prompt?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_PROMPT
        return JSONObject().apply {
            put("model", model)
            put("stream", false)
            put(
                "messages",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put(
                                "content",
                                "Return only the captcha text. Do not add spaces, punctuation, quotes, or explanation."
                            )
                        }
                    )
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put(
                                "content",
                                JSONArray().apply {
                                    put(
                                        JSONObject().apply {
                                            put("type", "text")
                                            put("text", userPrompt)
                                        }
                                    )
                                    put(
                                        JSONObject().apply {
                                            put("type", "image_url")
                                            put(
                                                "image_url",
                                                JSONObject().apply {
                                                    put("url", imageDataUri)
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }.toString()
    }

    fun extractCaptchaText(responseBody: String, prompt: String? = null): String {
        val root = JSONObject(responseBody)
        val message = root.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?: JSONObject()
        val raw = extractContentText(message.opt("content"))
            .ifBlank { root.optString("response") }
        return normalizeCaptchaText(raw, prompt)
    }

    suspend fun recognize(
        source: BaseSource?,
        imageUrlOrDataUri: String,
        prompt: String?
    ): String {
        val config = requireConfiguredProvider(
            AppConfig.aiCurrentProvider,
            AppConfig.aiCurrentModelConfig
        )
        val imageDataUri = resolveImageDataUri(source, imageUrlOrDataUri)
        val requestBody = buildRequestBody(
            model = config.model.modelId,
            imageDataUri = imageDataUri,
            prompt = prompt
        )
        val chatUrl = resolveChatUrl(config.provider.baseUrl)
        val response = okHttpClient.newCallResponse {
            url(chatUrl)
            addHeader("Accept", "application/json")
            addHeader("Content-Type", "application/json")
            config.provider.apiKey.trim().takeIf { it.isNotBlank() }?.let {
                addHeader("Authorization", "Bearer $it")
            }
            addHeaders(parseCustomHeaders(config.provider.headers.orEmpty()))
            postJson(requestBody)
        }
        response.use { rawResponse ->
            val payload = rawResponse.body.string()
            if (!rawResponse.isSuccessful) {
                throw AiChatException(
                    message = extractError(payload).ifBlank {
                        "${rawResponse.code} ${rawResponse.message}"
                    },
                    debugLog = "url=$chatUrl\nstatus=${rawResponse.code} ${rawResponse.message}\nresponse=$payload"
                )
            }
            return extractCaptchaText(payload, prompt).ifBlank {
                throw AiChatException(
                    message = "AI captcha response is empty",
                    debugLog = "url=$chatUrl\nresponse=$payload"
                )
            }
        }
    }

    private suspend fun resolveImageDataUri(
        source: BaseSource?,
        imageUrlOrDataUri: String
    ): String {
        val image = imageUrlOrDataUri.trim()
        if (image.startsWith("data:image/", ignoreCase = true)) {
            return image
        }
        val absoluteUrl = NetworkUtils.getAbsoluteURL(source?.getKey(), image)
        val response = okHttpClient.newCallResponse {
            url(absoluteUrl)
            source?.getHeaderMap(true)?.let(::addHeaders)
            if (source?.enabledCookieJar == true) {
                addHeader(cookieJarHeader, "1")
            }
        }
        response.use { rawResponse ->
            if (!rawResponse.isSuccessful) {
                throw AiChatException(
                    message = "Captcha image download failed",
                    debugLog = "url=$absoluteUrl\nstatus=${rawResponse.code} ${rawResponse.message}"
                )
            }
            val body = rawResponse.body
            val bytes = body.bytes()
            val mimeType = imageMimeType(rawResponse, absoluteUrl)
            return "data:$mimeType;base64,${EncoderUtils.base64Encode(bytes)}"
        }
    }

    private fun imageMimeType(response: Response, url: String): String {
        response.body.contentType()?.toString()
            ?.substringBefore(";")
            ?.trim()
            ?.takeIf { it.startsWith("image/", ignoreCase = true) }
            ?.let { return it }
        return when (url.substringBefore("?").substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            else -> "image/png"
        }
    }

    private fun normalizeCaptchaText(raw: String, prompt: String?): String {
        if (isMathCaptchaPrompt(prompt)) {
            normalizeMathCaptchaText(raw).takeIf { it.isNotBlank() }?.let {
                return it
            }
        }
        return raw.asSequence()
            .filter { it in '0'..'9' || it in 'A'..'Z' || it in 'a'..'z' }
            .joinToString("")
            .take(12)
    }

    private fun isMathCaptchaPrompt(prompt: String?): Boolean {
        val text = prompt?.lowercase(Locale.ROOT).orEmpty()
        return listOf("公式", "计算", "结果", "整数", "加减乘", "answer", "result", "math")
            .any(text::contains)
    }

    private fun normalizeMathCaptchaText(raw: String): String {
        val text = raw
            .replace('×', '*')
            .replace('x', '*')
            .replace('X', '*')
            .replace('＋', '+')
            .replace('－', '-')
            .replace('—', '-')
            .replace('–', '-')
            .trim()

        Regex("""^[^\d-]*(-?\d{1,4})[^\d]*$""").matchEntire(text)?.let {
            return it.groupValues[1]
        }

        val rightSide = text.substringAfterLast('=', "")
        if (rightSide.isNotBlank()) {
            Regex("""-?\d{1,4}""").find(rightSide)?.let {
                return it.value
            }
        }

        if (Regex("""答案|结果|answer|result""", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            Regex("""-?\d{1,4}""").findAll(text).lastOrNull()?.let {
                return it.value
            }
        }

        return evaluateSingleDigitExpression(text)?.toString().orEmpty()
    }

    private fun evaluateSingleDigitExpression(raw: String): Int? {
        val expression = Regex("""[0-9+\-* ]+""").find(raw)?.value
            ?.replace(" ", "")
            ?.takeIf { it.any(Char::isDigit) && it.any { char -> char == '+' || char == '-' || char == '*' } }
            ?: return null
        val values = mutableListOf<Int>()
        val operators = mutableListOf<Char>()
        var expectDigit = true
        expression.forEach { char ->
            when {
                expectDigit && char in '0'..'9' -> {
                    values.add(char.digitToInt())
                    expectDigit = false
                }
                !expectDigit && (char == '+' || char == '-' || char == '*') -> {
                    operators.add(char)
                    expectDigit = true
                }
                else -> return null
            }
        }
        if (expectDigit || values.size != operators.size + 1) {
            return null
        }

        var index = 0
        while (index < operators.size) {
            if (operators[index] == '*') {
                values[index] = values[index] * values.removeAt(index + 1)
                operators.removeAt(index)
            } else {
                index++
            }
        }

        var result = values.first()
        operators.forEachIndexed { opIndex, operator ->
            val value = values[opIndex + 1]
            result = if (operator == '+') result + value else result - value
        }
        return result
    }

    private fun extractContentText(content: Any?): String {
        return when (content) {
            is String -> content
            is JSONArray -> contentArrayToText(content)
            is JSONObject -> content.optString("text")
            else -> ""
        }
    }

    private fun contentArrayToText(content: JSONArray): String {
        return buildString {
            for (index in 0 until content.length()) {
                when (val part = content.opt(index)) {
                    is String -> append(part)
                    is JSONObject -> append(part.optString("text"))
                }
            }
        }
    }

    private fun parseCustomHeaders(rawHeaders: String): Map<String, String> {
        val text = rawHeaders.trim()
        if (text.isBlank()) return emptyMap()
        runCatching {
            val json = JSONObject(text)
            return buildMap {
                json.keys().forEach { key ->
                    val value = json.optString(key)
                    if (key.isNotBlank() && value.isNotBlank()) put(key, value)
                }
            }
        }
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val separator = line.indexOf(':').takeIf { it > 0 }
                    ?: line.indexOf('=').takeIf { it > 0 }
                separator?.let {
                    line.substring(0, it).trim() to line.substring(it + 1).trim()
                }
            }
            .filter { it.first.isNotBlank() && it.second.isNotBlank() }
            .toMap()
    }

    private fun resolveChatUrl(baseUrl: String): String {
        val normalized = baseUrl.trim().trimEnd('/')
        return when {
            normalized.endsWith("/chat/completions") -> normalized
            normalized.endsWith("/v1") -> "$normalized/chat/completions"
            else -> "$normalized/v1/chat/completions"
        }
    }

    private fun extractError(body: String): String {
        if (body.isBlank()) return ""
        return runCatching {
            val root = JSONObject(body)
            root.optJSONObject("error")?.optString("message")
                ?: root.optString("message")
        }.getOrNull().orEmpty()
    }
}
