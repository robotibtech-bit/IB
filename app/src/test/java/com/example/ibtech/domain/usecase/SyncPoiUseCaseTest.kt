package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.FacilityDirection
import com.example.ibtech.domain.model.FacilitySyncStatus
import com.example.ibtech.domain.model.GuideMode
import com.example.ibtech.domain.model.LibrarySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPoiUseCaseTest {

    @Test
    fun `new remote poi is added on the base floor with escort plus location guide mode, front direction, visible on by default`() {
        val result = SyncPoiUseCase(local = emptyList(), remote = listOf("새 POI"))

        assertEquals(1, result.size)
        val added = result.single()
        assertEquals("새 POI", added.sourcePoiName)
        assertEquals(LibrarySettings.DEFAULT_BASE_FLOOR, added.floor)
        assertEquals(GuideMode.BOTH, added.guideMode)
        assertEquals(FacilityDirection.FRONT, added.direction)
        assertTrue(added.isEnabled)
        assertEquals(FacilitySyncStatus.NEW, added.syncStatus)
    }

    @Test
    fun `existing facility keeps admin metadata and updates sync status`() {
        val existing = Facility(
            id = "poi-1",
            sourcePoiName = "poi-1",
            name = "관리자가 지정한 이름",
            floor = 2,
            shortDescription = "관리자 설명",
            guideMode = GuideMode.ESCORT,
            isEnabled = true,
            sortOrder = 5,
            syncStatus = FacilitySyncStatus.SYNCED
        )

        val result = SyncPoiUseCase(local = listOf(existing), remote = listOf("poi-1"))

        assertEquals(1, result.size)
        val synced = result.single()
        assertEquals("관리자가 지정한 이름", synced.name)
        assertEquals(2, synced.floor)
        assertEquals("관리자 설명", synced.shortDescription)
        assertEquals(GuideMode.ESCORT, synced.guideMode)
        assertTrue(synced.isEnabled)
        assertEquals(5, synced.sortOrder)
        assertEquals(FacilitySyncStatus.SYNCED, synced.syncStatus)
    }

    @Test
    fun `poi missing from remote is marked not found but not deleted`() {
        val existing = Facility(
            id = "poi-1",
            sourcePoiName = "poi-1",
            name = "사라질 시설",
            floor = 1,
            isEnabled = true
        )

        val result = SyncPoiUseCase(local = listOf(existing), remote = emptyList())

        assertEquals(1, result.size)
        val disappeared = result.single()
        assertEquals("poi-1", disappeared.sourcePoiName)
        assertEquals(FacilitySyncStatus.NOT_FOUND_ON_TEMI, disappeared.syncStatus)
        // 관리자 메타데이터는 그대로 남아 있어야 한다 — 삭제가 아니라 상태 표시만 바뀐다.
        assertEquals("사라질 시설", disappeared.name)
        assertTrue(disappeared.isEnabled)
    }

    @Test
    fun `mixed new changed and disappeared poi are all handled together`() {
        val kept = Facility(id = "keep", sourcePoiName = "keep", name = "유지", floor = 1, isEnabled = true)
        val gone = Facility(id = "gone", sourcePoiName = "gone", name = "삭제 예정", floor = 1, isEnabled = true)

        val result = SyncPoiUseCase(local = listOf(kept, gone), remote = listOf("keep", "new"))

        assertEquals(3, result.size)
        assertEquals(FacilitySyncStatus.SYNCED, result.first { it.sourcePoiName == "keep" }.syncStatus)
        assertEquals(FacilitySyncStatus.NEW, result.first { it.sourcePoiName == "new" }.syncStatus)
        assertEquals(FacilitySyncStatus.NOT_FOUND_ON_TEMI, result.first { it.sourcePoiName == "gone" }.syncStatus)
    }

    @Test
    fun `empty local and empty remote produce empty result`() {
        val result = SyncPoiUseCase(local = emptyList(), remote = emptyList())
        assertTrue(result.isEmpty())
    }
}
