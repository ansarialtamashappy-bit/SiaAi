package com.nova.assistant

import android.content.Context
import android.content.Intent
import android.icu.text.Transliterator
import android.provider.MediaStore
import android.provider.Settings

class AppLauncher(private val context: Context) {

    private val toLatin: Transliterator? = try {
        Transliterator.getInstance("Any-Latin; Latin-ASCII")
    } catch (e: Exception) {
        null
    }

    private fun skeleton(s: String): String {
        val latin = try {
            toLatin?.transliterate(s) ?: s
        } catch (e: Exception) {
            s
        }
        var t = latin.lowercase()
        t = t.replace("ph", "f").replace("w", "v").replace("c", "k").replace("q", "k")
        t = t.replace(Regex("[^a-z0-9]"), "")
        t = t.replace(Regex("[aeiouyh]"), "")
        t = t.replace(Regex("(.)\\1+"), "\$1")
        return t
    }

    private fun distance(a: String, b: String): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val tmp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + cost)
                prev = tmp
            }
        }
        return dp[b.length]
    }

    private fun allowed(len: Int): Int {
        return if (len <= 3) 0 else if (len <= 6) 1 else 2
    }

    private fun fits(cands: List<String>, name: String): Boolean {
        val n = skeleton(name)
        if (n.length < 2) return false
        return cands.any { it.length >= 2 && distance(it, n) <= allowed(n.length) }
    }

    fun open(spoken: String, target: String): String {
        val words = target.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) {
            return "कौन सा ऐप खोलूँ? जैसे कहिए: यूट्यूब खोलो।"
        }

        val cands = ArrayList<String>()
        for (i in words.indices) {
            cands.add(skeleton(words[i]))
            if (i + 1 < words.size) cands.add(skeleton(words[i] + words[i + 1]))
            if (i + 2 < words.size) cands.add(skeleton(words[i] + words[i + 1] + words[i + 2]))
        }

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(mainIntent, 0)

        var bestPkg: String? = null
        var bestLabel = ""
        var bestDist = 99
        var bestLen = 0

        for (ri in apps) {
            val label = ri.loadLabel(pm).toString()
            val n = skeleton(label)
            if (n.length < 2) continue
            val limit = allowed(n.length)
            for (c in cands) {
                if (c.length < 2) continue
                val d = distance(c, n)
                if (d <= limit && (d < bestDist || (d == bestDist && n.length > bestLen))) {
                    bestDist = d
                    bestLen = n.length
                    bestPkg = ri.activityInfo.packageName
                    bestLabel = label
                }
            }
        }

        val foundPkg = bestPkg
        if (foundPkg != null) {
            return launchPackage(foundPkg, bestLabel)
        }

        if (fits(cands, "camera")) {
            return startIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "कैमरा")
        }
        if (fits(cands, "settings")) {
            return startIntent(Intent(Settings.ACTION_SETTINGS), "सेटिंग्स")
        }
        if (fits(cands, "phone") || fits(cands, "dialer")) {
            return startIntent(Intent(Intent.ACTION_DIAL), "फोन")
        }

        return "मुझे ${words.joinToString(" ")} नाम का ऐप नहीं मिला। ऐप का नाम दोबारा बोलकर देखिए।"
    }

    private fun launchPackage(pkg: String, label: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return "$label इस फोन में इंस्टॉल नहीं है।"
        return startIntent(intent, label)
    }

    private fun startIntent(intent: Intent, label: String): String {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "ठीक है, $label खोल रहा हूँ।"
        } catch (e: Exception) {
            "$label नहीं खुल पाया।"
        }
    }
}
