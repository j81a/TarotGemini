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
        // Construir el JSON según la especificación REST para generateContent/generate
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
                put("temperature", 0.7)
                put("maxOutputTokens", 512)
            })
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
                    // La estructura puede variar; intentaremos extraer texto de varias formas comunes
                    // 1) candidates -> [0] -> content -> parts -> [0] -> text
                    if (j.has("candidates")) {
                        val candidates = j.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            // Extraer text flexible desde 'content' (puede ser JSONObject/JSONArray/string)
                            val contentObj = candidate.opt("content")
                            val extracted = extractTextFromContent(contentObj)
                            if (extracted.isNotBlank()) {
                                // Log de texto extraído (por seguridad truncamos a 1000 chars en log)
                                val preview = if (extracted.length > 1000) extracted.substring(0, 1000) + "..." else extracted
                                Log.d("ERROR_INTERPRETACION", "Extracted text length=${extracted.length}. Preview: $preview")
                                // Si la generación fue truncada por tokens, avisar
                                val finishReason = candidate.optString("finishReason", candidate.optString("finish_reason", ""))
                                if (finishReason.equals("MAX_TOKENS", ignoreCase = true)) {
                                    Log.w("ERROR_INTERPRETACION", "Response truncated (finishReason=MAX_TOKENS). Consider increasing maxOutputTokens if needed.")
                                }
                                return Result.success(extracted)
                            }
                            // fallback: candidate might have 'output' field
                            val out = candidate.optString("output", "")
                            if (out.isNotBlank()) return Result.success(out)
                        }
                    }

                    // 2) outputs -> [0] -> content -> parts -> [0] -> text
                    if (j.has("outputs")) {
                        val outputs = j.getJSONArray("outputs")
                        if (outputs.length() > 0) {
                            val outObj = outputs.getJSONObject(0)
                            val contentObj = outObj.opt("content")
                            val extracted = extractTextFromContent(contentObj)
                            if (extracted.isNotBlank()) {
                                val preview = if (extracted.length > 1000) extracted.substring(0, 1000) + "..." else extracted
                                Log.d("ERROR_INTERPRETACION", "Extracted text from outputs length=${extracted.length}. Preview: $preview")
                                val finishReason = outObj.optString("finishReason", outObj.optString("finish_reason", ""))
                                if (finishReason.equals("MAX_TOKENS", ignoreCase = true)) {
                                    Log.w("ERROR_INTERPRETACION", "Outputs truncated (finishReason=MAX_TOKENS).")
                                }
                                return Result.success(extracted)
                            }
                        }
                    }

                    // 3) top-level fields
                    if (j.has("output")) return Result.success(j.optString("output"))
                    if (j.has("content")) {
                        val contentObj = j.opt("content")
                        val extracted = extractTextFromContent(contentObj)
                        if (extracted.isNotBlank()) return Result.success(extracted)
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
}
