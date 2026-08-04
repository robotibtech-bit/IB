package com.example.ibtech.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens

/** 어린이 콘텐츠 메뉴 화면 (요구사항 명세서 2.10절): 퀴즈/추천도서/예절 3개 진입점. */
@Composable
fun KidsMenuScreen(
    onQuizClick: () -> Unit,
    onBooksClick: () -> Unit,
    onEtiquetteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 220.dp),
            contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
            horizontalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                KidsMenuCard(
                    icon = Icons.Filled.Quiz,
                    label = stringResource(R.string.kids_menu_action_quiz),
                    onClick = onQuizClick
                )
            }
            item {
                KidsMenuCard(
                    icon = Icons.Filled.AutoStories,
                    label = stringResource(R.string.kids_menu_action_books),
                    onClick = onBooksClick
                )
            }
            item {
                KidsMenuCard(
                    icon = Icons.Filled.EmojiPeople,
                    label = stringResource(R.string.kids_menu_action_etiquette),
                    onClick = onEtiquetteClick
                )
            }
        }
    }
}

@Composable
private fun KidsMenuCard(icon: ImageVector, label: String, onClick: () -> Unit) {
    LibraryCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(onClick))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
