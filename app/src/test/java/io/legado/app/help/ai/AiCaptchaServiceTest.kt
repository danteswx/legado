package io.legado.app.help.ai

import io.legado.app.ui.main.ai.AiModelConfig
import io.legado.app.ui.main.ai.AiProviderConfig
import io.legado.app.ui.main.ai.AiChatException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AiCaptchaServiceTest {

    @Test
    fun buildRequestBodyUsesCurrentModelAndImageContent() {
        val body = AiCaptchaService.buildRequestBody(
            model = "vision-model",
            imageDataUri = "data:image/png;base64,abc123",
            prompt = "识别验证码"
        )

        val root = JSONObject(body)
        assertEquals("vision-model", root.getString("model"))
        assertFalse(root.getBoolean("stream"))

        val messages = root.getJSONArray("messages")
        assertEquals("system", messages.getJSONObject(0).getString("role"))
        assertTrue(messages.getJSONObject(0).getString("content").contains("Return only"))

        val user = messages.getJSONObject(1)
        assertEquals("user", user.getString("role"))
        val content = user.getJSONArray("content")
        assertEquals("text", content.getJSONObject(0).getString("type"))
        assertTrue(content.getJSONObject(0).getString("text").contains("识别验证码"))
        assertEquals("image_url", content.getJSONObject(1).getString("type"))
        assertEquals(
            "data:image/png;base64,abc123",
            content.getJSONObject(1).getJSONObject("image_url").getString("url")
        )
    }

    @Test
    fun extractCaptchaTextNormalizesStringResponse() {
        val response = """
            {
              "choices": [
                {
                  "message": {
                    "content": "验证码是 A 9-kZ。"
                  }
                }
              ]
            }
        """.trimIndent()

        assertEquals("A9kZ", AiCaptchaService.extractCaptchaText(response))
    }

    @Test
    fun extractCaptchaTextReadsContentArrayResponse() {
        val response = """
            {
              "choices": [
                {
                  "message": {
                    "content": [
                      {"type": "text", "text": " 7 b C 2 "}
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        assertEquals("7bC2", AiCaptchaService.extractCaptchaText(response))
    }

    @Test
    fun extractCaptchaTextReturnsSignedIntegerForMathPrompt() {
        val response = """
            {
              "choices": [
                {
                  "message": {
                    "content": "结果是 -2"
                  }
                }
              ]
            }
        """.trimIndent()

        assertEquals(
            "-2",
            AiCaptchaService.extractCaptchaText(
                response,
                "计算验证码里的加减乘公式，只返回整数结果"
            )
        )
    }

    @Test
    fun extractCaptchaTextEvaluatesMathExpressionForMathPrompt() {
        val response = """
            {
              "choices": [
                {
                  "message": {
                    "content": "3+4*2"
                  }
                }
              ]
            }
        """.trimIndent()

        assertEquals(
            "11",
            AiCaptchaService.extractCaptchaText(
                response,
                "计算验证码里的加减乘公式，只返回整数结果"
            )
        )
    }

    @Test
    fun requireConfiguredProviderRejectsMissingAiConfig() {
        try {
            AiCaptchaService.requireConfiguredProvider(null, null)
            fail("Missing provider and model should fail")
        } catch (e: AiChatException) {
            assertEquals("AI helper is not configured", e.message)
        }

        val provider = AiProviderConfig(
            name = "Local",
            baseUrl = "https://example.invalid/v1",
            apiKey = "secret"
        )
        try {
            AiCaptchaService.requireConfiguredProvider(provider, null)
            fail("Missing model should fail")
        } catch (e: AiChatException) {
            assertEquals("AI helper is not configured", e.message)
        }

        val model = AiModelConfig(providerId = provider.id, modelId = "vision-model")
        val config = AiCaptchaService.requireConfiguredProvider(provider, model)
        assertEquals(provider, config.provider)
        assertEquals(model, config.model)
    }
}
