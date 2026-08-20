package com.example.ibtech.data.booksearch

import android.util.Log
import com.example.ibtech.domain.model.BookDetail
import com.example.ibtech.domain.model.BookHit
import com.example.ibtech.domain.model.BookQueryPlan
import com.example.ibtech.domain.model.BookSearchResult
import com.example.ibtech.domain.model.BookShelf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 검색이 실패한 사유. 표시 문구는 UI 계층이 `strings.xml`로 매핑한다. */
enum class BookSearchIssue {
    /** 관리자가 아직 서버 주소를 넣지 않았다. */
    NOT_CONFIGURED,

    /** 주소는 있으나 형식이 잘못됐다. */
    INVALID_URL,

    /** 연결 실패·타임아웃. 와이파이가 끊겼거나 서버가 꺼져 있다. */
    UNREACHABLE,

    /** 서버가 200이 아닌 응답을 돌려줬다. */
    SERVER_ERROR,

    /** 응답이 예상한 JSON 형태가 아니다. */
    MALFORMED_RESPONSE
}

/** 검색 실패. [issue]로만 사유를 구분하고 문구는 만들지 않는다. */
class BookSearchException(
    val issue: BookSearchIssue,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * 도서검색 서버 클라이언트.
 *
 * HTTP 라이브러리를 새로 들이지 않고 `HttpURLConnection` + `org.json`으로 직접 구현한다 —
 * 이 프로젝트는 의존성을 엄격히 관리하고(`gradle/libs.versions.toml` 주석 참고) 여기서 쓰는
 * 엔드포인트는 `POST /search` 하나뿐이라 라이브러리를 추가할 만한 규모가 아니다.
 *
 * 서버 응답 스키마는 `ibLib-server/app/schemas.py`가 원본이다. 앱은 필요한 필드만 읽고
 * 모르는 필드는 무시하므로, 서버에 필드가 추가돼도 앱을 고칠 필요가 없다.
 */
class BookSearchApi(
    private val connectTimeoutMillis: Int = CONNECT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = READ_TIMEOUT_MILLIS
) {

    /**
     * [baseUrl]의 서버에 [query]를 검색한다.
     *
     * @param callPrefix 별치기호 한정. null 이면 전체 검색.
     * @throws BookSearchException 모든 실패는 이 예외로 정규화된다.
     */
    suspend fun search(
        baseUrl: String,
        query: String,
        top: Int = DEFAULT_TOP,
        callPrefix: String? = null
    ): BookSearchResult = withContext(Dispatchers.IO) {
        val endpoint = buildEndpoint(baseUrl)
        val body = JSONObject().apply {
            put("q", query)
            put("top", top)
            if (callPrefix != null) put("call_prefix", callPrefix)
        }.toString()

        val responseText = post(endpoint, body)
        parseSearchResponse(query, responseText)
    }

    /**
     * 등록번호 한 건의 상세. 표지·저자·출판사는 서버가 외부 서점에서 보강해 준다.
     *
     * 검색 결과 10건마다 부르지 않고 사용자가 고른 1건만 부른다 — 지연과 외부 API 쿼터를
     * 아끼기 위해서다.
     */
    suspend fun detail(baseUrl: String, bookId: String): BookDetail = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            throw BookSearchException(BookSearchIssue.NOT_CONFIGURED, "서버 주소가 설정되지 않았습니다.")
        }
        val encoded = java.net.URLEncoder.encode(bookId, "UTF-8")
        val endpoint = runCatching { URL(normalizeBaseUrl(baseUrl) + "/books/" + encoded) }
            .getOrElse { throw BookSearchException(BookSearchIssue.INVALID_URL, "주소 형식 오류", it) }
        parseDetail(get(endpoint))
    }

    /** 서버가 살아 있는지 확인한다. 관리자 화면의 "연결 확인" 버튼이 쓴다. */
    suspend fun health(baseUrl: String): String = withContext(Dispatchers.IO) {
        val endpoint = runCatching { URL(normalizeBaseUrl(baseUrl) + "/health") }
            .getOrElse { throw BookSearchException(BookSearchIssue.INVALID_URL, "주소 형식 오류", it) }
        val json = JSONObject(get(endpoint))
        val books = json.optInt("book_count")
        val provider = json.optString("provider")
        "장서 ${books}건 / 임베딩 $provider"
    }

    private fun buildEndpoint(baseUrl: String): URL {
        if (baseUrl.isBlank()) {
            throw BookSearchException(BookSearchIssue.NOT_CONFIGURED, "서버 주소가 설정되지 않았습니다.")
        }
        return runCatching { URL(normalizeBaseUrl(baseUrl) + "/search") }
            .getOrElse { throw BookSearchException(BookSearchIssue.INVALID_URL, "주소 형식 오류", it) }
    }

    private fun post(endpoint: URL, body: String): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = connectTimeoutMillis
                readTimeout = readTimeoutMillis
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            return readResponse(connection)
        } catch (e: BookSearchException) {
            throw e
        } catch (e: IOException) {
            throw BookSearchException(BookSearchIssue.UNREACHABLE, "서버에 연결하지 못했습니다.", e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun get(endpoint: URL): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMillis
                readTimeout = readTimeoutMillis
                setRequestProperty("Accept", "application/json")
            }
            return readResponse(connection)
        } catch (e: BookSearchException) {
            throw e
        } catch (e: IOException) {
            throw BookSearchException(BookSearchIssue.UNREACHABLE, "서버에 연결하지 못했습니다.", e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val code = connection.responseCode
        if (code !in 200..299) {
            // 오류 본문은 진단용으로만 남긴다. 사용자에게는 issue 로만 구분해 보여 준다.
            val detail = connection.errorStream?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?.take(ERROR_BODY_LOG_LIMIT)
                .orEmpty()
            Log.w(TAG, "검색 서버 오류 응답 $code: $detail")
            throw BookSearchException(BookSearchIssue.SERVER_ERROR, "서버 오류 ($code)")
        }
        return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun parseSearchResponse(query: String, text: String): BookSearchResult = try {
        val root = JSONObject(text)
        BookSearchResult(
            query = root.optString("query", query),
            plan = parsePlan(root.optJSONObject("plan")),
            hits = root.optJSONArray("results").toHits()
        )
    } catch (e: BookSearchException) {
        throw e
    } catch (e: Exception) {
        throw BookSearchException(BookSearchIssue.MALFORMED_RESPONSE, "응답을 이해하지 못했습니다.", e)
    }

    private fun parseDetail(text: String): BookDetail = try {
        val root = JSONObject(text)
        BookDetail(
            bookId = root.optString("book_id"),
            callNo = root.optString("call_no"),
            title = root.optString("title"),
            holding = root.optString("holding"),
            kdcLabel = root.optString("kdc_label"),
            shelf = root.optJSONObject("shelf")?.toShelf(),
            authors = root.optJSONArray("authors").toStringList(),
            translators = root.optJSONArray("translators").toStringList(),
            publisher = root.optString("publisher"),
            publishedYear = root.optString("published_year"),
            isbn = root.optString("isbn"),
            thumbnail = root.optString("thumbnail"),
            metaConfidence = root.optString("meta_confidence", "not_found")
        )
    } catch (e: BookSearchException) {
        throw e
    } catch (e: Exception) {
        throw BookSearchException(BookSearchIssue.MALFORMED_RESPONSE, "응답을 이해하지 못했습니다.", e)
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { optString(it) }.filter { it.isNotBlank() }
    }

    private fun parsePlan(json: JSONObject?): BookQueryPlan {
        if (json == null) return BookQueryPlan(query = "", keywords = emptyList())
        val keywords = json.optJSONArray("keywords")
        return BookQueryPlan(
            query = json.optString("query"),
            keywords = List(keywords?.length() ?: 0) { keywords!!.optString(it) }
                .filter { it.isNotBlank() }
        )
    }

    private fun JSONArray?.toHits(): List<BookHit> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            val item = optJSONObject(index) ?: return@mapNotNull null
            BookHit(
                bookId = item.optString("book_id"),
                callNo = item.optString("call_no"),
                title = item.optString("title"),
                author = item.optString("author"),
                holding = item.optString("holding"),
                sameTitleCount = item.optInt("same_title_count", 1),
                shelf = item.optJSONObject("shelf")?.toShelf()
            )
        }
    }

    private fun JSONObject.toShelf(): BookShelf {
        val locationName = optString("location_name")
        return BookShelf(
            locationName = locationName,
            shelfLabel = optString("shelf_label").ifBlank { locationName },
            room = optString("room"),
            // 서버는 층을 문자열로 준다("1", "4", 또는 빈 문자열). 숫자가 아니면 층을
            // 모르는 것으로 보고 동행 안내를 하지 않는다.
            floor = optString("floor").trim().toIntOrNull(),
            isEstimated = optBoolean("estimated", false)
        )
    }

    companion object {
        private const val TAG = "BookSearchApi"
        private const val CONNECT_TIMEOUT_MILLIS = 4_000
        private const val READ_TIMEOUT_MILLIS = 10_000
        private const val ERROR_BODY_LOG_LIMIT = 500
        const val DEFAULT_TOP = 10

        /** 뒤 슬래시와 앞뒤 공백을 정리한다. 관리자가 "http://1.2.3.4:8080/" 로 넣어도 동작하게. */
        fun normalizeBaseUrl(raw: String): String = raw.trim().trimEnd('/')
    }
}
