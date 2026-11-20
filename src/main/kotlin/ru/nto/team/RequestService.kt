package ru.nto.team

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class RequestService(
    private val staffService: StaffService,
    private val requests: PassRequestRepository,
    private val clock: Clock,
) {
    private fun now(): LocalDateTime = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Europe/Moscow"))

    @Transactional(readOnly = true)
    fun list(code: String, status: String?): List<RequestListDto> {
        val staff = staffService.authenticate(code)
        val filtered = if (status == null) {
            requests.findAllByHostIdOrderByValidFromAscIdAsc(staff.id)
        } else {
            val parsed = RequestStatus.entries.find { it.name == status } ?: throw ApiException(400, "Invalid status")
            requests.findAllByHostIdAndStatusOrderByValidFromAscIdAsc(staff.id, parsed)
        }
        return filtered.map { it.toListDto() }
    }

    @Transactional(readOnly = true)
    fun details(code: String, id: Long): RequestDetailsDto {
        val staff = staffService.authenticate(code)
        return ownRequest(id, staff.id).toDetailsDto()
    }

    @Transactional
    fun create(code: String, body: CreateRequestBody): RequestDetailsDto {
        val staff = staffService.authenticate(code)
        val currentTime = now()
        val data = RequestValidation.create(body, currentTime)
        // Границы соседних интервалов могут совпадать без пересечения.
        if (requests.hasOverlap(staff.id, data.phone, listOf(RequestStatus.PENDING, RequestStatus.APPROVED),
                data.validFrom, data.validTo)) throw ApiException(409, "Duplicate request")
        val timestamp = currentTime.truncatedTo(ChronoUnit.SECONDS)
        return requests.save(PassRequest(host = staff, fullName = data.fullName, phone = data.phone,
            organization = data.organization, purpose = data.purpose, validFrom = data.validFrom,
            validTo = data.validTo, status = RequestStatus.PENDING, comment = null,
            createdAt = timestamp, updatedAt = timestamp)).toDetailsDto()
    }

    @Transactional
    fun cancel(code: String, id: Long): RequestDetailsDto {
        val staff = staffService.authenticate(code)
        val request = ownRequest(id, staff.id)
        requirePending(request)
        request.status = RequestStatus.CANCELLED
        request.updatedAt = now().truncatedTo(ChronoUnit.SECONDS)
        return requests.save(request).toDetailsDto()
    }

    @Transactional
    fun decide(id: Long, body: DecisionBody): RequestDetailsDto {
        val request = requests.findById(id).orElseThrow { ApiException(404, "Request not found") }
        val (status, comment) = RequestValidation.decision(body)
        requirePending(request)
        request.status = status
        request.comment = comment
        request.updatedAt = now().truncatedTo(ChronoUnit.SECONDS)
        return requests.save(request).toDetailsDto()
    }

    private fun ownRequest(id: Long, hostId: Long): PassRequest =
        requests.findByIdAndHostId(id, hostId) ?: throw ApiException(404, "Request not found")

    private fun requirePending(request: PassRequest) {
        if (request.status != RequestStatus.PENDING) throw ApiException(409, "Invalid status transition")
    }
}
