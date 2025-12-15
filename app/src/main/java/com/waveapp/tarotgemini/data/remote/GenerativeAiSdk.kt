package com.waveapp.tarotgemini.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GenerativeAiSdk {
    private val apiKey: String by lazy {
        try {
            val key = com.waveapp.tarotgemini.BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank()) key else (System.getenv("GEMINI_API_KEY") ?: "")
        } catch (_: Throwable) {
            System.getenv("GEMINI_API_KEY") ?: ""
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val contentType = "application/json; charset=utf-8".toMediaType()

    // Modelo fijo (detectado en tu cuenta)
    private const val MODEL_ENDPOINT = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-pro:generateContent"

    suspend fun generateInterpretation(prompt: String, maxOutputTokens: Int = 512, temperature: Double = 0.7): Result<String> {
        if (apiKey.isBlank()) return Result.failure(IllegalStateException("GEMINI_API_KEY no configurada"))

        return withContext(Dispatchers.IO) {
            val triedBodies = mutableListOf<String>()

            // Construir varias variantes de payload para intentar (algunas APIs aceptan estructuras distintas)
            val variants = listOf(
                // Variant A: prompt.messages style (recommended for newer Gemini signatures)
                JSONObject().apply {
                    put("prompt", JSONObject().apply {
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("author", "user")
                                put("content", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("type", "text")
                                        put("text", prompt)
                                    })
                                })
                            })
                        })
                    })
                    put("temperature", temperature)
                    put("max_output_tokens", maxOutputTokens)
                }.toString(),

                // Variant B: prompt.text
                JSONObject().apply {
                    put("prompt", JSONObject().apply {
                        put("text", prompt)
                    })
                    put("temperature", temperature)
                    put("max_output_tokens", maxOutputTokens)
                }.toString(),

                // Variant C: content + parameters
                JSONObject().apply {
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
                    })
                    put("parameters", JSONObject().apply {
                        put("temperature", temperature)
                        put("maxOutputTokens", maxOutputTokens)
                    })
                }.toString()
            )

            var lastError: Throwable? = null

            for (body in variants) {
                triedBodies.add(body)

                val request = Request.Builder()
                    .url("$MODEL_ENDPOINT?key=$apiKey")
                    .post(body.toRequestBody(contentType))
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        val respBody = response.body?.string() ?: ""

                        if (!response.isSuccessful) {
                            // Log completo de la respuesta no exitosa
                            Log.e("ERROR_INTERPRETACION", "HTTP ${response.code}: $respBody\nRequest body: $body")
                            lastError = IOException("HTTP ${response.code}: $respBody")
                            // probar siguiente variante
                        } else {
                            if (respBody.isBlank()) {
                                Log.e("ERROR_INTERPRETACION", "Empty response body. Request body: $body")
                                lastError = IOException("Empty response body")
                            } else {
                                // intentar parsear texto de distintas ubicaciones
                                val parsed = parseResponseText(respBody)
                                if (parsed.isNotBlank()) return@withContext Result.success(parsed.trim())

                                // No se extrajo texto: loguear body completo para depuración
                                Log.e("ERROR_INTERPRETACION", "No text extracted from response. Response body: $respBody\nRequest body: $body")
                                lastError = IOException("No text extracted from response: $respBody")
                            }
                        }
                    }
                } catch (t: Throwable) {
                    // Logear excepción y el body que se intentó
                    Log.e("ERROR_INTERPRETACION", "Exception calling Generative API. Request body: $body\nError: ${t.message}", t)
                    lastError = t
                }
            }

            // Si llegamos aquí, todas las variantes fallaron
            val errorMsg = lastError?.message ?: "Unknown error calling Generative API"

            // Log final con todos los bodies intentados y última excepción
            try {
                Log.e("ERROR_INTERPRETACION", "All payload variants failed. Tried bodies:\n${triedBodies.joinToString(separator = "\n\n")}\nLast error: $errorMsg", lastError)
            } catch (_: Throwable) {
                // proteger logging de posibles errores de toString
            }

            Result.failure(IOException("All payload variants failed. Last error: $errorMsg"))
        }
    }

    private fun parseResponseText(respBody: String): String {
        try {
            val json = JSONObject(respBody)
            // margen de nombres comunes
            if (json.has("candidates")) {
                val candidates = json.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val first = candidates.getJSONObject(0)
                    if (first.has("content")) return first.optString("content", "")
                    if (first.has("text")) return first.optString("text", "")
                }
            }

            if (json.has("output")) {
                val output = json.getJSONObject("output")
                if (output.has("content")) return output.optString("content", "")
                if (output.has("text")) return output.optString("text", "")
            }

            if (json.has("result")) return json.optString("result", "")

            if (json.has("choices")) {
                val choices = json.getJSONArray("choices")
                if (choices.length() > 0) return choices.getJSONObject(0).optString("text", "")
            }

            // a veces la respuesta tiene nested 'candidates' dentro de 'outputs'
            if (json.has("outputs")) {
                val outputs = json.getJSONArray("outputs")
                for (i in 0 until outputs.length()) {
                    val out = outputs.getJSONObject(i)
                    if (out.has("content")) return out.optString("content", "")
                }
            }

            return ""
        } catch (t: Throwable) {
            Log.e("ERROR_INTERPRETACION", "Error parsing response body: ${t.message}", t)
            return ""
        }
    }
}
