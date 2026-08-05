package com.example.ibtech.data.repository

import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.FacilitySyncStatus
import com.example.ibtech.domain.model.GuideMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * [Facility] 목록과 JSON 문자열 사이의 변환. `org.json`(Android 프레임워크 내장)만 쓰고
 * 새 직렬화 라이브러리는 들이지 않는다.
 */
object FacilityJsonMapper {

    fun toJson(facilities: List<Facility>): String {
        val array = JSONArray()
        facilities.forEach { facility ->
            val obj = JSONObject()
            obj.put("id", facility.id)
            obj.put("sourcePoiName", facility.sourcePoiName)
            obj.put("name", facility.name)
            obj.put("floor", facility.floor)
            obj.put("shortDescription", facility.shortDescription)
            obj.put("guideMode", facility.guideMode.name)
            obj.put("directionText", facility.directionText ?: JSONObject.NULL)
            obj.put("mapImagePath", facility.mapImagePath ?: JSONObject.NULL)
            obj.put("iconKey", facility.iconKey ?: JSONObject.NULL)
            obj.put("isEnabled", facility.isEnabled)
            obj.put("isFeatured", facility.isFeatured)
            obj.put("sortOrder", facility.sortOrder)
            obj.put("syncStatus", facility.syncStatus.name)
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(json: String): List<Facility> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                Facility(
                    id = obj.getString("id"),
                    sourcePoiName = obj.getString("sourcePoiName"),
                    name = obj.getString("name"),
                    floor = obj.getInt("floor"),
                    shortDescription = obj.optString("shortDescription", ""),
                    guideMode = runCatching { GuideMode.valueOf(obj.getString("guideMode")) }
                        .getOrDefault(GuideMode.LOCATION_ONLY),
                    directionText = obj.optStringOrNull("directionText"),
                    mapImagePath = obj.optStringOrNull("mapImagePath"),
                    iconKey = obj.optStringOrNull("iconKey"),
                    isEnabled = obj.optBoolean("isEnabled", false),
                    isFeatured = obj.optBoolean("isFeatured", false),
                    sortOrder = obj.optInt("sortOrder", 0),
                    syncStatus = runCatching { FacilitySyncStatus.valueOf(obj.getString("syncStatus")) }
                        .getOrDefault(FacilitySyncStatus.SYNCED)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
}
