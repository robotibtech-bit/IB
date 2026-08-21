plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.ibtech"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.ibtech"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "1.0-20260820.1448"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 여러 PC/작업자가 각자 다른 로컬 디버그 키(~/.android/debug.keystore)로 빌드하면 자체
    // 업데이터(AppUpdateRepository)가 배포마다 서명이 달라져 "앱이 설치되지 않았습니다" 오류로
    // 설치가 막힌다(2026-08-20 실제 발생 — 로컬 키스토어가 갱신되며 기존 설치와 서명이 어긋남).
    // 그래서 디버그 키를 로컬에 맡기지 않고 이 저장소에 커밋해 둔 app/debug.keystore 하나로
    // 고정한다 — 어느 PC에서 pull해서 빌드해도 항상 같은 서명이 나온다.
    //
    // release 서명키는 절대 이 저장소에 커밋하지 않는다 — 환경변수로만 전달받는다(정책,
    // 2026-08-21). 아래 네 값이 모두 있어야 release가 서명된다:
    //   IB_RELEASE_STORE_FILE / IB_RELEASE_STORE_PASSWORD / IB_RELEASE_KEY_ALIAS / IB_RELEASE_KEY_PASSWORD
    // 하나라도 없으면 assembleRelease/bundleRelease 자체를 막는다(아래 tasks.matching) — 서명 없이
    // 조용히 unsigned APK를 만들거나, 새 키를 임의로 생성하는 일은 절대 없다.
    val releaseStoreFile = providers.environmentVariable("IB_RELEASE_STORE_FILE").orNull
    val releaseStorePassword = providers.environmentVariable("IB_RELEASE_STORE_PASSWORD").orNull
    val releaseKeyAlias = providers.environmentVariable("IB_RELEASE_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.environmentVariable("IB_RELEASE_KEY_PASSWORD").orNull
    val hasReleaseSigningEnv = !releaseStoreFile.isNullOrBlank() &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            if (hasReleaseSigningEnv) {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isDebuggable = false
            // 우선순위: 안정성. minify/R8을 켜면 디버그와 동작이 달라질 위험이 있어 당장은
            // 끈 채로 유지한다(정책, 2026-08-21) — 기능은 debug와 동일하게 동작해야 한다.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigningEnv) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // release 서명키 환경변수가 없으면 release 빌드 자체를 아예 실패시킨다 — signingConfig를
    // 안 붙이면 Gradle은 에러 없이 조용히 unsigned APK를 만들어 버리는데, 그건 이 프로젝트
    // 정책(배포 중단 후 사용자에게 알림, 임의 키 생성 금지)에 어긋난다. package* 단계(실제
    // APK를 디스크에 쓰는 태스크)에도 걸어야 한다 — assemble*/bundle*에만 걸면 package가
    // 먼저 끝나 서명되지 않은 APK 파일이 실패한 빌드에도 결과물 폴더에 남는다.
    tasks.matching {
        it.name.startsWith("assembleRelease") ||
            it.name.startsWith("bundleRelease") ||
            it.name.startsWith("packageRelease")
    }.configureEach {
        doFirst {
            if (!hasReleaseSigningEnv) {
                throw GradleException(
                    "Release 서명 키가 설정되지 않았습니다. IB_RELEASE_STORE_FILE / " +
                        "IB_RELEASE_STORE_PASSWORD / IB_RELEASE_KEY_ALIAS / IB_RELEASE_KEY_PASSWORD " +
                        "네 환경변수를 모두 설정한 뒤 다시 시도하세요. 새 키를 임의로 만들지 않습니다 — " +
                        "정해진 회사 Release 키를 준비해야 합니다."
                )
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// 출력 APK 파일명을 기본값(app-debug.apk 등)이 아니라 "앱이름-날짜_시간.apk"로 바꾼다(사용자
// 요청). 앱 라벨은 한글("신트리도서관")을 쓰다가, GitHub 릴리스 업로드 시 한글 파일명이 깨져
// 엉뚱한 이름의 자산이 함께 올라가는 문제(2026-08-12)가 있어 영문 "shintree"로 바꿨다 —
// strings.xml의 app_name(사용자에게 보이는 화면 표시 이름)은 그대로 "신트리도서관"이며 이
// 빌드 파일명과는 무관하다. versionName은 "1.0-YYYYMMDD.HHmm" 고정 형식(배포 절차 참고)이라
// "1.0-" 접두사를 떼고 "."을 "_"로 바꿔 "YYYYMMDD_HHmm"만 남긴다.
androidComponents {
    val appLabel = "shintree"
    onVariants { variant ->
        variant.outputs.forEach { output ->
            if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                val versionName = output.versionName.get()
                val dateStamp = versionName.substringAfter('-').replace('.', '_')
                output.outputFileName.set("${appLabel}-${dateStamp}.apk")
            }
        }
    }
}

dependencies {
    implementation(libs.temi.sdk)
    implementation(libs.coil.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}