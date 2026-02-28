package io.waggle.waggleapiserver.domain.memberreview.enums

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode

enum class ReviewTag(val type: ReviewType) {
    // LIKE
    PUNCTUAL(ReviewType.LIKE),
    SKILLED(ReviewType.LIKE),
    GOOD_COMMUNICATOR(ReviewType.LIKE),
    RESPONSIBLE(ReviewType.LIKE),
    KIND(ReviewType.LIKE),
    PICASSO(ReviewType.LIKE),
    PROMOTER(ReviewType.LIKE),
    GOAT(ReviewType.LIKE),
    LEGEND(ReviewType.LIKE),
    METICULOUS(ReviewType.LIKE),

    // DISLIKE
    LATE(ReviewType.DISLIKE),
    NO_SHOW(ReviewType.DISLIKE),
    SENSITIVE(ReviewType.DISLIKE),
    UNKIND(ReviewType.DISLIKE),
    DESERTER(ReviewType.DISLIKE),
    ;

    companion object {
        fun validateTags(type: ReviewType, tags: Collection<ReviewTag>) {
            val invalidTags = tags.filter { it.type != type }
            if (invalidTags.isNotEmpty()) {
                throw BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "Tags $invalidTags are not allowed for review type $type",
                )
            }
        }
    }
}
