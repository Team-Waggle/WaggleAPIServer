package io.waggle.waggleapiserver.common.validation.validator

import io.waggle.waggleapiserver.common.validation.constraint.UniquePosition
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class UniquePositionValidator : ConstraintValidator<UniquePosition, List<RecruitmentUpsertRequest>?> {
    override fun isValid(
        value: List<RecruitmentUpsertRequest>?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (value == null) return true
        return value.distinctBy { it.position }.size == value.size
    }
}
