package com.example.ibtech.ui.booksearch

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * 검색 결과 카드 한 장.
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
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(onClick)),
        accentColor = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = hit.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (hit.author.isNotBlank()) {
                Text(
                    text = hit.author,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hit.callNo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = hit.shelfSummary(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (hit.sameTitleCount > 1) {
                    Text(
                        text = stringResource(
                            R.string.book_search_same_title_count,
                            hit.sameTitleCount
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
