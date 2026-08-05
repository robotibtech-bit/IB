package com.example.ibtech.data.repository

import com.example.ibtech.domain.model.UsageTopic
import org.json.JSONArray
import org.json.JSONObject

/**
 * [UsageTopic] 목록과 JSON 문자열 사이의 변환. [FacilityJsonMapper]와 동일하게 `org.json`
 * (Android 프레임워크 내장)만 쓰고 새 직렬화 라이브러리는 들이지 않는다.
 */
object UsageJsonMapper {

    fun toJson(topics: List<UsageTopic>): String {
        val array = JSONArray()
        topics.forEach { topic ->
            val obj = JSONObject()
            obj.put("id", topic.id)
            obj.put("parentId", topic.parentId ?: JSONObject.NULL)
            obj.put("title", topic.title)
            obj.put("shortAnswer", topic.shortAnswer ?: JSONObject.NULL)
            obj.put("tableData", topic.tableData ?: JSONObject.NULL)
            obj.put("qrUrl", topic.qrUrl ?: JSONObject.NULL)
            obj.put("relatedFacilityId", topic.relatedFacilityId ?: JSONObject.NULL)
            obj.put("isEnabled", topic.isEnabled)
            obj.put("sortOrder", topic.sortOrder)
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(json: String): List<UsageTopic> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                UsageTopic(
                    id = obj.getString("id"),
                    parentId = obj.optStringOrNull("parentId"),
                    title = obj.getString("title"),
                    shortAnswer = obj.optStringOrNull("shortAnswer"),
                    tableData = obj.optStringOrNull("tableData"),
                    qrUrl = obj.optStringOrNull("qrUrl"),
                    relatedFacilityId = obj.optStringOrNull("relatedFacilityId"),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    sortOrder = obj.optInt("sortOrder", 0)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
}
