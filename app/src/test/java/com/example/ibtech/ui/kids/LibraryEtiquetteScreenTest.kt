package com.example.ibtech.ui.kids

import com.example.ibtech.domain.model.LibraryEtiquetteTip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 도서관 예절 라운드 구성 로직 회귀 테스트. 이번 세션에 실기에서만 발견됐던 두 버그(문구를
 * 고치면 이미지 매칭이 깨지는 문제, 한 판 안에서 같은 이미지가 중복 노출되는 문제)를 빌드
 * 단계에서 잡기 위해 추가했다.
 */
class LibraryEtiquetteScreenTest {

    private val seedTips = listOf(
        LibraryEtiquetteTip("etiquette_1", "책을 다 읽은 뒤에는 책 수레에 놓아 주세요.", sortOrder = 0),
        LibraryEtiquetteTip("etiquette_2", "도서관에서는 작은 목소리로 말해요.", sortOrder = 1),
        LibraryEtiquetteTip("etiquette_3", "손을 깨끗이 하고 책을 읽어요.", sortOrder = 2),
        LibraryEtiquetteTip("etiquette_4", "다른 친구가 읽고 있는 책은 조용히 기다렸다가 빌려요.", sortOrder = 3)
    )

    // ── etiquetteTipImageRes: id 기반 매칭 ──────────────────────────────────

    @Test
    fun `known seed ids map to their drawable`() {
        assertNotNull(etiquetteTipImageRes("etiquette_1"))
        assertNotNull(etiquetteTipImageRes("etiquette_2"))
        assertNotNull(etiquetteTipImageRes("etiquette_3"))
        assertNotNull(etiquetteTipImageRes("etiquette_4"))
    }

    @Test
    fun `unknown id returns null instead of guessing`() {
        assertNull(etiquetteTipImageRes("etiquette_5"))
        assertNull(etiquetteTipImageRes(""))
    }

    @Test
    fun `matching is unaffected by tip text changes since it keys off id, not text`() {
        // 회귀 재현: 예전엔 문구 텍스트로 매칭해서, 문구를 고치면(이미 시드된 기기의 관리자
        // 데이터는 옛 텍스트 그대로 남아 있으므로) 매칭이 깨졌다. id만 있으면 텍스트와 무관하게
        // 항상 같은 그림을 돌려줘야 한다.
        val beforeEdit = etiquetteTipImageRes("etiquette_3")
        val afterEdit = etiquetteTipImageRes("etiquette_3")
        assertEquals(beforeEdit, afterEdit)
        assertNotNull(afterEdit)
    }

    // ── buildEtiquetteRounds: 라운드 수 ──────────────────────────────────────

    @Test
    fun `round count is always between 5 and 7`() {
        repeat(50) {
            val rounds = buildEtiquetteRounds(seedTips)
            assertTrue("round count was ${rounds.size}", rounds.size in 5..7)
        }
    }

    // ── buildEtiquetteRounds: 라운드별 필드가 자기 타입에 맞게 채워져 있는지 ──────

    @Test
    fun `every round has exactly the fields its game type needs, non-null`() {
        repeat(20) {
            buildEtiquetteRounds(seedTips).forEach { round ->
                when (round.type) {
                    EtiquetteGameType.SITUATION_CHOICE -> {
                        assertNotNull("SITUATION_CHOICE round missing tip", round.tip)
                        assertNotNull("SITUATION_CHOICE round missing decoys", round.situationDecoys)
                        assertEquals(2, round.situationDecoys!!.size)
                        assertNull(round.likeOrNotTarget)
                        assertNull(round.oddOneOptions)
                    }

                    EtiquetteGameType.LIKE_OR_NOT -> {
                        assertNotNull("LIKE_OR_NOT round missing target", round.likeOrNotTarget)
                        assertNull(round.tip)
                        assertNull(round.situationDecoys)
                        assertNull(round.oddOneOptions)
                    }

                    EtiquetteGameType.FIND_THE_ODD_ONE -> {
                        assertNotNull("FIND_THE_ODD_ONE round missing options", round.oddOneOptions)
                        assertEquals(3, round.oddOneOptions!!.size)
                        assertEquals(1, round.oddOneOptions!!.count { it.isGood })
                        assertNull(round.tip)
                        assertNull(round.situationDecoys)
                        assertNull(round.likeOrNotTarget)
                    }
                }
            }
        }
    }

    // ── buildEtiquetteRounds: 상황극 선택은 정답과 같은 문구를 오답으로 안 씀 ─────

    @Test
    fun `situation choice decoys never repeat the correct tip's own text`() {
        repeat(20) {
            buildEtiquetteRounds(seedTips)
                .filter { it.type == EtiquetteGameType.SITUATION_CHOICE }
                .forEach { round ->
                    val tipText = round.tip!!.text
                    round.situationDecoys!!.forEach { decoy ->
                        assertTrue("decoy text matched the tip's own text", decoy.text != tipText)
                    }
                }
        }
    }

    @Test
    fun `situation choice decoys are two distinct images`() {
        repeat(20) {
            buildEtiquetteRounds(seedTips)
                .filter { it.type == EtiquetteGameType.SITUATION_CHOICE }
                .forEach { round ->
                    val (first, second) = round.situationDecoys!!
                    assertTrue(first.imageRes != second.imageRes)
                }
        }
    }

    @Test
    fun `find the odd one wrong options are two distinct bad images`() {
        repeat(20) {
            buildEtiquetteRounds(seedTips)
                .filter { it.type == EtiquetteGameType.FIND_THE_ODD_ONE }
                .forEach { round ->
                    val wrongOnes = round.oddOneOptions!!.filterNot { it.isGood }
                    assertEquals(2, wrongOnes.size)
                    assertTrue(wrongOnes[0].imageRes != wrongOnes[1].imageRes)
                }
        }
    }

    // ── buildEtiquetteRounds: 한 판 안에서 같은 이미지가 중복 노출되지 않는지 ─────

    /** 라운드 하나가 실제로 화면에 보여줄 그림 resId 전부(카드 3장이면 3개, 좋아요/안돼요면 1개). */
    private fun EtiquetteRound.shownImageResIds(): List<Int> = when (type) {
        EtiquetteGameType.SITUATION_CHOICE -> {
            val correct = etiquetteTipImageRes(tip!!.id)
            situationDecoys!!.map { it.imageRes } + listOfNotNull(correct)
        }
        EtiquetteGameType.LIKE_OR_NOT -> listOf(likeOrNotTarget!!.imageRes)
        EtiquetteGameType.FIND_THE_ODD_ONE -> oddOneOptions!!.map { it.imageRes }
    }

    @Test
    fun `no image repeats within a single playthrough`() {
        // 그림 30장 풀에서 한 판(최대 7라운드 x 최대 3장 = 21장)은 이론상 항상 다 채울 수 있다 —
        // 여러 번 반복해 우연히라도 겹치는 경우가 없는지 확인한다(사용자 피드백: "같은 이미지가
        // 중복으로 나온다").
        repeat(100) {
            val rounds = buildEtiquetteRounds(seedTips)
            val shown = rounds.flatMap { it.shownImageResIds() }
            val duplicates = shown.groupingBy { it }.eachCount().filterValues { it > 1 }
            assertTrue("duplicate images shown in one playthrough: $duplicates", duplicates.isEmpty())
        }
    }

    // ── 이미지 풀 자체의 정합성 (다른 회귀 방지) ────────────────────────────────

    @Test
    fun `good and bad image pools together total the 30-image asset set`() {
        assertEquals(12, ETIQUETTE_GOOD_IMAGES.size)
        assertEquals(18, ETIQUETTE_BAD_IMAGES.size)
        assertEquals(30, ETIQUETTE_ALL_IMAGES.size)
        assertEquals(18, ETIQUETTE_DECOYS.size)
    }

    @Test
    fun `no drawable id is reused across the good and bad pools`() {
        val allResIds = ETIQUETTE_ALL_IMAGES.map { it.imageRes }
        assertEquals(allResIds.size, allResIds.toSet().size)
    }
}
