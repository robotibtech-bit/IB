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

/** 세로/가로 배율 스크립트 적용 후 화면을 보여주기까지 기다리는 시간. 프레임 한두 번으로는
 * 웹뷰 내부 렌더러가 새 배율을 실제로 그려 넘기기 전에 화면이 보이는 경우가 있어, 눈에 띄는
 * 지연을 감수하고 넉넉히 잡아 깜빡임을 확실히 없앤다. */
private const val ADJUSTMENT_REVEAL_DELAY_MS = 400L

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
    /** 페이지 내용을 세로로만 늘려 보여줄 배율(1이면 원본 그대로). 가로로 넓게 설계된 페이지가
     * 가로로 긴 키오스크 화면에서 위아래로 짧고 빈약해 보일 때 쓴다 — 실제 레이아웃을 다시 짜는
     * 게 아니라 렌더링된 화면만 CSS `transform: scaleY()`로 시각적으로 늘린다. */
    verticalScale: Float = 1f,
    /** true면 고정 폭으로 만들어진 옛날 페이지가 넓은 키오스크 화면 오른쪽에 여백을 크게
     * 남기는 문제를 고친다 — `body`의 내용을 감싸 실제 콘텐츠 폭을 측정한 뒤, 화면 가로폭의
     * [horizontalFitWidthFraction]만큼 차지하도록 가로로만 늘리고 좌우 여백을 똑같이 남겨
     * 가운데로 옮긴다. 페이지 내부 구조(태그 id 등)를 몰라도 되게 자동으로 감싼다. */
    horizontalFit: Boolean = false,
    horizontalFitWidthFraction: Float = 0.8f
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    val needsAdjustment = verticalScale != 1f || horizontalFit

    fun buildAdjustmentScript(): String = buildString {
        append("(function() {")
        if (verticalScale != 1f) {
            append(
                """
                document.body.style.transformOrigin = 'top';
                document.body.style.transform = 'scaleY($verticalScale)';
                """.trimIndent()
            )
        }
        if (horizontalFit) {
            append(
                """
                var wrap = document.getElementById('__ibtechFitWrap');
                if (!wrap) {
                    wrap = document.createElement('div');
                    wrap.id = '__ibtechFitWrap';
                    wrap.style.display = 'inline-block';
                    while (document.body.firstChild) {
                        wrap.appendChild(document.body.firstChild);
                    }
                    document.body.appendChild(wrap);
                    document.body.style.margin = '0';
                }
                var vw = window.innerWidth;
                var natW = wrap.getBoundingClientRect().width;
                if (natW > 0) {
                    var scale = (vw * $horizontalFitWidthFraction) / natW;
                    wrap.style.transformOrigin = 'top left';
                    wrap.style.transform = 'scale(' + scale + ', 1)';
                    wrap.style.marginLeft = (vw * (1 - $horizontalFitWidthFraction) / 2) + 'px';
                }
                """.trimIndent()
            )
        }
        append("})();")
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    if (needsAdjustment) alpha = 0f

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    // 이 페이지들은 첫 로딩이 끝난 직후 자기 자신의 스크립트로 다시 한번 조용히
                    // 다시 불러오는 경우가 있다(예: 좌석현황 30초 자동새로고침, 로그인 방식 자동
                    // 전환 폼 재제출). 그때마다 onPageStarted/onPageFinished가 다시 불리는데, 처음
                    // 한 번만 가리고 보여주면 이후 재로딩에서는 배율이 안 잡힌 원본이 그대로 보였다가
                    // 다시 배율이 적용되는 깜빡임이 남는다 — 그래서 "다시 불러오기 시작"마다 매번
                    // 새로 가리고, 배율까지 다 적용된 뒤에만 다시 보여준다.
                    var pendingReveal: Runnable? = null

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                            if (!needsAdjustment) return
                            pendingReveal?.let { view.removeCallbacks(it) }
                            isLoading = true
                            view.alpha = 0f
                        }

                        override fun onPageFinished(view: WebView, pageUrl: String?) {
                            if (!needsAdjustment) {
                                isLoading = false
                                return
                            }
                            view.evaluateJavascript(buildAdjustmentScript()) {
                                // evaluateJavascript의 콜백은 스크립트 실행이 "끝났다"는 뜻일 뿐,
                                // 웹뷰 내부 렌더러가 그 결과를 실제 화면에 그려 넘기는 시점은 별도라
                                // 프레임 한두 번 넘기는 정도로는 깜빡임이 완전히 없어지지 않았다.
                                // 대신 로딩 화면을 넉넉히 더 붙잡아 둔다 — 살짝 느려지더라도
                                // 확실하게 깜빡임 없이 보여주는 쪽을 택한다.
                                val reveal = Runnable {
                                    isLoading = false
                                    view.alpha = 1f
                                }
                                pendingReveal = reveal
                                view.postDelayed(reveal, ADJUSTMENT_REVEAL_DELAY_MS)
                            }
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError
                        ) {
                            if (request.isForMainFrame) {
                                pendingReveal?.let { view.removeCallbacks(it) }
                                isLoading = false
                                loadError = true
                                view.alpha = 1f
                            }
                        }
                    }
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
