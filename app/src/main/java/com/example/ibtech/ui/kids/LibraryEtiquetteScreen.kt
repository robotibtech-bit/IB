package com.example.ibtech.ui.kids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ibtech.domain.model.LibraryEtiquetteTip
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.theme.LibraryDimens

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

@Composable
private fun EtiquetteCard(tip: LibraryEtiquetteTip) {
    LibraryCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LibraryDimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiPeople,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = tip.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
