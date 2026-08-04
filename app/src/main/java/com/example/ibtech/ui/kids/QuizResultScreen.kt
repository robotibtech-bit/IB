package com.example.ibtech.ui.kids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.theme.LibraryDimens

/** 퀴즈 결과 화면 (요구사항 명세서 2.13절). */
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
        ) {
            Text(
                text = stringResource(
                    R.string.quiz_result_summary_format,
                    uiState.totalCount,
                    uiState.correctCount
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (uiState.recommendedBooks.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.quiz_result_books_heading),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                uiState.recommendedBooks.forEach { book ->
                    RecommendedBookCard(book = book)
                }
            }

            val facility = uiState.childrenFacility
            if (facility != null) {
                LibraryPrimaryButton(
                    text = stringResource(R.string.kids_go_facility_action),
                    onClick = { onGoToChildrenFacility(facility.id) }
                )
            }

            LibraryOutlinedButton(
                text = stringResource(R.string.quiz_result_retry),
                onClick = onRetryQuiz
            )
        }
    }
}
