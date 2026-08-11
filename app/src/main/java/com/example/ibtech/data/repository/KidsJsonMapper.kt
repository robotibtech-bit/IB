package com.example.ibtech.data.repository

import com.example.ibtech.domain.model.LibraryEtiquetteTip
import com.example.ibtech.domain.model.QuizQuestion
import com.example.ibtech.domain.model.RecommendedBook
import org.json.JSONArray
import org.json.JSONObject

/**
 * 어린이 콘텐츠 세 모델과 JSON 문자열 사이의 변환. [FacilityJsonMapper]와 동일하게
 * `org.json`(Android 프레임워크 내장)만 쓰고 새 직렬화 라이브러리는 들이지 않는다.
 */
object KidsJsonMapper {

    fun quizToJson(questions: List<QuizQuestion>): String {
        val array = JSONArray()
        questions.forEach { q ->
            val obj = JSONObject()
            obj.put("id", q.id)
            obj.put("category", q.category)
            obj.put("question", q.question)
            obj.put("choices", JSONArray(q.choices))
            obj.put("correctIndex", q.correctIndex)
            obj.put("explanation", q.explanation)
            obj.put("recommendedBookIds", JSONArray(q.recommendedBookIds))
            obj.put("isEnabled", q.isEnabled)
            obj.put("sortOrder", q.sortOrder)
            obj.put("imageKey", q.imageKey ?: JSONObject.NULL)
            array.put(obj)
        }
        return array.toString()
    }

    fun quizFromJson(json: String): List<QuizQuestion> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                QuizQuestion(
                    id = obj.getString("id"),
                    category = obj.getString("category"),
                    question = obj.getString("question"),
                    choices = obj.getJSONArray("choices").toStringList(),
                    correctIndex = obj.getInt("correctIndex"),
                    explanation = obj.optString("explanation", ""),
                    recommendedBookIds = obj.optJSONArray("recommendedBookIds")?.toStringList().orEmpty(),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    sortOrder = obj.optInt("sortOrder", 0),
                    imageKey = obj.optStringOrNull("imageKey")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun booksToJson(books: List<RecommendedBook>): String {
        val array = JSONArray()
        books.forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("title", b.title)
            obj.put("author", b.author)
            obj.put("ageGroup", b.ageGroup ?: JSONObject.NULL)
            obj.put("topic", b.topic ?: JSONObject.NULL)
            obj.put("description", b.description)
            obj.put("coverPath", b.coverPath ?: JSONObject.NULL)
            obj.put("locationText", b.locationText ?: JSONObject.NULL)
            obj.put("isEnabled", b.isEnabled)
            obj.put("sortOrder", b.sortOrder)
            obj.put("tags", JSONArray(b.tags))
            array.put(obj)
        }
        return array.toString()
    }

    fun booksFromJson(json: String): List<RecommendedBook> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                RecommendedBook(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    author = obj.getString("author"),
                    ageGroup = obj.optStringOrNull("ageGroup"),
                    topic = obj.optStringOrNull("topic"),
                    description = obj.optString("description", ""),
                    coverPath = obj.optStringOrNull("coverPath"),
                    locationText = obj.optStringOrNull("locationText"),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    sortOrder = obj.optInt("sortOrder", 0),
                    tags = obj.optJSONArray("tags")?.toStringList().orEmpty()
                )
            }
        }.getOrDefault(emptyList())
    }

    fun etiquetteToJson(tips: List<LibraryEtiquetteTip>): String {
        val array = JSONArray()
        tips.forEach { tip ->
            val obj = JSONObject()
            obj.put("id", tip.id)
            obj.put("text", tip.text)
            obj.put("isEnabled", tip.isEnabled)
            obj.put("sortOrder", tip.sortOrder)
            array.put(obj)
        }
        return array.toString()
    }

    fun etiquetteFromJson(json: String): List<LibraryEtiquetteTip> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                LibraryEtiquetteTip(
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

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }
}
