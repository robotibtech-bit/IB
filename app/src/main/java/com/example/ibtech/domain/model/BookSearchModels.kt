package com.example.ibtech.domain.model

/**
 * 도서검색 도메인 모델 (홈 > 책 찾기).
 *
 * 검색·서가 매핑은 전부 외부 서버가 한다. 앱은 발화를 넘기고 결과를 받아 안내만 한다 —
 * 장서 데이터가 CSV 에서 도서관 API 로 바뀌어도 이 모델과 앱은 그대로다.
 */

/**
 * 검색 결과 한 건이 가리키는 서가.
 *
 * [locationName]은 Temi 지도의 POI 이름이라 `goTo()`에 그대로 넘길 수 있다. 다만 로봇은
 * 자기 층 지도만 갖고 있으므로, [floor]가 관리자 설정 기준층과 다르면 직접 갈 수 없다 —
 * 그 판단은 [com.example.ibtech.domain.usecase.ResolveNavigationTargetUseCase]가 한다.
 */
data class BookShelf(
    /** Temi POI 이름. 예: "아동4~1" */
    val locationName: String,
    /** 화면·음성 안내용 표시명. 대개 [locationName]과 같다. */
    val shelfLabel: String,
    /** 자료실명. 예: "어린이자료실". 로봇이 못 가는 서가의 안내 문구에 쓴다. */
    val room: String,
    /** 서가가 있는 층. 서버가 층을 모르면 null 이며, 이때는 동행 안내를 하지 않는다. */
    val floor: Int?,
    /**
     * 매핑표에 구간이 없어 앞 서가로 추정 배정된 위치인지.
     * true 면 안내 문구를 "OO 서가에 있습니다"가 아니라 "OO 서가 근처에 있을 거예요"로 낮춘다.
     */
    val isEstimated: Boolean
)

/** 검색 결과 도서 한 건. */
data class BookHit(
    val bookId: String,
    val callNo: String,
    val title: String,
    /** 원본 장서 데이터에 저자 열이 없어 현재는 항상 빈 문자열이다. */
    val author: String,
    /** 배가상태. "비치자료" / "관외대출자료" 등. 비어 있을 수 있다. */
    val holding: String,
    /** 같은 제목으로 묶인 장서 수(이 건 포함). 2 이상이면 "N권 소장"으로 표시한다. */
    val sameTitleCount: Int,
    /** 서버가 서가를 정하지 못하면 null. 이때는 위치 안내도 하지 못한다. */
    val shelf: BookShelf?
)

/**
 * 서버가 발화를 어떻게 알아들었는지. 화면에 "이렇게 찾았어요"로 보여 준다.
 *
 * 음성 인식 결과는 오타와 군더더기가 많아, 사용자가 자기 말이 어떻게 해석됐는지 볼 수 있어야
 * 다시 말할지 판단할 수 있다.
 */
data class BookQueryPlan(
    val query: String,
    val keywords: List<String>
)

/** 검색 한 번의 결과. */
data class BookSearchResult(
    val query: String,
    val plan: BookQueryPlan,
    val hits: List<BookHit>
)

/**
 * 검색 결과를 눌렀을 때 보여줄 한 건의 전체 정보.
 *
 * 앞쪽은 도서관 장서 데이터 그대로이고, [authors] 이하는 외부 서점에서 보강한 값이라
 * 비어 있을 수 있다. 서버가 저자기호·제목 대조로 검증에 실패한 결과는 아예 내려보내지
 * 않으므로, 값이 있으면 그대로 믿고 표시하면 된다.
 */
data class BookDetail(
    val bookId: String,
    val callNo: String,
    val title: String,
    /** 배가상태(비치자료/관외대출자료). 실시간 대출 상태가 아니다. */
    val holding: String,
    val kdcLabel: String,
    val shelf: BookShelf?,
    val authors: List<String>,
    val translators: List<String>,
    val publisher: String,
    val publishedYear: String,
    val isbn: String,
    /** 표지 이미지 URL. 매칭에 실패했으면 빈 문자열이라 표지 자리를 비운다. */
    val thumbnail: String,
    /** verified / unverified / mismatch / not_found. unverified 면 화면에 확인 문구를 덧붙인다. */
    val metaConfidence: String
) {
    val hasMeta: Boolean
        get() = authors.isNotEmpty() || publisher.isNotBlank() || thumbnail.isNotBlank()

    /** 외부에서 가져왔지만 저자 대조까지는 못한 정보인지. */
    val isMetaUnverified: Boolean
        get() = metaConfidence == "unverified"
}
