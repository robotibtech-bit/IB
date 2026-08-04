package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.UsageTopic

/**
 * 이용방법 1차 메뉴 카드를 만든다 (요구사항 명세서 2.7절).
 *
 * 카테고리(부모가 없는 항목)마다 활성 하위 항목이 정확히 1개면 [UsageCategoryItem.singleAnswerTopicId]
 * 에 그 id를 채운다 — `UsageCategoryScreen`은 이 값이 있으면 `usage_subcategory`를 건너뛰고
 * 바로 `usage_answer`로 이동한다("세부 항목이 1개뿐인 카테고리는 세부 목록 없이 바로
 * usage_answer로 건너뛴다").
 */
object ResolveUsageCategoryItemsUseCase {

    operator fun invoke(allTopics: List<UsageTopic>): List<UsageCategoryItem> {
        val categories = allTopics
            .filter { it.parentId == null && it.isEnabled }
            .sortedBy { it.sortOrder }

        return categories.map { category ->
            val children = allTopics.filter { it.parentId == category.id && it.isEnabled }
            UsageCategoryItem(
                category = category,
                singleAnswerTopicId = children.singleOrNull()?.id
            )
        }
    }
}

data class UsageCategoryItem(
    val category: UsageTopic,
    val singleAnswerTopicId: String?
)
