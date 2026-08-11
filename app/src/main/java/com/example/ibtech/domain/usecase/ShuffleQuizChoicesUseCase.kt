package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.QuizQuestion

/**
 * 문제 하나가 출제될 때 보기 순서를 랜덤화한다 (기획 문서 "보기 순서는 가능하면 랜덤화하되
 * 정답 매핑이 절대 깨지지 않게 한다").
 *
 * 보기 텍스트가 아니라 인덱스 목록을 섞은 뒤 그 순서대로 재배열한다 — 보기 문구가 우연히
 * 같은 경우([QuizQuestion.choices]에 중복 텍스트가 있는 경우)에도 정답 위치가 항상 정확히
 * 맞물린다. 원본 [QuizQuestion](저장소의 정본 데이터)은 건드리지 않고, 화면에 낼 새 복사본만
 * 만든다 — 매 세션·매 문제마다 이 함수를 다시 호출해야 매번 다른 순서가 나온다.
 */
object ShuffleQuizChoicesUseCase {

    operator fun invoke(question: QuizQuestion): QuizQuestion {
        val order = question.choices.indices.shuffled()
        val shuffledChoices = order.map { question.choices[it] }
        val newCorrectIndex = order.indexOf(question.correctIndex)
        return question.copy(choices = shuffledChoices, correctIndex = newCorrectIndex)
    }
}
