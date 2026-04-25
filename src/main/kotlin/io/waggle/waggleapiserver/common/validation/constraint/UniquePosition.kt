package io.waggle.waggleapiserver.common.validation.constraint

import io.waggle.waggleapiserver.common.validation.validator.UniquePositionValidator
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [UniquePositionValidator::class])
annotation class UniquePosition(
    val message: String = "must not contain duplicate positions",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
