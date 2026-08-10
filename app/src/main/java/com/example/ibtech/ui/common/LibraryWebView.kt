package com.example.ibtech.ui.common

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ibtech.R

/**
 * 행사 QR/URL처럼 관리자가 입력한 외부 링크를 키오스크 앱 밖으로 나가지 않고 보여준다.
 *
 * 기존에는 `Intent.ACTION_VIEW`로 외부 브라우저를 띄웠는데, 그러면 단일 액티비티 키오스크
 * 구조(무입력 자동복귀, 뒤로가기 정책)를 벗어난다 — 이 화면 안에서 계속 보여주기 위해
 * [WebView]를 직접 얹는다.
 */
@Composable
fun LibraryWebView(
    url: String,
    modifier: Modifier = Modifier,
    onWebViewReady: (WebView) -> Unit = {},
    onCanGoBackChanged: (Boolean) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, pageUrl: String?) {
                            isLoading = false
                            onCanGoBackChanged(view.canGoBack())
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError
                        ) {
                            if (request.isForMainFrame) {
                                isLoading = false
                                loadError = true
                            }
                        }
                    }
                    onWebViewReady(this)
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (loadError) {
            EmptyState(
                message = stringResource(R.string.web_view_load_error),
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}
