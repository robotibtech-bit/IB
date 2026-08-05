package com.example.ibtech.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ibtech.domain.model.RecommendedBook
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.SkyAccent
import com.example.ibtech.ui.theme.SkyAccentContainer

/**
 * 추천도서 카드 (요구사항 명세서 2.13/2.14절 공용) — 퀴즈 결과 화면과 추천도서 화면이 함께
 * 쓴다. 표지 이미지 자산이 없어 [RecommendedBook.coverPath] 유무와 무관하게 책 아이콘으로
 * 대신한다(관리자가 실제 표지를 등록하는 기능은 10단계).
 *
 * 어린이 콘텐츠 팔레트(Sky="나에게 맞는 책")를 그대로 쓴다. 관리자가 입력하는 제목·설명은
 * 길이 제한이 없어 maxLines+Ellipsis로 카드 높이가 예측 불가능하게 늘어나지 않게 막는다.
 */
@Composable
fun RecommendedBookCard(book: RecommendedBook, modifier: Modifier = Modifier) {
    LibraryCard(modifier = modifier.fillMaxWidth(), accentColor = SkyAccent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(SkyAccentContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = SkyAccent
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.description.isNotBlank()) {
                    Text(
                        text = book.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!book.locationText.isNullOrBlank()) {
                    Text(
                        text = book.locationText,
                        style = MaterialTheme.typography.labelLarge,
                        color = SkyAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
