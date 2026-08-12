package com.example.ibtech.data.repository

import android.content.Context
import com.example.ibtech.R
import com.example.ibtech.domain.model.UsageTopic

/**
 * 이용방법 안내 최초 시드 콘텐츠 (요구사항 로드맵 2.1절 "관리자 설정 또는 로컬 데이터 파일에서
 * 변경 가능" — 코드에 문구를 직접 박지 않고 `strings.xml`에서 읽어 [UsageTopic]으로 구성한다).
 *
 * 사용자가 제공한 "신트리도서관 종합 이용안내" PDF(개관시간/회원가입/대출·반납/자료실/열람실
 * 5개 장)를 그대로 옮긴 실제 운영 데이터다. 표 형태 정보([UsageTopic.tableData])는 PDF에서
 * 표로 제시된 항목(운영시간, 가입 서류, 대출 규정)에만 채우고, 그 외는 안내문 형태로 둔다.
 * [UsageTopic.qrUrl]/[UsageTopic.relatedFacilityId]는 PDF에 실제 QR·연계 시설 정보가 없어
 * 지어내지 않고 null로 둔다.
 *
 * 참고 프로젝트(TemiRobotApp, 12단계에서 UI 비교에 썼던 그 앱)의 이용방법 안내가 "카테고리
 * 4개 × 세부 항목 4개"로 균일하게 나뉘어 있어, 이 구조를 그대로 따른다. PDF 5개 장 중
 * "자료실 상세 안내"는 별도 카테고리로 두지 않고 "열람실 이용"과 합쳐 카테고리 4개를 유지했다
 * (참고 프로젝트에는 없던 실제 자료실 데이터를 자리표시자 항목 늘리기 없이 편입하기 위함).
 */
object DefaultUsageContent {

    /** 신트리도서관 일반열람실 실시간 좌석 현황(ezlib). 도서관 자체 시스템 링크라 PDF에는 없다. */
    const val READING_SEAT_STATUS_URL = "http://seat8.ice.go.kr/EZ5500/SEAT/RoomStatus.aspx"
    const val READING_SEAT_STATUS_TOPIC_ID = "reading_seat_status"

    /** 디지털자료실(PC) 예약현황 페이지(LibMate). "실시간 좌석 · 예약현황" 메뉴 명세에 지정된
     * 주소를 그대로 쓴다 — 임의의 예약 URL을 추측해 넣지 않는다. */
    const val DIGITAL_ROOM_RESERVATION_STATUS_URL = "http://digital8.ice.go.kr:8800/LibMate/LibMate.php"

    /** [ensureReadingSeatStatusTopic][com.example.ibtech.data.repository.UsageRepository.ensureReadingSeatStatusTopic]의
     * 1회성 마이그레이션과 [build] 양쪽에서 같은 항목을 쓰기 위해 따로 뺀다. */
    fun buildReadingSeatStatusTopic(context: Context) = UsageTopic(
        id = READING_SEAT_STATUS_TOPIC_ID,
        parentId = "category_reading_materials",
        title = context.getString(R.string.usage_topic_reading_seat_status_title),
        shortAnswer = context.getString(R.string.usage_topic_reading_seat_status_answer),
        qrUrl = READING_SEAT_STATUS_URL,
        sortOrder = 0
    )

    fun build(context: Context): List<UsageTopic> {
        fun s(resId: Int) = context.getString(resId)

        return listOf(
            UsageTopic(id = "category_loan", parentId = null, title = s(R.string.usage_category_loan), sortOrder = 0),
            UsageTopic(
                id = "loan_period",
                parentId = "category_loan",
                title = s(R.string.usage_topic_loan_period_title),
                shortAnswer = s(R.string.usage_topic_loan_period_answer),
                tableData = s(R.string.usage_topic_loan_period_table),
                sortOrder = 0
            ),
            UsageTopic(
                id = "loan_extend",
                parentId = "category_loan",
                title = s(R.string.usage_topic_loan_extend_title),
                shortAnswer = s(R.string.usage_topic_loan_extend_answer),
                tableData = s(R.string.usage_topic_loan_extend_table),
                sortOrder = 1
            ),
            UsageTopic(
                id = "loan_return_kiosk",
                parentId = "category_loan",
                title = s(R.string.usage_topic_loan_return_kiosk_title),
                shortAnswer = s(R.string.usage_topic_loan_return_kiosk_answer),
                sortOrder = 2
            ),
            UsageTopic(
                id = "loan_count",
                parentId = "category_loan",
                title = s(R.string.usage_topic_loan_count_title),
                shortAnswer = s(R.string.usage_topic_loan_count_answer),
                sortOrder = 3
            ),

            UsageTopic(id = "category_membership", parentId = null, title = s(R.string.usage_category_membership), sortOrder = 1),
            UsageTopic(
                id = "membership_target",
                parentId = "category_membership",
                title = s(R.string.usage_topic_membership_target_title),
                shortAnswer = s(R.string.usage_topic_membership_target_answer),
                sortOrder = 0
            ),
            UsageTopic(
                id = "membership_documents",
                parentId = "category_membership",
                title = s(R.string.usage_topic_membership_documents_title),
                shortAnswer = s(R.string.usage_topic_membership_documents_answer),
                tableData = s(R.string.usage_topic_membership_documents_table),
                sortOrder = 1
            ),
            UsageTopic(
                id = "membership_procedure",
                parentId = "category_membership",
                title = s(R.string.usage_topic_membership_procedure_title),
                shortAnswer = s(R.string.usage_topic_membership_procedure_answer),
                sortOrder = 2
            ),
            UsageTopic(
                id = "membership_card",
                parentId = "category_membership",
                title = s(R.string.usage_topic_membership_card_title),
                shortAnswer = s(R.string.usage_topic_membership_card_answer),
                sortOrder = 3
            ),

            UsageTopic(id = "category_reading_materials", parentId = null, title = s(R.string.usage_category_reading_materials), sortOrder = 2),
            buildReadingSeatStatusTopic(context),
            UsageTopic(
                id = "reading_seat",
                parentId = "category_reading_materials",
                title = s(R.string.usage_topic_reading_seat_title),
                shortAnswer = s(R.string.usage_topic_reading_seat_answer),
                sortOrder = 1
            ),
            UsageTopic(
                id = "reading_rules",
                parentId = "category_reading_materials",
                title = s(R.string.usage_topic_reading_rules_title),
                shortAnswer = s(R.string.usage_topic_reading_rules_answer),
                sortOrder = 2
            ),
            UsageTopic(
                id = "materials_general_kids",
                parentId = "category_reading_materials",
                title = s(R.string.usage_topic_materials_general_kids_title),
                shortAnswer = s(R.string.usage_topic_materials_general_kids_answer),
                sortOrder = 3
            ),
            UsageTopic(
                id = "materials_digital",
                parentId = "category_reading_materials",
                title = s(R.string.usage_topic_materials_digital_title),
                shortAnswer = s(R.string.usage_topic_materials_digital_answer),
                sortOrder = 4
            ),

            UsageTopic(id = "category_hours", parentId = null, title = s(R.string.usage_category_hours), sortOrder = 3),
            UsageTopic(
                id = "hours_operating",
                parentId = "category_hours",
                title = s(R.string.usage_topic_hours_operating_title),
                shortAnswer = s(R.string.usage_topic_hours_operating_answer),
                tableData = s(R.string.usage_topic_hours_operating_table),
                sortOrder = 0
            ),
            UsageTopic(
                id = "hours_guide",
                parentId = "category_hours",
                title = s(R.string.usage_topic_hours_title),
                shortAnswer = s(R.string.usage_topic_hours_answer),
                sortOrder = 1
            )
        )
    }
}
