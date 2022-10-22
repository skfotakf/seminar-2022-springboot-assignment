package com.wafflestudio.seminar.core.user.service

import com.wafflestudio.seminar.core.user.database.InstructorProfileRepository
import com.wafflestudio.seminar.core.user.database.SeminarEntity
import com.wafflestudio.seminar.core.user.database.SeminarRepository
import com.wafflestudio.seminar.core.user.domain.Seminar
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class SeminarService(
    private val authTokenService: AuthTokenService,
    private val instructorProfileRepository: InstructorProfileRepository,
    private val seminarRepository: SeminarRepository
    ) {

    fun createSeminar(seminar: Seminar, token: String): SeminarEntity{
        //todo: online 여부 외에는 하나라도 빠지면 400으로 응답하며, 적절한 에러 메시지를 포함합니다.
        //todo: name에 0글자가 오는 경우, capacity와 count에 양의 정수가 아닌 값이 오는 경우는 400으로 응답합니다.
        //todo: 세미나 진행자 자격을 가진 User만 요청할 수 있으며, 그렇지 않은 경우 403으로 응답
        return seminarRepository.save(SeminarEntity(seminar, token))

    }
   
   
    private fun SeminarEntity(seminar: Seminar, token: String) = seminar.run{
        SeminarEntity(
            name = seminar.name,
            capacity = seminar.capacity,
            count = seminar.count,
            time = LocalTime.parse(seminar.time, DateTimeFormatter.ISO_TIME),
            online = true,
            instructorProfileEntity = instructorProfileRepository.findByEmailInstructor(authTokenService.getCurrentEmail(token))
        
            
        )
        
    }

}