package com.example.ibtech.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.ibtech.domain.model.AppReleaseInfo
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * [KSH/updater] 프로젝트의 `UpdateRepository`를 이식했다. 원본은 별도의 "설치용 앱"이 대상 앱의
 * 설치 여부/버전을 확인해 APK를 내려받아 설치했지만, 이 앱은 자기 자신을 갱신하므로 대상
 * packageName은 항상 [Context.getPackageName]이다.
 */
class AppUpdateRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val selfPackageName = context.packageName

    fun getInstalledVersionCode(): Long {
        val info = packageManager.getPackageInfo(selfPackageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION") info.versionCode.toLong()
        }
    }

    fun fetchLatestRelease(): AppReleaseInfo {
        val url = requireHttps(AppUpdateConfig.LATEST_JSON_URL, "버전 정보 URL")
        val connection = openConnection(url)
        return try {
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseRelease(body).also { release ->
                if (release.packageName != selfPackageName) {
                    throw InvalidReleaseException("서버의 packageName이 이 앱과 일치하지 않습니다.")
                }
                requireHttps(release.apkUrl, "APK URL")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun downloadApk(release: AppReleaseInfo, onProgress: (Int) -> Unit): File {
        val connection = openConnection(requireHttps(release.apkUrl, "APK URL"))
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val partial = File(directory, "app-update.apk.part")
        val output = File(directory, "app-update.apk")
        try {
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                partial.outputStream().buffered().use { sink ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        sink.write(buffer, 0, count)
                        downloaded += count
                        if (total > 0) onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            if (partial.length() == 0L) throw IOException("다운로드된 APK 파일이 비어 있습니다.")
            if (output.exists() && !output.delete()) throw IOException("이전 APK 파일을 지울 수 없습니다.")
            if (!partial.renameTo(output)) throw IOException("다운로드 파일을 확정할 수 없습니다.")
            verifyApk(output, release.packageName)
            onProgress(100)
            return output
        } catch (error: Exception) {
            partial.delete()
            output.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun verifyApk(file: File, expectedPackage: String) {
        val archive = packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            ?: throw InvalidApkException("다운로드 파일을 유효한 APK로 확인할 수 없습니다.")
        if (archive.packageName != expectedPackage) {
            throw InvalidApkException("APK packageName이 이 앱과 일치하지 않습니다.")
        }
    }

    private fun parseRelease(body: String): AppReleaseInfo = try {
        val json = JSONObject(body)
        AppReleaseInfo(
            packageName = json.getString("packageName"),
            versionCode = json.getLong("versionCode"),
            versionName = json.getString("versionName"),
            apkUrl = json.getString("apkUrl"),
            releaseNote = json.optString("releaseNote", "업데이트 내용이 없습니다."),
        ).also {
            if (it.versionCode < 1 || it.versionName.isBlank()) {
                throw InvalidReleaseException("버전 정보 값이 올바르지 않습니다.")
            }
        }
    } catch (error: JSONException) {
        throw InvalidReleaseException("latest.json 형식을 해석할 수 없습니다.", error)
    }

    private fun openConnection(url: URL): HttpsURLConnection {
        val connection = url.openConnection() as HttpsURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/json, application/vnd.android.package-archive")
        connection.setRequestProperty("User-Agent", "IBTechAppUpdater/1.0")
        connection.connect()
        if (connection.responseCode !in 200..299) {
            val code = connection.responseCode
            connection.disconnect()
            throw HttpStatusException(code)
        }
        return connection
    }

    private fun requireHttps(value: String, label: String): URL {
        val url = try {
            URL(value)
        } catch (error: MalformedURLException) {
            throw InvalidReleaseException("$label 형식이 올바르지 않습니다.", error)
        }
        if (url.protocol != "https") throw InvalidReleaseException("${label}은 HTTPS여야 합니다.")
        return url
    }

    companion object {
        @Volatile private var instance: AppUpdateRepository? = null

        fun getInstance(context: Context): AppUpdateRepository =
            instance ?: synchronized(this) {
                instance ?: AppUpdateRepository(context.applicationContext).also { instance = it }
            }
    }
}

/** GitHub Releases에 latest.json/APK를 올려두는 배포 방식(원본 [KSH/updater] 프로젝트와 동일)을 그대로 쓴다. */
object AppUpdateConfig {
    const val LATEST_JSON_URL = "https://raw.githubusercontent.com/robotibtech-bit/IB/main/latest.json"
}

class HttpStatusException(val statusCode: Int) : IOException("HTTP $statusCode")
class InvalidReleaseException(message: String, cause: Throwable? = null) : Exception(message, cause)
class InvalidApkException(message: String) : Exception(message)

fun Throwable.toUpdateErrorMessage(operation: String): String = when (this) {
    is SocketTimeoutException -> "$operation 시간이 초과되었습니다. 인터넷 연결을 확인하고 다시 시도해 주세요."
    is java.net.UnknownHostException, is java.net.ConnectException ->
        "인터넷에 연결할 수 없습니다. 네트워크를 확인하고 다시 시도해 주세요."
    is HttpStatusException -> "서버에서 파일을 가져오지 못했습니다. (HTTP $statusCode)"
    is InvalidReleaseException, is InvalidApkException -> message ?: "${operation}에 실패했습니다."
    is IOException -> "$operation 중 오류가 발생했습니다. 다시 시도해 주세요."
    else -> "${operation} 중 알 수 없는 오류가 발생했습니다."
}
