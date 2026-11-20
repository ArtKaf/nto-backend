package ru.nto.team

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

data class ValidatedRequest(val fullName: String, val phone: String, val organization: String?, val purpose: String,
                            val validFrom: LocalDateTime, val validTo: LocalDateTime)

object RequestValidation {
    private val datePattern = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}")
    private val formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss").withResolverStyle(ResolverStyle.STRICT)

    private fun parseDate(value: String?): LocalDateTime? {
        if (value == null || !datePattern.matches(value)) return null
        return try { LocalDateTime.parse(value, formatter) } catch (_: DateTimeParseException) { null }
    }

    fun create(body: CreateRequestBody, now: LocalDateTime): ValidatedRequest {
        val errors = linkedMapOf<String, String>()
        val name = body.fullName?.trim().orEmpty()
        val phone = body.phone?.trim().orEmpty()
        val organization = body.organization?.trim()?.ifEmpty { null }
        val purpose = body.purpose?.trim().orEmpty()
        if (name.length !in 5..150) errors["fullName"] = "Must contain 5 to 150 characters"
        if (!phone.matches(Regex("\\+7[0-9]{10}"))) errors["phone"] = "Invalid phone number"
        if (organization != null && organization.length > 150) errors["organization"] = "Must contain at most 150 characters"
        if (purpose.length !in 5..200) errors["purpose"] = "Must contain 5 to 200 characters"
        val from = parseDate(body.validFrom?.trim())
        val to = parseDate(body.validTo?.trim())
        if (from == null) errors["validFrom"] = "Invalid date and time"
        else if (from.isBefore(now)) errors["validFrom"] = "Must not be in the past"
        if (to == null) errors["validTo"] = "Invalid date and time"
        else if (from != null && (!to.isAfter(from) || to.toLocalDate() != from.toLocalDate()))
            errors["validTo"] = "Must be later on the same day"
        if (errors.isNotEmpty()) throw ValidationException(errors)
        return ValidatedRequest(name, phone, organization, purpose, from!!, to!!)
    }

    fun decision(body: DecisionBody): Pair<RequestStatus, String?> {
        val errors = linkedMapOf<String, String>()
        val status = when (body.status?.trim()) {
            "APPROVED" -> RequestStatus.APPROVED
            "REJECTED" -> RequestStatus.REJECTED
            else -> null
        }
        if (status == null) errors["status"] = "Invalid status"
        val comment = body.comment?.trim()
        if (status == RequestStatus.REJECTED && (comment.isNullOrEmpty() || comment.length > 500))
            errors["comment"] = "Must contain 1 to 500 characters"
        if (errors.isNotEmpty()) throw ValidationException(errors)
        return status!! to if (status == RequestStatus.REJECTED) comment else null
    }
}
