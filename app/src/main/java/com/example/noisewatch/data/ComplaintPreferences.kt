package com.example.noisewatch.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class ComplaintRecipient(
    val id: String,
    val name: String,
    val email: String
)

class ComplaintPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("noisewatch_complaint_prefs", Context.MODE_PRIVATE)

    fun getRecipients(): List<ComplaintRecipient> {
        val jsonStr = prefs.getString(KEY_RECIPIENTS_JSON, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<ComplaintRecipient>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ComplaintRecipient(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        name = obj.optString("name", ""),
                        email = obj.optString("email", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveRecipients(list: List<ComplaintRecipient>) {
        val jsonArray = JSONArray()
        list.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("name", r.name)
            obj.put("email", r.email)
            jsonArray.put(obj)
        }
        prefs.edit { putString(KEY_RECIPIENTS_JSON, jsonArray.toString()) }
    }

    fun getDefaultSubject(): String {
        return prefs.getString(KEY_DEFAULT_SUBJECT, DEFAULT_SUBJECT_TEMPLATE) ?: DEFAULT_SUBJECT_TEMPLATE
    }

    fun saveDefaultSubject(subject: String) {
        prefs.edit { putString(KEY_DEFAULT_SUBJECT, subject) }
    }

    fun getDefaultBody(): String {
        return prefs.getString(KEY_DEFAULT_BODY, DEFAULT_BODY_TEMPLATE) ?: DEFAULT_BODY_TEMPLATE
    }

    fun saveDefaultBody(body: String) {
        prefs.edit { putString(KEY_DEFAULT_BODY, body) }
    }

    companion object {
        private const val KEY_RECIPIENTS_JSON = "key_recipients_json"
        private const val KEY_DEFAULT_SUBJECT = "key_default_subject"
        private const val KEY_DEFAULT_BODY = "key_default_body"

        const val DEFAULT_SUBJECT_TEMPLATE = "Noise complaint – {location} – {date}"

        val DEFAULT_BODY_TEMPLATE = """
Dear Sir/Madam,

I am writing to report a noise-related incident documented using NoiseWatch.

Date: {date}
Time: {time}
Location: {location}
Coordinates: {latitude}, {longitude}
Location accuracy: {accuracy}

LAeq: {laeq} dB(A)
Maximum: {max} dB(A)
Minimum: {min} dB(A)
Duration: {duration}

Noise source: {source}

Additional notes:
{notes}

A NoiseWatch incident report is attached for reference.

I request the competent authority to examine the matter and take appropriate action under the Noise Pollution (Regulation and Control) Rules, 2000, applicable Government of Maharashtra / Maharashtra Pollution Control Board directions, and other applicable law. The Supreme Court has also recognised protection from excessive noise in In Re: Noise Pollution – Restricting Use of Loudspeakers (2005).

Phone-based measurements are indicative and are not certified enforcement measurements. Authorities may require verification using calibrated equipment.

Regards,
        """.trimIndent()
    }
}
