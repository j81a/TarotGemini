package com.waveapp.tarotgemini.data.remote

import android.util.Log
import com.waveapp.tarotgemini.data.model.DrawnCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Cliente que llama a la API de Generative Language (Gemini) de Google.
 * Intenta usar la SDK oficial si está disponible en runtime. Si no, usa OkHttp con el endpoint REST.
 */
class GeminiClient {

    private val apiKey: String by lazy {
        try {
            val key = com.waveapp.tarotgemini.BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank()) key else (System.getenv("GEMINI_API_KEY") ?: "")
        } catch (_: Throwable) {
            System.getenv("GEMINI_API_KEY") ?: ""
        }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val contentType = "application/json; charset=utf-8".toMediaType()

    // Modelo a usar: usar el que exista en la cuenta (según tu curl, gemini-2.5-flash está disponible)
    private val modelName = "models/gemini-2.5-flash"

    // Reintentos
    private val maxRetries = 2

    suspend fun interpretSpread(question: String, drawnCards: List<DrawnCard>): Result<String> {
        if (apiKey.isBlank()) {
            // Fallback local para desarrollo: generar interpretación simulada
            return Result.success(generateFakeResponse(buildPrompt(question, drawnCards)) + "\n\n[Nota: GEMINI_API_KEY no configurada]")
        }

        val prompt = buildPrompt(question, drawnCards)

        return callWithRetries(prompt)
    }

    suspend fun getCardMeaning(drawnCard: DrawnCard): Result<String> {
        if (apiKey.isBlank()) {
            return Result.success(generateFakeResponse("Carta: ${drawnCard.card.name}\nSignificado: ${drawnCard.card.uprightMeaning}"))
        }

        val prompt = buildString {
            appendLine("Explica el significado de la carta del tarot:")
            appendLine("Carta: ${drawnCard.card.name}")
            appendLine("Orientación: ${if (drawnCard.isReversed) "Invertida" else "Normal"}")
            appendLine()
            val meaning = if (drawnCard.isReversed) drawnCard.card.reversedMeaning else drawnCard.card.uprightMeaning
            appendLine("Significado base: $meaning")
            appendLine()
            appendLine("Proporciona una explicación más detallada y profunda de este significado.")
            appendLine("Incluye consejos prácticos sobre cómo aplicar este mensaje.")
            appendLine("Usa un tono místico pero claro, en 2-3 párrafos.")
        }

        return callWithRetries(prompt)
    }

    private suspend fun callWithRetries(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        var attempt = 0
        var lastError: Throwable? = null

        while (attempt <= maxRetries) {
            try {
                // Intentar usar la SDK oficial en runtime si está disponible
                val sdkResult = tryCallSdk(prompt)
                return@withContext sdkResult
            } catch (t: Throwable) {
                lastError = t
                Log.e("ERROR_INTERPRETACION", "Error en intento de llamada a Generative API (attempt=$attempt): ${t.message}", t)
                attempt++
                if (attempt <= maxRetries) {
                    val backoffMs = 500L * attempt
                    delay(backoffMs)
                } else {
                    val fallback = generateFakeResponse(prompt) + "\n\n[Nota: fallback local. Error: ${t.message} ]"
                    Log.e("ERROR_INTERPRETACION", "Retornando fallback local por error final: ${t.message}. Fallback:\n$fallback", t)
                    return@withContext Result.success(fallback)
                }
            }
        }

        val finalFallback = generateFakeResponse(prompt) + "\n\n[Nota: fallback local. Error: ${lastError?.message} ]"
        Log.e("ERROR_INTERPRETACION", "Retornando finalFallback: ${lastError?.message}\nFallback:\n$finalFallback", lastError)
        return@withContext Result.success(finalFallback)
    }

    private fun buildPrompt(question: String, drawnCards: List<DrawnCard>): String {
        // Prompt minimalista en una sola línea para ahorrar tokens
        val compactCards = drawnCards.mapIndexed { index, dc ->
            val orient = if (dc.isReversed) "(Inv)" else "(Up)"
            "${index + 1})${dc.card.name}$orient"
        }.joinToString(";")
        return "PREGUNTA: $question | CARTAS: $compactCards | RESPONDE: una sola línea, sin razonamiento ni metadatos, máximo 600 caracteres."
    }


    // Intentar llamar a la SDK oficial por reflexión para evitar dependencias estáticas en tiempo de compilación
    private fun tryCallSdk(prompt: String): Result<String> {
        // Construir payload básico para la REST API por si necesitamos usarlo en el fallback
        val restBody = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 512)
            })
        }.toString()

        // Log request
        Log.d("ERROR_INTERPRETACION", "Request to Generative API (rest fallback) body: $restBody")

        // Intentar invocar la SDK: si la clase no existe, capturamos y usamos OkHttp
        return try {
            // Intentamos cargar una clase representativa de la SDK
            val sdkClass = Class.forName("com.google.ai.generativelanguage.v1.ClientWrapper")
            // Si existe, intentamos llamar a un método estático hipotético. Este bloque está preparado para adaptarse
            // a la API real: si la clase/firmas cambian, el fallback OkHttp será usado.
            try {
                // Aquí intentamos una invocación simple por reflexión (esto puede requerir ajuste si la SDK difiere)
                val method = sdkClass.getMethod("generateText", String::class.java, String::class.java)
                val result = method.invoke(null, modelName, prompt) as? String
                if (result != null) {
                    Log.d("ERROR_INTERPRETACION", "SDK result (via reflection) length=${result.length}")
                    return Result.success(result)
                }
            } catch (e: NoSuchMethodException) {
                // La firma no coincide: fallback a OkHttp
                Log.w("ERROR_INTERPRETACION", "SDK found pero firma no coincide: ${e.message}")
            } catch (e: Throwable) {
                Log.w("ERROR_INTERPRETACION", "Error invocando SDK por reflexión: ${e.message}", e)
            }

            // Si no retornó, usar fallback HTTP
            callRestGenerate(prompt)
        } catch (e: ClassNotFoundException) {
            // SDK no disponible en runtime -> fallback HTTP manual
            Log.w("ERROR_INTERPRETACION", "SDK no encontrada en runtime, uso OkHttp fallback: ${e.message}")
            callRestGenerate(prompt)
        }
    }

    private fun callRestGenerate(prompt: String): Result<String> {
        val url = "https://generativelanguage.googleapis.com/v1/${modelName}:generateContent?key=$apiKey"

        // Intentos de tokens: primer intento conservador, reintento si truncado
        // Usamos más espacio en tokens en la primera pasada para asegurar salida larga si es necesario
        // Aumentamos tokens permitidos para la salida para evitar truncamiento
        val tokenAttempts = listOf(2048, 4096)

        for (attempt in tokenAttempts.indices) {
            val maxTokens = tokenAttempts[attempt]

            val requestJson = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    // temperatura baja para respuestas más concretas
                    put("temperature", 0.0)
                    put("maxOutputTokens", maxTokens)
                })
            }.toString()

            Log.d("ERROR_INTERPRETACION", "HTTP Request JSON (attempt=${attempt + 1}, maxOutputTokens=$maxTokens): $requestJson")

            val req = Request.Builder()
                .url(url)
                .post(requestJson.toRequestBody(contentType))
                .build()

            try {
                // Manejo especial para 503/429 (modelo ocupado / rate limit)
                var resp = httpClient.newCall(req).execute()
                var attempt503 = 0
                val max503Retries = 4
                var bodyText = ""
                var code = resp.code
                while ((code == 503 || code == 429) && attempt503 < max503Retries) {
                    bodyText = resp.body?.string() ?: ""
                    Log.w("ERROR_INTERPRETACION", "HTTP $code received (attempt503=${attempt503 + 1}). Retrying after backoff. Body: $bodyText")
                    try { resp.close() } catch (_: Throwable) {}
                    // backoff exponencial con jitter (ms)
                    val base = 1000L * (1L shl attempt503)
                    val jitter = Random.nextLong(0, 500)
                    val sleepMs = base + jitter
                    Thread.sleep(sleepMs)
                    attempt503++
                    // reintentar
                    resp = httpClient.newCall(req).execute()
                    code = resp.code
                }

                bodyText = resp.body?.string() ?: ""
                code = resp.code
                if ((code == 503 || code == 429) && attempt503 >= max503Retries) {
                    Log.e("ERROR_INTERPRETACION", "HTTP $code after $attempt503 retries: $bodyText")
                    try { resp.close() } catch (_: Throwable) {}
                    // En vez de fallar de forma definitiva, pasamos al siguiente intento de tokens
                    Log.w("ERROR_INTERPRETACION", "Model overloaded after retries, intentaremos con el siguiente tamaño de token (si existe) o fallback local.")
                    try { resp.close() } catch (_: Throwable) {}
                    // continuar al siguiente token attempt
                    continue
                }

                // No es 503/429 -> proceder
                if (!resp.isSuccessful) {
                    Log.e("ERROR_INTERPRETACION", "HTTP ${resp.code}: $bodyText")
                    try { resp.close() } catch (_: Throwable) {}
                    return Result.failure(IOException("HTTP ${resp.code}: $bodyText"))
                }

                Log.d("ERROR_INTERPRETACION", "HTTP ${resp.code} successful. Response body: $bodyText")

                // Parsear y extraer texto — en un bloque limpio
                try {
                    val j = JSONObject(bodyText)
                    var wasTruncated = false

                    // registrar usageMetadata si existe
                    if (j.has("usageMetadata")) {
                        try {
                            val usage = j.getJSONObject("usageMetadata")
                            val promptTokens = usage.optInt("promptTokenCount", -1)
                            val totalTokens = usage.optInt("totalTokenCount", -1)
                            val thoughts = usage.optInt("thoughtsTokenCount", -1)
                            Log.d("ERROR_INTERPRETACION", "usageMetadata: promptTokens=$promptTokens, thoughts=$thoughts, totalTokens=$totalTokens")
                        } catch (_: Throwable) {}
                    }

                    // candidates
                    val candidateText = if (j.has("candidates")) {
                        val candidates = j.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val finishReason = candidate.optString("finishReason", candidate.optString("finish_reason", ""))
                            if (finishReason.equals("MAX_TOKENS", ignoreCase = true)) wasTruncated = true
                            extractTextFromContent(candidate.opt("content"))
                        } else ""
                    } else ""

                    if (candidateText.isNotBlank()) {
                        var finalText = candidateText
                        if (finalText.length > 300) finalText = finalText.substring(0, 300)
                        if (wasTruncated && attempt < tokenAttempts.size - 1 && finalText.length < 300) {
                            Log.w("ERROR_INTERPRETACION", "Response truncated (finishReason=MAX_TOKENS). Reintentando con más tokens...")
                            Thread.sleep(200L)
                            try { resp.close() } catch (_: Throwable) {}
                            continue
                        }
                        try { resp.close() } catch (_: Throwable) {}
                        return Result.success(finalText)
                    }

                    // outputs
                    val outputsText = if (j.has("outputs")) {
                        val outputs = j.getJSONArray("outputs")
                        if (outputs.length() > 0) {
                            val outObj = outputs.getJSONObject(0)
                            val finishReason = outObj.optString("finishReason", outObj.optString("finish_reason", ""))
                            if (finishReason.equals("MAX_TOKENS", ignoreCase = true)) wasTruncated = true
                            extractTextFromContent(outObj.opt("content"))
                        } else ""
                    } else ""

                    if (outputsText.isNotBlank()) {
                        var finalText = outputsText
                        if (finalText.length > 300) finalText = finalText.substring(0, 300)
                        if (wasTruncated && attempt < tokenAttempts.size - 1 && finalText.length < 300) {
                            Log.w("ERROR_INTERPRETACION", "Outputs truncated (finishReason=MAX_TOKENS). Reintentando with more tokens...")
                            Thread.sleep(200L)
                            try { resp.close() } catch (_: Throwable) {}
                            continue
                        }
                        try { resp.close() } catch (_: Throwable) {}
                        return Result.success(finalText)
                    }

                    // top-level
                    if (j.has("output")) {
                        try { resp.close() } catch (_: Throwable) {}
                        return Result.success(j.optString("output"))
                    }

                    if (j.has("content")) {
                        val top = extractTextFromContent(j.opt("content"))
                        if (top.isNotBlank()) {
                            try { resp.close() } catch (_: Throwable) {}
                            return Result.success(top)
                        }
                    }

                    // Si llegamos aquí y fue truncado, reintentar con más tokens
                    if (wasTruncated && attempt < tokenAttempts.size - 1) {
                        Log.w("ERROR_INTERPRETACION", "Detected truncation but no extractable text; reintentando con más tokens...")
                        Thread.sleep(200L)
                        try { resp.close() } catch (_: Throwable) {}
                        continue
                    }

                    try { resp.close() } catch (_: Throwable) {}
                    return Result.success(bodyText)
                } catch (eParse: Throwable) {
                    Log.e("ERROR_INTERPRETACION", "Error parsing response JSON: ${eParse.message}")
                    try { resp.close() } catch (_: Throwable) {}
                    return Result.success(bodyText)
                }
            } catch (e: Throwable) {
                Log.e("ERROR_INTERPRETACION", "Exception during HTTP call: ${e.message}", e)
                return Result.failure(e)
            }
        }

        return Result.failure(IOException("Failed to generate content after retries"))
    }

    // Extrae texto de un objeto 'content' flexible que puede ser JSONObject, JSONArray o String
    private fun extractTextFromContent(contentObj: Any?): String {
        try {
            if (contentObj == null) return ""
            // JSONArray
            if (contentObj is org.json.JSONArray) {
                val sb = StringBuilder()
                for (i in 0 until contentObj.length()) {
                    val item = contentObj.get(i)
                    val partText = extractTextFromContent(item)
                    if (partText.isNotBlank()) {
                        if (sb.isNotEmpty()) sb.append("\n")
                        sb.append(partText)
                    }
                }
                return sb.toString()
            }

            // JSONObject
            if (contentObj is org.json.JSONObject) {
                // caso común: content -> { "parts": [ { "text": "..." } ], "role": "model" }
                if (contentObj.has("parts")) {
                    val parts = contentObj.getJSONArray("parts")
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val p = parts.getJSONObject(i)
                        val t = p.optString("text", p.optString("output", "")).trim()
                        if (t.isNotBlank()) {
                            if (sb.isNotEmpty()) sb.append("\n")
                            sb.append(t)
                        }
                    }
                    if (sb.isNotEmpty()) return sb.toString()
                }

                // content may directly have 'text' or 'output'
                val direct = contentObj.optString("text", contentObj.optString("output", "")).trim()
                if (direct.isNotBlank()) return direct

                // fallback: return whole JSON string
                return contentObj.toString()
            }

            // String or other
            return contentObj.toString()
        } catch (t: Throwable) {
            Log.w("ERROR_INTERPRETACION", "extractTextFromContent failed: ${t.message}")
            return ""
        }
    }

    // Generador local para fallback cuando la API no está disponible o falla
    private fun generateFakeResponse(prompt: String): String {
        return buildString {
            appendLine("Interpretación simulada (fallback local):")
            appendLine("Consulta: $prompt")
            appendLine("Respuesta: Las cartas indican un camino de autodescubrimiento y crecimiento personal.")
            appendLine("Consejo: Confía en tu intuición y busca el equilibrio emocional.")
        }
    }

    // Intenta expandir un texto hasta al menos `minChars` llamando al endpoint una vez.
    private fun expandTextToLength(shortText: String, minChars: Int): String {
        val expandPrompt = "Extiende el siguiente texto hasta al menos $minChars caracteres, manteniendo el mismo tono y sin añadir metadatos ni encabezados. Texto: \"$shortText\""
        val url = "https://generativelanguage.googleapis.com/v1/${modelName}:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", expandPrompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.0)
                put("maxOutputTokens", 2048)
            })
        }.toString()

        Log.d("ERROR_INTERPRETACION", "HTTP Expand Request JSON: $requestJson")

        val req = Request.Builder().url(url).post(requestJson.toRequestBody(contentType)).build()
        try {
            httpClient.newCall(req).execute().use { resp ->
                val bodyText = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    Log.w("ERROR_INTERPRETACION", "Expand HTTP ${resp.code}: $bodyText")
                    return ""
                }
                try {
                    val j = JSONObject(bodyText)
                    // reuse extractor
                    if (j.has("candidates")) {
                        val candidates = j.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val contentObj = candidate.opt("content")
                            val extracted = extractTextFromContent(contentObj)
                            return extracted
                        }
                    }
                    if (j.has("outputs")) {
                        val outputs = j.getJSONArray("outputs")
                        if (outputs.length() > 0) {
                            val outObj = outputs.getJSONObject(0)
                            val contentObj = outObj.opt("content")
                            val extracted = extractTextFromContent(contentObj)
                            return extracted
                        }
                    }
                    return bodyText
                } catch (e: Throwable) {
                    Log.w("ERROR_INTERPRETACION", "Expand parse failed: ${e.message}")
                    return bodyText
                }
            }
        } catch (e: Throwable) {
            Log.w("ERROR_INTERPRETACION", "Expand request exception: ${e.message}")
            return ""
        }
    }
}
