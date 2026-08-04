package com.example.ibtech.ui.usage

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.ibtech.R
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.InfoDialog
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.debounced
import com.example.ibtech.ui.theme.LibraryDimens

private const val COLLAPSED_MAX_LINES = 2

/** 이용방법 답변 화면 (요구사항 명세서 2.9절). */
@Composable
fun UsageAnswerScreen(
    uiState: UsageAnswerUiState,
    onRelatedFacilityClick: (String) -> Unit,
    onStaffHelpClick: () -> Unit,
    onDismissStaffHelp: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topic = uiState.topic

    when {
        !uiState.isLoaded -> Unit

        topic == null -> EmptyState(
            message = stringResource(R.string.usage_invalid_content),
            actionLabel = stringResource(R.string.top_bar_home),
            onAction = onGoHome,
            modifier = modifier.fillMaxSize()
        )

        else -> Box(modifier = modifier.fillMaxSize()) {
            DecorativeBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
            ) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (!topic.shortAnswer.isNullOrBlank()) {
                    ExpandableAnswer(text = topic.shortAnswer)
                }

                if (!topic.qrUrl.isNullOrBlank()) {
                    QrButton(url = topic.qrUrl)
                }

                val relatedFacility = uiState.relatedFacility
                if (relatedFacility != null) {
                    LibraryOutlinedButton(
                        text = stringResource(R.string.usage_answer_related_facility_action),
                        icon = Icons.Filled.Place,
                        onClick = { onRelatedFacilityClick(relatedFacility.id) }
                    )
                }

                LibraryOutlinedButton(
                    text = stringResource(R.string.usage_answer_staff_help_action),
                    icon = Icons.Filled.SupportAgent,
                    onClick = onStaffHelpClick
                )
            }

            if (uiState.showStaffHelp) {
                InfoDialog(
                    title = stringResource(R.string.usage_staff_help_title),
                    body = stringResource(R.string.usage_staff_help_body),
                    dismissLabel = stringResource(R.string.usage_staff_help_dismiss),
                    onDismiss = onDismissStaffHelp
                )
            }
        }
    }
}

/** "더 보기"/"접기"로 확장하는 답변 텍스트(요구사항 2.9절 "긴 답변은 더 보기로 확장"). */
@Composable
private fun ExpandableAnswer(text: String) {
    var expanded by remember { mutableStateOf(false) }
    var isOverflowing by remember { mutableStateOf(false) }

    Column {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) isOverflowing = result.hasVisualOverflow
            }
        )
        if (isOverflowing || expanded) {
            Text(
                text = stringResource(if (expanded) R.string.usage_answer_less else R.string.usage_answer_more),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = debounced { expanded = !expanded })
            )
        }
    }
}

@Composable
private fun QrButton(url: String) {
    val context = LocalContext.current
    val errorMessage = stringResource(R.string.usage_answer_qr_error)
    LibraryOutlinedButton(
        text = stringResource(R.string.usage_answer_qr_action),
        icon = Icons.Filled.QrCode,
        onClick = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }.onFailure {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    )
}
