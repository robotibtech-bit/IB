package com.example.ibtech.ui.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ibtech.R
import com.example.ibtech.robot.FakeOutcome
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.theme.LibraryDimens

/**
 * 개발자 메뉴 (`BuildConfig.DEBUG` 전용, 로드맵 5단계).
 *
 * 실기 없이도 동행 흐름의 성공/실패/외부중단 3가지를 재현할 수 있게 [TemiControllerProvider]
 * 의 Fake 전환과 `FakeTemiController.nextOutcome`을 여기서 바꾼다.
 */
@Composable
fun DevMenuScreen(
    uiState: DevMenuUiState,
    onToggleFake: (Boolean) -> Unit,
    onSelectOutcome: (FakeOutcome) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DecorativeBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LibraryDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dev_menu_use_fake),
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(checked = uiState.useFake, onCheckedChange = onToggleFake)
            }

            Text(
                text = stringResource(R.string.dev_menu_outcome_label),
                style = MaterialTheme.typography.titleMedium
            )

            OutcomeOption(
                label = stringResource(R.string.dev_menu_outcome_success),
                selected = uiState.nextOutcome == FakeOutcome.SUCCESS,
                onClick = { onSelectOutcome(FakeOutcome.SUCCESS) }
            )
            OutcomeOption(
                label = stringResource(R.string.dev_menu_outcome_failed),
                selected = uiState.nextOutcome == FakeOutcome.FAILED,
                onClick = { onSelectOutcome(FakeOutcome.FAILED) }
            )
            OutcomeOption(
                label = stringResource(R.string.dev_menu_outcome_external),
                selected = uiState.nextOutcome == FakeOutcome.EXTERNAL_INTERRUPTION,
                onClick = { onSelectOutcome(FakeOutcome.EXTERNAL_INTERRUPTION) }
            )
        }
    }
}

@Composable
private fun OutcomeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
