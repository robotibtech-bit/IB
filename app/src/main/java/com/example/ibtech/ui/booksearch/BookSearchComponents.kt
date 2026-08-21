package com.example.ibtech.ui.booksearch

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
 * 표지는 검색 응답(`/search`)에 없고 상세 API(`/books/{id}`,
 * [com.example.ibtech.domain.model.BookDetail.thumbnail])에만 있다. 검색 결과 전부를
 * 한꺼번에 상세 조회하면 지연·외부 API 쿼터가 커지므로, 카드가 실제로 화면에 나타날 때만
 * [onAppear]로 상위(ViewModel)에 표지 조회를 요청한다 — [thumbnail]이 아직 없으면 도착
 * 전까지 아이콘 플레이스홀더를 보여 준다.
 *
 * 저자를 보여주지 않는 이유 — 도서관에서 받은 장서 데이터에 저자 열이 없다. 나중에 채워지면
 * [BookHit.author]가 비어 있지 않게 되므로 그때 표시하면 된다.
 */
@Composable
fun BookHitCard(
    hit: BookHit,
    thumbnail: String?,
    onAppear: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(hit.bookId) { onAppear() }

    LibraryCard(
        modifier = modifier.clickable(onClick = debounced(onClick)),
        accentColor = MaterialTheme.colorScheme.primary
    ) {
        // 카드 크기를 전부 동일하게 맞추려고(요청: "카드크기 전부 동일하게") 전체를 고정
        // 높이로 감싼다. 그리고 기본 titleMedium/bodyMedium를 그대로 쓰지 않는다 — 이
        // 앱의 kiosk 타이포그래피(titleMedium 58sp, bodyMedium 54sp 줄간격, 몇 걸음
        // 떨어져서도 읽히게 키운 값, Type.kt 참고)로 5줄을 다 쓰면 카드 하나가 600dp를
        // 넘어간다. 시설 목록 카드([FacilityListScreen.FacilityCard])가 같은 문제를 이미
        // 겪어서(주석: "labelLarge를 그대로 쓰면 ... 카드 아래쪽이 잘려 보였다") 부제 줄에
        // 훨씬 작은 fontSize를 직접 지정해 해결한 것과 같은 방식을 쓴다.
        val cardTitleStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 30.sp, lineHeight = 36.sp)
        val cardBodyStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 24.sp, lineHeight = 30.sp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding)
                .height(BOOK_CARD_CONTENT_HEIGHT),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BookCover(title = hit.title, thumbnail = thumbnail)

            Text(
                text = hit.title,
                style = cardTitleStyle,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 요청: "책제목이랑 밑에글까지 공백 한줄만 줄여볼래" — hit.author는 장서
            // 데이터에 저자 열이 없어 지금은 항상 비어 있고, 그 빈 줄이 제목 밑에 공백처럼
            // 보였다. 나중에 저자 데이터가 채워지면 그때 다시 넣으면 된다.
            Text(
                text = hit.callNo,
                style = cardBodyStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = hit.shelfSummary(),
                style = cardBodyStyle,
                color = MaterialTheme.colorScheme.primary,
                minLines = 1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (hit.sameTitleCount > 1) {
                    stringResource(R.string.book_search_same_title_count, hit.sameTitleCount)
                } else {
                    ""
                },
                style = cardBodyStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 표지 자리. [thumbnail]이 도착하기 전(또는 서점에도 표지가 없는 책)에는 책 아이콘으로
 * 빈 칸처럼 보이지 않게 채운다. 요청("책이미지 크기 살짝 줄일까")에 따라 폭 비율(aspectRatio)
 * 대신 고정 높이를 쓴다 — [BOOK_CARD_CONTENT_HEIGHT]와 짝을 맞춰 카드 전체 높이를
 * 예측 가능하게 유지하기 위해서다. */
@Composable
private fun BookCover(title: String, thumbnail: String?) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BOOK_COVER_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail.isNullOrBlank()) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(0.4f).aspectRatio(1f)
            )
        } else {
            AsyncImage(
                model = thumbnail,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(BOOK_COVER_HEIGHT)
            )
        }
    }
}

/** 카드 표지 높이. 기존 aspectRatio(3:4) 기준 값보다 살짝 줄였다. */
private val BOOK_COVER_HEIGHT = 190.dp

/** 카드 안쪽(표지+텍스트 4줄) 고정 높이 — CardPadding을 뺀 순수 콘텐츠 영역 기준이다.
 * 표지(190) + 제목 2줄(36dp×2) + 나머지 3줄(30dp×3) + 줄 사이 간격(spacedBy 10dp × 4) +
 * 여유값. 실제 lineHeight 기준(190+72+90+40=392)보다 넉넉히 잡아 클리핑을 막는다. */
private val BOOK_CARD_CONTENT_HEIGHT = 420.dp

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
