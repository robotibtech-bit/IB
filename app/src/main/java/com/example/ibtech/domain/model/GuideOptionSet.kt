package com.example.ibtech.domain.model

/**
 * 시설 상세 화면이 보여줄 안내 방식 버튼 조합 (요구사항 명세서 4장 `resolveGuideOptions`).
 *
 * [domain.usecase.ResolveGuideOptionUseCase]의 결과 타입이며, UI는 이 값만 보고 버튼을
 * 구성한다 — 층 비교나 guideMode 분기를 화면에서 직접 하지 않는다.
 */
sealed interface GuideOptionSet {

    /** 비활성 시설. 목록 단계에서 걸러지므로 상세 화면에 정상적으로 도달하면 안 되는 방어용 상태. */
    data object Hidden : GuideOptionSet

    /** 층·안내방식이 아직 설정되지 않은 POI. Temi 직접 동행만 제공하거나 화면에서 숨긴다(3.2절). */
    data object TemiEscortOnlyUnconfigured : GuideOptionSet

    /** guideMode가 ESCORT/BOTH. `동행 안내`와 `위치만 보기` 모두 노출 — 기준층이 아니어도
     * sourcePoiName이 엘리베이터 POI를 가리키면 동행이 가능하다. */
    data object EscortAndLocationOnly : GuideOptionSet

    /** guideMode가 LOCATION_ONLY. 동행 없이 층·방향 안내만 제공. */
    data object LocationOnlyWithDirections : GuideOptionSet
}
