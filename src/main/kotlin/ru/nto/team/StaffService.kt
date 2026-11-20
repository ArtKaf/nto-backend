package ru.nto.team

import org.springframework.stereotype.Service

@Service
class StaffService(private val staffRepository: StaffRepository) {
    fun authenticate(code: String): Staff {
        if (!code.matches(Regex("[A-Za-z0-9]{4}"))) throw ApiException(400, "Invalid code format")
        return staffRepository.findByCode(code) ?: throw ApiException(401, "Unauthorized")
    }

    fun profile(code: String): ProfileDto = ProfileDto(authenticate(code).fullName)
}
