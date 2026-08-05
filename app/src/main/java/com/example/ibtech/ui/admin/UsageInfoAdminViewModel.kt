package com.example.ibtech.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.R
import com.example.ibtech.data.repository.FacilityRepository
import com.example.ibtech.data.repository.UsageRepository
import com.example.ibtech.domain.model.Facility
import com.example.ibtech.domain.model.UsageTopic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 이용정보 세부 항목 추가/수정 중인 임시 입력값. [id]가 null이면 신규 추가다. */
data class UsageTopicDraft(
    val id: String? = null,
    val categoryId: String = "",
    val title: String = "",
    val shortAnswer: String = "",
    val qrUrl: String = "",
    val relatedFacilityId: String? = null,
    val isEnabled: Boolean = true,
    val sortOrderText: String = "0",
    val titleError: String? = null
)

data class UsageInfoAdminUiState(
    val isLoaded: Boolean = false,
    val categories: List<UsageTopic> = emptyList(),
    val topicsByCategory: Map<String, List<UsageTopic>> = emptyMap(),
    val facilities: List<Facility> = emptyList(),
    val editingDraft: UsageTopicDraft? = null
)

/** 이용정보(4개 고정 카테고리 하위 세부 항목) 관리 화면 (로드맵 10단계). */
class UsageInfoAdminViewModel(application: Application) : AndroidViewModel(application) {

    private val usageRepository = UsageRepository.getInstance(application)
    private val facilityRepository = FacilityRepository.getInstance(application)

    private val editingDraft = MutableStateFlow<UsageTopicDraft?>(null)

    val uiState: StateFlow<UsageInfoAdminUiState> = combine(
        usageRepository.topics,
        facilityRepository.visibleFacilities,
        editingDraft
    ) { topics, facilities, draft ->
        val categories = topics.filter { it.parentId == null }.sortedBy { it.sortOrder }
        val byCategory = categories.associate { category ->
            category.id to topics.filter { it.parentId == category.id }.sortedBy { it.sortOrder }
        }
        UsageInfoAdminUiState(
            isLoaded = true,
            categories = categories,
            topicsByCategory = byCategory,
            facilities = facilities,
            editingDraft = draft
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UsageInfoAdminUiState()
    )

    fun onAddTopic(categoryId: String) {
        editingDraft.value = UsageTopicDraft(categoryId = categoryId)
    }

    fun onEditTopic(topic: UsageTopic) {
        editingDraft.value = UsageTopicDraft(
            id = topic.id,
            categoryId = topic.parentId.orEmpty(),
            title = topic.title,
            shortAnswer = topic.shortAnswer.orEmpty(),
            qrUrl = topic.qrUrl.orEmpty(),
            relatedFacilityId = topic.relatedFacilityId,
            isEnabled = topic.isEnabled,
            sortOrderText = topic.sortOrder.toString()
        )
    }

    fun onDismissDialog() {
        editingDraft.value = null
    }

    fun onDraftTitleChange(value: String) {
        editingDraft.update { it?.copy(title = value, titleError = null) }
    }

    fun onDraftShortAnswerChange(value: String) {
        editingDraft.update { it?.copy(shortAnswer = value) }
    }

    fun onDraftQrUrlChange(value: String) {
        editingDraft.update { it?.copy(qrUrl = value) }
    }

    fun onDraftFacilityChange(facilityId: String?) {
        editingDraft.update { it?.copy(relatedFacilityId = facilityId) }
    }

    fun onDraftEnabledChange(value: Boolean) {
        editingDraft.update { it?.copy(isEnabled = value) }
    }

    fun onDraftSortOrderChange(value: String) {
        editingDraft.update { it?.copy(sortOrderText = value) }
    }

    fun onSaveDraft() {
        val draft = editingDraft.value ?: return
        if (draft.title.isBlank()) {
            editingDraft.update { it?.copy(titleError = getApplication<Application>().getString(R.string.admin_error_blank_title)) }
            return
        }
        val sortOrder = draft.sortOrderText.trim().toIntOrNull() ?: 0
        val topic = UsageTopic(
            id = draft.id ?: "usage_${System.currentTimeMillis()}",
            parentId = draft.categoryId,
            title = draft.title.trim(),
            shortAnswer = draft.shortAnswer.trim().ifBlank { null },
            qrUrl = draft.qrUrl.trim().ifBlank { null },
            relatedFacilityId = draft.relatedFacilityId,
            isEnabled = draft.isEnabled,
            sortOrder = sortOrder
        )
        viewModelScope.launch {
            usageRepository.upsertTopic(topic)
            editingDraft.value = null
        }
    }

    fun onDeleteTopic(id: String) {
        viewModelScope.launch { usageRepository.deleteTopic(id) }
    }
}
