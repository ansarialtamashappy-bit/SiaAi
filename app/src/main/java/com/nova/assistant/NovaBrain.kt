package com.nova.assistant

import java.util.Calendar

class NovaBrain(private val launcher: AppLauncher) {

    private val openWords = listOf(
        "kholo", "kholna", "khol do", "khol", "open", "launch", "start",
        "chalu karo", "chalao",
        "खोल दो", "खोलो", "खोलिए", "खोलना", "खोल", "चालू करो", "चलाओ", "ओपन", "स्टार्ट"
    ).sortedByDescending { it.length }

    private val fillerWords = setOf(
        "sia", "सिया", "nova", "नोवा", "please", "plz", "प्लीज", "प्लीज़",
        "zara", "ज़रा", "जरा", "app", "ऐप", "एप", "ko", "को", "karo", "करो", "kar", "do", "दो"
    )

    private val timeWords = listOf(
        "time", "samay", "waqt", "wakt", "baje", "kitne baj",
        "टाइम", "समय", "वक्त", "बजे", "कितने बज"
    )

    private val dateWords = listOf(
        "tarikh", "tareekh", "kaun sa din", "aaj ka din",
        "तारीख", "कौन सा दिन", "आज का दिन"
    )

    private val days = arrayOf(
        "रविवार", "सोमवार", "मंगलवार", "बुधवार", "गुरुवार", "शुक्रवार", "शनिवार"
    )

    private val months = arrayOf(
        "जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून",
        "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
    )

    fun reply(raw: String): String {
        val t = raw.lowercase().trim()
        val tokens = t.split(Regex("\\s+")).toSet()

        // 1. App kholna
        if (openWords.any { t.contains(it) }) {
            var s = t
            for (w in openWords) s = s.replace(w, " ")
            val target = s.split(Regex("\\s+"))
                .filter { it.isNotBlank() && it !in fillerWords }
                .joinToString(" ")
            return launcher.open(t, target)
        }

        // 2. Time
        if (timeWords.any { t.contains(it) }) return timeReply()

        // 3. Date
        if (tokens.contains("date") || dateWords.any { t.contains(it) }) return dateReply()

        // 4. Naam
        if (listOf("tumhara naam", "aapka naam", "kaun ho", "तुम्हारा नाम", "आपका नाम", "कौन हो")
                .any { t.contains(it) }
        ) {
            return "मेरा नाम सिया है। मैं आपका वॉइस असिस्टेंट हूँ।"
        }

        // 5. Help
        if (listOf("kya kar sakte", "help", "madad", "क्या कर सकते", "मदद", "हेल्प")
                .any { t.contains(it) }
        ) {
            return "मैं समय और तारीख बता सकती हूँ, और ऐप खोल सकती हूँ। जैसे कहिए: यूट्यूब खोलो।"
        }

        // 6. Thanks
        if (listOf("thanks", "thank you", "shukriya", "dhanyavad", "धन्यवाद", "शुक्रिया", "थैंक")
                .any { t.contains(it) }
        ) {
            return "आपका स्वागत है!"
        }

        // 7. Greeting
        val greetings = setOf(
            "hello", "hi", "hey", "namaste", "नमस्ते", "हेलो", "हैलो", "हाय",
            "sia", "सिया", "nova", "नोवा"
        )
        if (tokens.any { it in greetings }) {
            return "नमस्ते! मैं सिया हूँ। बताइए, मैं आपकी क्या मदद करूँ?"
        }

        return "माफ़ कीजिए, मुझे समझ नहीं आया। आप कह सकते हैं: समय क्या हुआ है, या
