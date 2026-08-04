package com.example.ibtech.ui.kids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ibtech.domain.model.RecommendedBook
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 추천도서 카드 (요구사항 명세서 2.13/2.14절 공용) — 퀴즈 결과 화면과 추천도서 화면이 함께
 * 쓴다. 표지 이미지 자산이 없어 [RecommendedBook.coverPath] 유무와 무관하게 책 아이콘으로
 * 대신한다(관리자가 실제 표지를 등록하는 기능은 10단계).
 */
@Composable
fun RecommendedBookCard(book: RecommendedBook, modifier: Modifier = Modifier) {
    LibraryCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (book.description.isNotBlank()) {
                Text(
                    text = book.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!book.locationText.isNullOrBlank()) {
                Text(
                    text = book.locationText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
