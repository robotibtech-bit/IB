package com.example.ibtech.ui.kids

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ibtech.data.repository.KidsContentRepository
import kotlinx.coroutines.launch

/**
 * 어린이 콘텐츠 메뉴 (요구사항 명세서 2.10절). 메뉴 자체는 고정된 3개 진입점이라 별도 UI 상태가
 * 없다 — 이 화면이 어린이 콘텐츠 영역의 첫 진입점이므로 콘텐츠 시딩만 여기서 트리거한다
 * (7단계 `UsageCategoryViewModel`과 같은 자리).
 */
class KidsMenuViewModel(application: Application) : AndroidViewModel(application) {

    private val kidsContentRepository = KidsContentRepository.getInstance(application)

    init {
        viewModelScope.launch {
            kidsContentRepository.ensureSeeded(application)
            kidsContentRepository.ensureQuizBank200(application)
            kidsContentRepository.ensureBookPool60(application)
        }
    }
}
