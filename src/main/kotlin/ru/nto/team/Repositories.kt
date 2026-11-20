package ru.nto.team

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface StaffRepository : JpaRepository<Staff, Long> {
    fun findByCode(code: String): Staff?
}

interface PassRequestRepository : JpaRepository<PassRequest, Long> {
    fun findAllByHostIdOrderByValidFromAscIdAsc(hostId: Long): List<PassRequest>
    fun findAllByHostIdAndStatusOrderByValidFromAscIdAsc(hostId: Long, status: RequestStatus): List<PassRequest>
    fun findByIdAndHostId(id: Long, hostId: Long): PassRequest?

    @Query("""
        select count(r) > 0 from PassRequest r
        where r.host.id = :hostId and r.phone = :phone and r.status in :statuses
          and r.validFrom < :validTo and r.validTo > :validFrom
    """)
    fun hasOverlap(@Param("hostId") hostId: Long, @Param("phone") phone: String,
                   @Param("statuses") statuses: Collection<RequestStatus>,
                   @Param("validFrom") validFrom: LocalDateTime, @Param("validTo") validTo: LocalDateTime): Boolean
}
