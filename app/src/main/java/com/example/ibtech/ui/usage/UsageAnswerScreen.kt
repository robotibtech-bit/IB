package com.example.ibtech.ui.usage

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ibtech.R
import com.example.ibtech.domain.model.UsageTopic
import com.example.ibtech.ui.common.DecorativeBackground
import com.example.ibtech.ui.common.EmptyState
import com.example.ibtech.ui.common.InfoDialog
import com.example.ibtech.ui.common.LibraryCard
import com.example.ibtech.ui.common.LibraryOutlinedButton
import com.example.ibtech.ui.common.LibraryPrimaryButton
import com.example.ibtech.ui.theme.IBTECHTheme
import com.example.ibtech.ui.theme.LibraryDimens

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
                    .verticalScroll(rememberScrollState())
                    .padding(LibraryDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
            ) {
                // 13단계: 제목·표·본문을 흰색 상세 정보 카드 하나에 담아 정돈한다. tableData 파싱과
                // UsageDataTable 렌더링 로직 자체는 그대로다 — 카드로 감싸는 배치만 바뀐다.
                LibraryCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(LibraryDimens.CardPadding),
                        verticalArrangement = Arrangement.spacedBy(LibraryDimens.CardSpacing)
                    ) {
                        Text(
                            text = topic.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        if (!topic.tableData.isNullOrBlank()) {
                            UsageDataTable(tableData = topic.tableData)
                        }

                        if (!topic.shortAnswer.isNullOrBlank()) {
                            Text(
                                text = topic.shortAnswer,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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

                LibraryPrimaryButton(
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

// 표 칸 글자는 화면 전역의 확대 타이포그래피(본문 44sp)를 그대로 쓰면 열이 몇 개만 있어도
// 화면 폭을 넘어서므로, 표에서만 쓰는 고정 크기를 따로 둔다.
private val TableHeaderStyle = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
private val TableBodyStyle = TextStyle(fontSize = 22.sp, lineHeight = 28.sp)
private val TableMinColumnWidth = 190.dp

/**
 * [com.example.ibtech.domain.model.UsageTopic.tableData]를 표로 그린다.
 * 형식: 줄마다 한 행, 칸은 `|`로 구분, 첫 줄이 헤더. 열 수가 많아 화면 폭을 넘으면
 * 가로 스크롤로 빠져나가게 해 잘리는 대신 옆으로 밀리게 한다(12단계에서 반복 확인한
 * "고정 높이/폭에 큰 글자를 넣으면 잘린다" 문제를 표에서도 같은 방식으로 방지).
 */
@Composable
private fun UsageDataTable(tableData: String, modifier: Modifier = Modifier) {
    val rows = remember(tableData) {
        tableData.trim().lines()
            .filter { it.isNotBlank() }
            .map { line -> line.split("|").map(String::trim) }
    }
    if (rows.size < 2) return

    val columnCount = rows.first().size
    val header = rows.first()
    val body = rows.drop(1).map { row ->
        if (row.size >= columnCount) row.take(columnCount) else row + List(columnCount - row.size) { "" }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnWidth = maxOf(TableMinColumnWidth, maxWidth / columnCount)

        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            Row(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
                header.forEach { cell ->
                    TableCell(text = cell, width = columnWidth, style = TableHeaderStyle, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            body.forEachIndexed { index, row ->
                val rowColor = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                Row(modifier = Modifier.background(rowColor)) {
                    row.forEach { cell ->
                        TableCell(text = cell, width = columnWidth, style = TableBodyStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(text: String, width: Dp, style: TextStyle, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .width(width)
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, style = style, color = color)
    }
}

private fun previewAnswerUiState() = UsageAnswerUiState(
    isLoaded = true,
    topic = UsageTopic(
        id = "hours_operating",
        parentId = "category_hours",
        title = "시설별 운영시간",
        shortAnswer = "요일과 자료실에 따라 운영시간이 다릅니다. 방문 전 아래 표를 확인해 주세요.",
        tableData = "시설|평일 (화~금)|주말 (토,일)|위치\n종합자료실|09:00~20:00|09:00~17:00|3층\n어린이자료실|09:00~18:00|09:00~17:00|1층"
    )
)

@Preview(name = "이용방법 답변(표 포함) · 1280x720", widthDp = 1280, heightDp = 720, showBackground = true)
@Composable
private fun UsageAnswerScreenPreview() {
    IBTECHTheme {
        UsageAnswerScreen(
            uiState = previewAnswerUiState(),
            onRelatedFacilityClick = {},
            onStaffHelpClick = {},
            onDismissStaffHelp = {},
            onGoHome = {}
        )
    }
}
