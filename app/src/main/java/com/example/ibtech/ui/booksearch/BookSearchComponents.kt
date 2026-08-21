package com.example.ibtech.ui.booksearch

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.data.booksearch.BookSearchIssue
import com.example.ibtech.domain.model.BookHit
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens

/** 실패 사유 → 표시 문구. 데이터 계층은 사유만 구분하고 문구는 UI 가 매핑한다. */
@StringRes
fun BookSearchIssue.messageRes(): Int = when (this) {
    BookSearchIssue.NOT_CONFIGURED -> R.string.book_search_not_configured
    BookSearchIssue.INVALID_URL -> R.string.book_search_error_invalid_url
    BookSearchIssue.UNREACHABLE -> R.string.book_search_error_unreachable
    BookSearchIssue.SERVER_ERROR -> R.string.book_search_error_server
    BookSearchIssue.MALFORMED_RESPONSE -> R.string.book_search_error_malformed
}

/**
 * 검색 결과 카드 한 장. 그리드에 가로로 나열되는 세로형 카드다(요청: "카드형태로 카드들을
 * 가로로 나열").
 *
 * 표지 자리에 실제 책 이미지 대신 아이콘 플레이스홀더를 쓴다 — 검색 응답(`/search`)에는
 * 표지 URL이 없다. 서버가 표지를 주는 건 사용자가 한 건을 고른 뒤 부르는 상세 API
 * (`/books/{id}`, [com.example.ibtech.domain.model.BookDetail.thumbnail])뿐이라, 검색
 * 결과 10건마다 그 API를 부르면 지연·쿼터 낭비가 커진다(BookSearchApi.detail 문서 참고).
 * 검색 결과 카드에서도 실제 표지를 보여주려면 `/search` 응답 자체에 표지 URL을 포함하도록
 * 서버(`ibLib-server`, 이 저장소 밖) 를 먼저 고쳐야 한다.
 *
 * 저자를 보여주지 않는 이유 — 도서관에서 받은 장서 데이터에 저자 열이 없다. 나중에 채워지면
 * [BookHit.author]가 비어 있지 않게 되므로 그때 표시하면 된다.
 */
@Composable
fun BookHitCard(
    hit: BookHit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LibraryCard(
        modifier = modifier.clickable(onClick = debounced(onClick)),
        accentColor = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BookCoverPlaceholder(title = hit.title)

            Text(
                text = hit.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (hit.author.isNotBlank()) {
                Text(
                    text = hit.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = hit.callNo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = hit.shelfSummary(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (hit.sameTitleCount > 1) {
                Text(
                    text = stringResource(R.string.book_search_same_title_count, hit.sameTitleCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 표지 자리표시 — 실제 표지가 없을 때 빈 칸 대신 책 아이콘 + 첫 글자를 보여줘 카드가
 * "빈 칸처럼" 보이지 않게 한다. [title]의 첫 글자를 색 배지처럼 써서 카드끼리 한눈에
 * 구분되게 돕는다(실제 표지 이미지는 아니다). */
@Composable
private fun BookCoverPlaceholder(title: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(0.4f).aspectRatio(1f)
        )
    }
}

/** 카드에 한 줄로 보여줄 위치 요약. 예: "1층 어린이자료실 · 아동4~1" */
@Composable
private fun BookHit.shelfSummary(): String {
    val shelf = shelf ?: return stringResource(R.string.book_search_shelf_unknown)
    val floorText = shelf.floor?.let { stringResource(R.string.book_search_floor_format, it) }
    return listOfNotNull(
        floorText,
        shelf.room.takeIf { it.isNotBlank() },
        shelf.shelfLabel.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
}
