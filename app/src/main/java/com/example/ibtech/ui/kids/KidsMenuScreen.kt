package com.example.ibtech.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.FillSpaceGrid
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens

private data class KidsMenuItem(val icon: ImageVector, val label: String, val onClick: () -> Unit)

/**
 * 어린이 콘텐츠 메뉴 화면 (요구사항 명세서 2.10절): 퀴즈/추천도서/예절 3개 진입점.
 *
 * 12단계: 고정 3개 항목을 여백 없이 화면을 채우는 큰 카드로 가로 배치한다.
 */
@Composable
fun KidsMenuScreen(
    onQuizClick: () -> Unit,
    onBooksClick: () -> Unit,
    onEtiquetteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        KidsMenuItem(Icons.Filled.Quiz, stringResource(R.string.kids_menu_action_quiz), onQuizClick),
        KidsMenuItem(Icons.Filled.AutoStories, stringResource(R.string.kids_menu_action_books), onBooksClick),
        KidsMenuItem(Icons.Filled.EmojiPeople, stringResource(R.string.kids_menu_action_etiquette), onEtiquetteClick)
    )

    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        FillSpaceGrid(
            items = items,
            columns = items.size,
            modifier = Modifier
                .fillMaxSize()
                .padding(LibraryDimens.ScreenPadding)
        ) { item, cardModifier ->
            KidsMenuCard(icon = item.icon, label = item.label, onClick = item.onClick, modifier = cardModifier)
        }
    }
}

@Composable
private fun KidsMenuCard(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LibraryCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(onClick))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LibraryDimens.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(LibraryDimens.LargeIconCircle)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(LibraryDimens.LargeIconSize)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
