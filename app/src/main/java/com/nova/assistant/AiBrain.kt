package com.nova.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AiBrain(private val context: Context) {

    private class Result(val ok: Boolean, val code: Int, val text: String)

    private val prefs = context.getSharedPreferences("sia_prefs", Context.MODE_PRIVATE)

    private val models = listOf(
        "gemini-2.5-flash",
        "gemini-3.5-flash",
        "gemini-3.1-flash-lite",
        "gemini-flash-latest"
    )

    fun saveKey(key: String) {
        prefs.edit().putString("api_key", key.trim()).apply()
    }

    fun hasKey(): Boolean {
        return !prefs.getString("api_key", "").isNullOrBlank()
    }

    fun ask(question: String): String {
        val key = prefs.getString("api_key", "") ?: ""
        if (key.isBlank()) {
            return "AI ke liye pehle API key daalni hogi."
        }
        var last = "AI se jawab nahi mila."
        for (m in models) {
            val r = call(key, m, question)
            if (r.ok) {
                return r.text
            }
            last = r.text
            if (r.code != 404) {
                break
            }
        }
        return last
    }

    private fun call(key: String, model: String, question: String): Result {
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", key)
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.doOutput = true

            val prompt = "Tum Sia ho, ek madadgaar female voice assistant. " +
                "Jawab chhota (2-3 vakya) aur usi bhasha mein do jis mein sawaal poochha gaya. " +
                "Sawaal: " + question
            val part = JSONObject().put("text", prompt)
            val content = JSONObject().put("parts", JSONArray().put(part))
            val body = JSONObject().put("contents", JSONArray().put(content))

            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val code = conn.responseCode
            if (code != 200) {
                Result(false, code, "AI se jawab nahi mila (code $code, model $model).")
            } else {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val parts = JSONObject(text)
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    sb.append(parts.getJSONObject(i).optString("text", ""))
                }
                val answer = sb.toString().trim()
                if (answer.isEmpty()) {
                    Result(false, 200, "AI ne khali jawab diya.")
                } else {
                    Result(true, 200, answer)
                }
            }
        } catch (e: Exception) {
            Result(false, -1, "AI error: " + e.javaClass.simpleName)
        }
    }
}
