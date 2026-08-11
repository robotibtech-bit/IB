package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.RecommendedBook

/**
 * 취향 퀴즈 누적 점수([scores], `"차원:값"` 태그 → 점수)로 책을 채점하고 추천 3권을 고른다
 * (기획 문서 "2. 나에게 맞는 책 - 추천 로직" 절). 순수 함수라 유닛 테스트로 검증한다.
 */
object BookTasteEngine {

    private const val TOP_POOL_SIZE = 8
    private const val RECOMMEND_COUNT = 3

    /** 태그가 하나라도 점수와 겹치는 책만 점수를 매긴다 — 태그가 전혀 없는(관리자가 태그 없이
     * 추가한) 책은 0점으로 후보에는 남되, 태그를 가진 책들에 밀려 상위권에 잘 안 뽑힌다. */
    fun score(book: RecommendedBook, scores: Map<String, Int>): Int =
        book.tags.sumOf { tag -> scores[tag] ?: 0 }

    /**
     * 상위 [TOP_POOL_SIZE]개 후보 안에서 무작위로 [RECOMMEND_COUNT]권을 고른다(기획: "상위
     * 후보군 안에서 약간 랜덤하게 골라 같은 답을 해도 매번 완전히 같은 결과만 나오지 않게
     * 한다"). [excludeIds]는 "다른 책도 보여줘"에서 이미 보여준 책을 다시 뽑지 않기 위한
     * 목록이다 — 제외하고 나면 후보가 부족해질 수 있으므로 그때는 전체 후보에서 다시 고른다.
     */
    fun recommend(
        books: List<RecommendedBook>,
        scores: Map<String, Int>,
        excludeIds: Set<String> = emptySet()
    ): List<RecommendedBook> {
        val enabled = books.filter { it.isEnabled }
        if (enabled.isEmpty()) return emptyList()

        val ranked = enabled.sortedByDescending { score(it, scores) }
        val pool = ranked.take(TOP_POOL_SIZE.coerceAtLeast(RECOMMEND_COUNT))
        val freshPool = pool.filterNot { it.id in excludeIds }
        val chosenFrom = if (freshPool.size >= RECOMMEND_COUNT) freshPool else pool

        return chosenFrom.shuffled().take(RECOMMEND_COUNT.coerceAtMost(chosenFrom.size))
    }

    /** 가장 점수가 높은 genre 태그(있으면) — 결과 캐릭터를 고르는 데 쓴다. */
    fun topGenre(scores: Map<String, Int>): String? =
        scores.entries
            .filter { it.key.startsWith("genre:") }
            .maxByOrNull { it.value }
            ?.key
            ?.removePrefix("genre:")

    /** 결과 캐릭터 키(기획 문서 "4. 책 추천 결과" 절 8종). genre 10종 중 friendship/emotion/daily
     * 세 개는 "마음 따뜻한 이야기 수집가" 하나로 묶인다 — 캐릭터는 8종뿐이라서다. 문자열 자체는
     * 화면이 `R.string.kids_book_character_<key>`로 찾는다. */
    fun resultCharacterKey(scores: Map<String, Int>): String = when (topGenre(scores)) {
        "fantasy" -> "wizard"
        "mystery" -> "explorer"
        "humor" -> "comedian"
        "animal" -> "animal_friend"
        "science" -> "scientist"
        "history" -> "time_traveler"
        "adventure" -> "adventurer"
        "friendship", "emotion", "daily" -> "warm_collector"
        else -> "warm_collector"
    }

    fun mergeScores(base: Map<String, Int>, delta: Map<String, Int>): Map<String, Int> {
        val merged = base.toMutableMap()
        delta.forEach { (tag, points) -> merged[tag] = (merged[tag] ?: 0) + points }
        return merged
    }
}
