package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.QuizQuestion

/**
 * 퀴즈 세션 시작 시 낼 문제를 고른다 (요구사항 명세서 2.11~2.12절).
 *
 * 카테고리에 해당하는 활성 문제를 섞어 최대 [limit]개 뽑는다. 세션 진행 중에는 이 결과를
 * 순서대로 소비하기만 하고 다시 뽑지 않으므로, "세션 내 이미 낸 문제는 재출제하지 않는다"는
 * 요구사항이 별도 상태 추적 없이 이 한 번의 선택만으로 만족된다. 해당 주제 문제가 [limit]
 * 개보다 적으면 있는 만큼만 반환한다.
 */
object SelectQuizQuestionsUseCase {

    operator fun invoke(
        all: List<QuizQuestion>,
        category: String,
        limit: Int = 3
    ): List<QuizQuestion> {
        return all
            .filter { it.category == category && it.isEnabled }
            .shuffled()
            .take(limit)
    }
}
