package com.example.ibtech.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.domain.model.RecommendedBook
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.SuccessAccent
import com.example.ibtech.ui.theme.SuccessAccentContainer

/**
 * 퀴즈 결과 화면 (요구사항 명세서 2.13절).
 *
 * 추천 도서 수가 관리자 설정에 따라 늘어날 수 있어(고정 개수가 아님) [LazyColumn]으로 바꿨다 —
 * 예전 [Column]은 스크롤이 없어 도서가 많으면 화면 아래로 잘렸다.
 */
@Composable
fun QuizResultScreen(
    uiState: QuizResultUiState,
    onGoToChildrenFacility: (String) -> Unit,
    onRetryQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!uiState.isLoaded) return

    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ScoreCard(totalCount = uiState.totalCount, correctCount = uiState.correctCount)
            }

            if (uiState.recommendedBooks.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.quiz_result_books_heading),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(uiState.recommendedBooks, key = { it.id }) { book ->
                    RecommendedBookCard(book = book)
                }
            }

            item {
                val facility = uiState.childrenFacility
                if (facility != null) {
                    LibraryPrimaryButton(
                        text = stringResource(R.string.kids_go_facility_action),
                        onClick = { onGoToChildrenFacility(facility.id) }
                    )
                }
            }

            item {
                LibraryOutlinedButton(
                    text = stringResource(R.string.quiz_result_retry),
                    onClick = onRetryQuiz
                )
            }
        }
    }
}

/** 점수를 Success 색 원형 배지로 강조한다(참고: 02_SCREEN_BLUEPRINT.md "점수 원형/카드"). */
@Composable
private fun ScoreCard(totalCount: Int, correctCount: Int) {
    LibraryCard(modifier = Modifier.fillMaxWidth(), accentColor = SuccessAccent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(SuccessAccentContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$correctCount/$totalCount",
                    style = MaterialTheme.typography.titleLarge,
                    color = SuccessAccent,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = stringResource(R.string.quiz_result_summary_format, totalCount, correctCount),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
