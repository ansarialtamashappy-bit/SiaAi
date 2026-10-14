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

    private fun timeReply(): String {
        val c = Calendar.getInstance()
        val h24 = c.get(Calendar.HOUR_OF_DAY)
        val min = c.get(Calendar.MINUTE)
        var h12 = h24 % 12
        if (h12 == 0) h12 = 12
        val part = when (h24) {
            in 4..11 -> "सुबह"
            in 12..15 -> "दोपहर"
            in 16..19 -> "शाम"
            else -> "रात"
        }
        return if (min == 0) {
            "अभी $part के $h12 बजे हैं।"
        } else {
            "अभी $part के $h12 बजकर $min मिनट हुए हैं।"
        }
    }

    private fun dateReply(): String {
        val c = Calendar.getInstance()
        val day = days[c.get(Calendar.DAY_OF_WEEK) - 1]
        val d = c.get(Calendar.DAY_OF_MONTH)
        val m = months[c.get(Calendar.MONTH)]
        val y = c.get(Calendar.YEAR)
        return "आज $day है, $d $m $y।"
    }

    fun reply(raw: String): String {
        val t = raw.lowercase().trim()
        val tokens = t.split(Regex("\\s+")).toSet()

        if (openWords.any { t.contains(it) }) {
            var s = t
            for (w in openWords) s = s.replace(w, " ")
            val target = s.split(Regex("\\s+"))
                .filter { it.isNotBlank() && it !in fillerWords }
                .joinToString(" ")
            return launcher.open(t, target)
        }

        if (timeWords.any { t.contains(it) }) return timeReply()

        if (tokens.contains("date") || dateWords.any { t.contains(it) }) return dateReply()

        val nameWords = listOf("tumhara naam", "aapka naam", "kaun ho", "तुम्हारा नाम", "आपका नाम", "कौन हो")
        if (nameWords.any { t.contains(it) }) {
            return "मेरा नाम सिया है। मैं आपका वॉइस असिस्टेंट हूँ।"
        }

        val helpWords = listOf("kya kar sakte", "help", "madad", "क्या कर सकते", "मदद", "हेल्प")
        if (helpWords.any { t.contains(it) }) {
            return "मैं समय और तारीख बता सकती हूँ, और ऐप खोल सकती हूँ। जैसे कहिए: यूट्यूब खोलो।"
        }

        val thanksWords = listOf("thanks", "thank you", "shukriya", "dhanyavad", "धन्यवाद", "शुक्रिया", "थैंक")
        if (thanksWords.any { t.contains(it) }) {
            return "आपका स्वागत है!"
        }

        val greetings = setOf(
            "hello", "hi", "hey", "namaste", "नमस्ते", "हेलो", "हैलो", "हाय",
            "sia", "सिया", "nova", "नोवा"
        )
        if (tokens.any { it in greetings }) {
            return "नमस्ते! मैं सिया हूँ। बताइए, मैं आपकी क्या मदद करूँ?"
        }

        return "माफ़ कीजिए, मुझे समझ नहीं आया। आप कह सकते हैं: समय क्या हुआ है, या यूट्यूब खोलो।"
    }
}
