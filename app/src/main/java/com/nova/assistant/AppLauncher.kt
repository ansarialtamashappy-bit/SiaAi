package com.nova.assistant

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.provider.Settings

class AppLauncher(private val context: Context) {

    private class KnownApp(
        val label: String,
        val aliases: List<String>,
        val pkg: String
    )

    private val knownApps = listOf(
        KnownApp("व्हाट्सऐप",
            listOf("whatsapp", "whats app", "व्हाट्सएप", "व्हाट्सऐप", "वाट्सएप", "वॉट्सऐप", "व्हॉट्सएप", "व्हाट्स ऐप"),
            "com.whatsapp"),
        KnownApp("यूट्यूब",
            listOf("youtube", "you tube", "यूट्यूब", "यू ट्यूब", "यूटूब"),
            "com.google.android.youtube"),
        KnownApp("क्रोम",
            listOf("chrome", "क्रोम", "browser", "ब्राउज़र", "ब्राउजर"),
            "com.android.chrome"),
        KnownApp("मैप्स",
            listOf("maps", "map", "मैप", "मैप्स", "नक्शा"),
            "com.google.android.apps.maps"),
        KnownApp("इंस्टाग्राम",
            listOf("instagram", "इंस्टाग्राम", "इंस्टा"),
            "com.instagram.android"),
        KnownApp("फेसबुक",
            listOf("facebook", "फेसबुक"),
            "com.facebook.katana"),
        KnownApp("जीमेल",
            listOf("gmail", "जीमेल", "mail", "मेल"),
            "com.google.android.gm"),
        KnownApp("प्ले स्टोर",
            listOf("play store", "playstore", "प्ले स्टोर", "प्लेस्टोर"),
            "com.android.vending"),
        KnownApp("टेलीग्राम",
            listOf("telegram", "टेलीग्राम"),
            "org.telegram.messenger"),
        KnownApp("फोटोज़",
            listOf("photos", "फोटोज", "फोटो", "gallery", "गैलरी"),
            "com.google.android.apps.photos"),
        KnownApp("फोनपे",
            listOf("phonepe", "phone pe", "फोनपे", "फोन पे"),
            "com.phonepe.app"),
        KnownApp("पेटीएम",
            listOf("paytm", "पेटीएम"),
            "net.one97.paytm"),
        KnownApp("गूगल पे",
            listOf("google pay", "gpay", "गूगल पे", "जीपे"),
            "com.google.android.apps.nbu.paisa.user"),
        KnownApp("स्पॉटिफ़ाई",
            listOf("spotify", "स्पॉटिफाई", "स्पॉटिफ़ाई"),
            "com.spotify.music")
    )

    fun open(spoken: String, target: String): String {
        // 1. Pehle jaane-pehchaane apps
        for (app in knownApps) {
            if (app.aliases.any { spoken.contains(it) }) {
                return launchPackage(app.pkg, app.label)
            }
        }

        // 2. System cheezein
        if (listOf("camera", "कैमरा", "कैमरे").any { spoken.contains(it) }) {
            return startIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "कैमरा")
        }
        if (listOf("settings", "setting", "सेटिंग").any { spoken.contains(it) }) {
            return startIntent(Intent(Settings.ACTION_SETTINGS), "सेटिंग्स")
        }
        if (listOf("phone", "dialer", "dial", "फोन", "फ़ोन", "डायलर").any { spoken.contains(it) }) {
            return startIntent(Intent(Intent.ACTION_DIAL), "फोन")
        }

        if (target.isBlank()) {
            return "कौन सा ऐप खोलूँ? जैसे कहिए: यूट्यूब खोलो।"
        }

        // 3. Phone mein installed apps ke naam se dhundhna
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(mainIntent, 0)

        for (ri in apps) {
            val label = ri.loadLabel(pm).toString().lowercase()
            if (label.length >= 3 && spoken.contains(label)) {
                return launchPackage(ri.activityInfo.packageName, ri.loadLabel(pm).toString())
            }
        }
        if (target.length >= 3) {
            for (ri in apps) {
                val label = ri.loadLabel(pm).toString().lowercase()
                if (label.contains(target)) {
                    return launchPackage(ri.activityInfo.packageName, ri.loadLabel(pm).toString())
                }
            }
        }

        return "मुझे $target नाम का ऐप नहीं मिला। ऐप का नाम अंग्रेज़ी में बोलकर देखिए।"
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
