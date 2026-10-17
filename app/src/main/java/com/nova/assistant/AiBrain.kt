package com.nova.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AiBrain(private val context: Context) {

    private val prefs = context.getSharedPreferences("sia_prefs", Context.MODE_PRIVATE)

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
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent")
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
                return "AI se jawab nahi mila (code $code). Key ya internet check karein."
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(text)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        } catch (e: Exception) {
            "AI se baat nahi ho paayi. Internet check karein."
        }
    }
}
