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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.FillSpaceGrid
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LavenderAccent
import com.example.ibtech.ui.theme.LavenderAccentContainer
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.SkyAccent
import com.example.ibtech.ui.theme.SkyAccentContainer
import com.example.ibtech.ui.theme.YellowAccent
import com.example.ibtech.ui.theme.YellowAccentContainer

private data class KidsMenuItem(
    val icon: ImageVector,
    val label: String,
    val accent: Color,
    val iconBackground: Color,
    val onClick: () -> Unit
)

/**
 * 어린이 콘텐츠 메뉴 화면 (요구사항 명세서 2.10절): 퀴즈/추천도서/예절 3개 진입점.
 *
 * 12단계: 고정 3개 항목을 여백 없이 화면을 채우는 큰 카드로 가로 배치한다.
 * 최종 단계(C, 어린이 콘텐츠·행사): 01_DESIGN_SYSTEM.md "열람실·퀴즈 = Lavender" 기준으로
 * 퀴즈=Lavender, 나머지는 남는 팔레트(Sky/Yellow)로 구분한다.
 */
@Composable
fun KidsMenuScreen(
    onQuizClick: () -> Unit,
    onBooksClick: () -> Unit,
    onEtiquetteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        KidsMenuItem(
            icon = Icons.Filled.Quiz,
            label = stringResource(R.string.kids_menu_action_quiz),
            accent = LavenderAccent,
            iconBackground = LavenderAccentContainer,
            onClick = onQuizClick
        ),
        KidsMenuItem(
            icon = Icons.Filled.AutoStories,
            label = stringResource(R.string.kids_menu_action_books),
            accent = SkyAccent,
            iconBackground = SkyAccentContainer,
            onClick = onBooksClick
        ),
        KidsMenuItem(
            icon = Icons.Filled.EmojiPeople,
            label = stringResource(R.string.kids_menu_action_etiquette),
            accent = YellowAccent,
            iconBackground = YellowAccentContainer,
            onClick = onEtiquetteClick
        )
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
            KidsMenuCard(item = item, modifier = cardModifier)
        }
    }
}

@Composable
private fun KidsMenuCard(item: KidsMenuItem, modifier: Modifier = Modifier) {
    LibraryCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = debounced(item.onClick)),
        accentColor = item.accent
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
                    .background(color = item.iconBackground, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.accent,
                    modifier = Modifier.size(LibraryDimens.LargeIconSize)
                )
            }
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
