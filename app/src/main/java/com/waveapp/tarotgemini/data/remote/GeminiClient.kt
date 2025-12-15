package com.waveapp.tarotgemini.data.remote

import android.util.Log
import com.waveapp.tarotgemini.data.model.DrawnCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

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

    // Timeout y reintentos
    private val requestTimeoutMs = 30_000L
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
        return buildString {
            appendLine("Eres un experto tarotista con años de experiencia en lectura de cartas.")
            appendLine("Tu objetivo es proporcionar interpretaciones profundas, místicas y significativas.")
            appendLine()
            appendLine("PREGUNTA DEL CONSULTANTE:")
            appendLine("\"$question\"")
            appendLine()
            appendLine("CARTAS DE LA TIRADA:")
            drawnCards.forEachIndexed { index, drawnCard ->
                appendLine()
                appendLine("Posición ${index + 1}: ${drawnCard.positionMeaning}")
                appendLine("Carta: ${drawnCard.card.name}")
                appendLine("Orientación: ${if (drawnCard.isReversed) "Invertida" else "Normal"}")
                val meaning = if (drawnCard.isReversed) drawnCard.card.reversedMeaning else drawnCard.card.uprightMeaning
                appendLine("Significado: $meaning")
            }
            appendLine()
            appendLine("Por favor, proporciona:")
            appendLine("1. Una interpretación general de la tirada completa")
            appendLine("2. Cómo las cartas se relacionan entre sí")
            appendLine("3. Un mensaje final o consejo para el consultante")
            appendLine()
            appendLine("Usa un tono místico pero accesible, comprensivo y esperanzador.")
            appendLine()
            appendLine("IMPORTANTE: No repitas el prompt ni las instrucciones. Devuelve únicamente la interpretación en texto plano, sin encabezados técnicos ni el prompt. Limítate a la interpretación completa en lenguaje natural.")
        }
    }

    // Intentar llamar a la SDK oficial por reflexión para evitar dependencias estáticas en tiempo de compilación
    private fun tryCallSdk(prompt: String): Result<String> {
        // Construir payload básico para la REST API por si necesitamos usarlo en el fallback
        val restBody = JSONObject().apply {
            put("prompt", JSONObject().apply {
                put("messages", listOf(JSONObject().apply {
                    put("author", "user")
                    put("content", listOf(JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    }))
                }))
            })
            put("temperature", 0.7)
            put("max_output_tokens", 512)
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
        // Construir el JSON según la especificación REST para generateContent/generate
        val requestJson = JSONObject().apply {
            // NEW: usar "input" / "instructions" bajo la versión v1? Usaremos la forma recomendada por la API de Generative Language
            // Basado en la respuesta del servidor, el endpoint espera 'prompt' con 'messages' OR puede aceptar 'input' dependiendo de la versión.
            // Usaremos la forma que tus curls hicieron con modelos:generateContent en v1: enviar 'prompt' con messages.
            put("prompt", JSONObject().apply {
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("author", "user")
                        put("content", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", prompt)
                            })
                        })
                    })
                })
            })
            put("temperature", 0.7)
            put("maxOutputTokens", 512)
        }.toString()

        // Log request body completo
        Log.d("ERROR_INTERPRETACION", "HTTP Request JSON: $requestJson")

        val url = "https://generativelanguage.googleapis.com/v1/${modelName}:generateContent?key=$apiKey"
        val req = Request.Builder()
            .url(url)
            .post(requestJson.toRequestBody(contentType))
            .build()

        try {
            httpClient.newCall(req).execute().use { resp ->
                val bodyText = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    Log.e("ERROR_INTERPRETACION", "HTTP ${resp.code}: $bodyText")
                    return Result.failure(IOException("HTTP ${resp.code}: $bodyText"))
                }

                Log.d("ERROR_INTERPRETACION", "HTTP ${resp.code} successful. Response body: $bodyText")

                // Extraer texto de la respuesta JSON según la forma esperada
                try {
                    val j = JSONObject(bodyText)
                    // La estructura puede variar; intentaremos extraer campos comunes
                    if (j.has("candidates")) {
                        val candidates = j.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val text = candidates.getJSONObject(0).optString("output")
                            if (text.isNotBlank()) return Result.success(text)
                        }
                    }

                    // Alternativa: "output" o "content" u otros
                    if (j.has("output")) {
                        return Result.success(j.optString("output"))
                    }

                    // Otra posible estructura: "content" -> [{"text":"..."}]
                    if (j.has("content")) {
                        val contentArr = j.getJSONArray("content")
                        if (contentArr.length() > 0) {
                            val first = contentArr.getJSONObject(0)
                            val txt = first.optString("text", first.optString("output", ""))
                            if (txt.isNotBlank()) return Result.success(txt)
                        }
                    }

                    // Si no encontramos, devolver el JSON completo como fallback
                    return Result.success(bodyText)
                } catch (je: Throwable) {
                    Log.e("ERROR_INTERPRETACION", "Error parsing response JSON: ${je.message}")
                    return Result.success(bodyText)
                }
            }
        } catch (e: Throwable) {
            Log.e("ERROR_INTERPRETACION", "Exception during HTTP call: ${e.message}", e)
            return Result.failure(e)
        }
    }

    // Generador local para fallback cuando la API no está disponible o falla
    private fun generateFakeResponse(prompt: String): String {
        // Intentamos construir una interpretación sencilla a partir del prompt: extraer nombres de cartas si aparecen
        val cardNames = Regex("Carta: ([\\p{L}0-9_\\- ]+)").findAll(prompt).map { it.groupValues[1].trim() }.toList()
        val sb = StringBuilder()
        if (cardNames.isNotEmpty()) {
            sb.appendLine("Interpretación (simulada):")
            sb.appendLine()
            sb.appendLine("Resumen de las cartas: ${cardNames.joinToString(", ")}")
            sb.appendLine()
            sb.appendLine("En general, estas cartas sugieren una mezcla de introspección y acción. Observa las posiciones y cómo se relacionan entre sí: algunas cartas apuntan a desafíos, otras a oportunidades.")
            sb.appendLine()
            sb.appendLine("Consejo: toma tiempo para reflexionar, comunica tus inquietudes con claridad y actúa con intención.")
        } else {
            sb.appendLine("Interpretación (simulada): Las cartas indican que se aproxima un periodo de cambio. Mantén la mente abierta y actúa con honestidad.")
        }
        return sb.toString()
    }
}
