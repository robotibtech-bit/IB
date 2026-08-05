package com.example.ibtech.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ibtech.domain.model.LibraryEtiquetteTip
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.theme.LibraryDimens
import com.example.ibtech.ui.theme.YellowAccent
import com.example.ibtech.ui.theme.YellowAccentContainer

/** 도서관 예절 화면 (요구사항 명세서 2.15절) — 행동 중심의 짧은 카드 목록. */
@Composable
fun LibraryEtiquetteScreen(
    uiState: LibraryEtiquetteUiState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        if (uiState.isLoaded) {
            LazyColumn(
                contentPadding = PaddingValues(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.tips, key = { it.id }) { tip ->
                    EtiquetteCard(tip)
                }
            }
        }
    }
}

// 도서관 예절 = Yellow(어린이 콘텐츠 팔레트, KidsMenuScreen과 동일). 금지/권장을 아이콘으로
// 구분하는 표시는 LibraryEtiquetteTip에 그런 필드가 없어(자유 문구 하나뿐) 임의로 만들지
// 않았다 — 도메인 모델 변경 없이는 정확히 표현할 수 없는 항목이라 색·아이콘 정돈만 적용했다.
@Composable
private fun EtiquetteCard(tip: LibraryEtiquetteTip) {
    LibraryCard(modifier = Modifier.fillMaxWidth(), accentColor = YellowAccent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(YellowAccentContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiPeople,
                    contentDescription = null,
                    tint = YellowAccent
                )
            }
            Text(
                text = tip.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
