package com.example.ibtech.data.repository

import android.content.Context
import com.example.ibtech.R
import com.example.ibtech.domain.model.UsageTopic

/**
 * 이용방법 안내 최초 시드 콘텐츠 (요구사항 로드맵 2.1절 "관리자 설정 또는 로컬 데이터 파일에서
 * 변경 가능" — 코드에 문구를 직접 박지 않고 `strings.xml`에서 읽어 [UsageTopic]으로 구성한다).
 *
 * "신트리도서관"은 실제 운영 데이터가 없는 예시 도서관이라, 대출 권수·정확한 운영시간처럼
 * 지어내면 사실처럼 보이는 수치는 넣지 않는다 — 답변은 "정확한 내용은 안내데스크에 문의해
 * 주세요"로 안내하고, 실제 값은 10단계 관리자 화면에서 채운다. 같은 이유로 [UsageTopic.qrUrl]/
 * [UsageTopic.relatedFacilityId]도 실존하지 않는 QR/시설을 지어내지 않기 위해 전부 null로 둔다.
 */
object DefaultUsageContent {

    fun build(context: Context): List<UsageTopic> {
        fun s(resId: Int) = context.getString(resId)

        return listOf(
            UsageTopic(id = "category_loan", parentId = null, title = s(R.string.usage_category_loan), sortOrder = 0),
            UsageTopic(
                id = "loan_count",
                parentId = "category_loan",
                title = s(R.string.usage_topic_loan_count_title),
                shortAnswer = s(R.string.usage_topic_loan_count_answer),
                sortOrder = 0
            ),
            UsageTopic(
                id = "loan_period",
                parentId = "category_loan",
                title = s(R.string.usage_topic_loan_period_title),
                shortAnswer = s(R.string.usage_topic_loan_period_answer),
                sortOrder = 1
            ),
            UsageTopic(
                id = "loan_extend",
                parentId = "category_loan",
                title = s(R.string.usage_topic_loan_extend_title),
                shortAnswer = s(R.string.usage_topic_loan_extend_answer),
                sortOrder = 2
            ),
            UsageTopic(
                id = "loan_return_kiosk",
                parentId = "category_loan",
                title = s(R.string.usage_topic_loan_return_kiosk_title),
                shortAnswer = s(R.string.usage_topic_loan_return_kiosk_answer),
                sortOrder = 3
            ),

            UsageTopic(id = "category_membership", parentId = null, title = s(R.string.usage_category_membership), sortOrder = 1),
            UsageTopic(
                id = "membership_guide",
                parentId = "category_membership",
                title = s(R.string.usage_topic_membership_title),
                shortAnswer = s(R.string.usage_topic_membership_answer),
                sortOrder = 0
            ),

            UsageTopic(id = "category_reading_room", parentId = null, title = s(R.string.usage_category_reading_room), sortOrder = 2),
            UsageTopic(
                id = "reading_room_guide",
                parentId = "category_reading_room",
                title = s(R.string.usage_topic_reading_room_title),
                shortAnswer = s(R.string.usage_topic_reading_room_answer),
                sortOrder = 0
            ),

            UsageTopic(id = "category_hours", parentId = null, title = s(R.string.usage_category_hours), sortOrder = 3),
            UsageTopic(
                id = "hours_guide",
                parentId = "category_hours",
                title = s(R.string.usage_topic_hours_title),
                shortAnswer = s(R.string.usage_topic_hours_answer),
                sortOrder = 0
            )
        )
    }
}
