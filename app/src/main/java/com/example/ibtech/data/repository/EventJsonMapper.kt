package com.example.ibtech.data.repository

import com.example.ibtech.domain.model.LibraryEvent
import com.example.ibtech.domain.model.LibraryNotice
import org.json.JSONArray
import org.json.JSONObject

/**
 * 행사·공지 모델과 JSON 문자열 사이의 변환. [KidsJsonMapper]와 동일하게 `org.json`만 쓴다.
 */
object EventJsonMapper {

    fun eventsToJson(events: List<LibraryEvent>): String {
        val array = JSONArray()
        events.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("title", e.title)
            obj.put("startDate", e.startDate)
            obj.put("endDate", e.endDate ?: JSONObject.NULL)
            obj.put("timeText", e.timeText ?: JSONObject.NULL)
            obj.put("place", e.place)
            obj.put("target", e.target ?: JSONObject.NULL)
            obj.put("description", e.description)
            obj.put("qrUrl", e.qrUrl ?: JSONObject.NULL)
            obj.put("relatedFacilityId", e.relatedFacilityId ?: JSONObject.NULL)
            obj.put("isEnabled", e.isEnabled)
            obj.put("sortOrder", e.sortOrder)
            array.put(obj)
        }
        return array.toString()
    }

    fun eventsFromJson(json: String): List<LibraryEvent> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                LibraryEvent(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    startDate = obj.getString("startDate"),
                    endDate = obj.optStringOrNull("endDate"),
                    timeText = obj.optStringOrNull("timeText"),
                    place = obj.optString("place", ""),
                    target = obj.optStringOrNull("target"),
                    description = obj.optString("description", ""),
                    qrUrl = obj.optStringOrNull("qrUrl"),
                    relatedFacilityId = obj.optStringOrNull("relatedFacilityId"),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    sortOrder = obj.optInt("sortOrder", 0)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun noticesToJson(notices: List<LibraryNotice>): String {
        val array = JSONArray()
        notices.forEach { n ->
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("text", n.text)
            obj.put("isEnabled", n.isEnabled)
            obj.put("sortOrder", n.sortOrder)
            array.put(obj)
        }
        return array.toString()
    }

    fun noticesFromJson(json: String): List<LibraryNotice> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                LibraryNotice(
                    id = obj.getString("id"),
                    text = obj.getString("text"),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    sortOrder = obj.optInt("sortOrder", 0)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
}
