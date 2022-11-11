package com.wafflestudio.seminar.core.seminar.service

import com.querydsl.jpa.impl.JPAQueryFactory
import com.wafflestudio.seminar.common.Seminar400
import com.wafflestudio.seminar.common.Seminar403
import com.wafflestudio.seminar.common.Seminar404
import com.wafflestudio.seminar.core.user.UserTestHelper
import com.wafflestudio.seminar.core.user.api.request.SeminarRequest
import com.wafflestudio.seminar.core.user.database.SeminarRepository
import com.wafflestudio.seminar.core.user.database.UserRepository
import com.wafflestudio.seminar.core.user.database.UserSeminarRepository
import com.wafflestudio.seminar.core.user.domain.*
import com.wafflestudio.seminar.core.user.service.AuthTokenService
import com.wafflestudio.seminar.core.user.service.SeminarService
import com.wafflestudio.seminar.global.HibernateQueryCounter
import org.mockito.BDDMockito.anyString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@SpringBootTest
internal class SeminarServiceTest @Autowired constructor(
    private val hibernateQueryCounter: HibernateQueryCounter,
    private val seminarRepository: SeminarRepository,
    private val userSeminarRepository: UserSeminarRepository,
    private val userRepository: UserRepository,
    private val seminarService: SeminarService,
    private val userTestHelper: UserTestHelper
){
    @MockBean
    private lateinit var authTokenService: AuthTokenService

    @BeforeEach
    fun createInstructor() {
        given(authTokenService.getCurrentEmail(anyString())).willReturn("email@snu.ac.kr")
        userRepository.deleteAll()
        seminarRepository.deleteAll()
        userTestHelper.createInstructor("email@snu.ac.kr")
    }

    /*
    * createSeminar()
    */

    // Failed: [N+1]
    @Test
    fun `(createSeminar) 세미나 생성`() {
        // Given
        val seminarRequest = SeminarRequest("spring", 30, 6, "19:00", false)
        
        // When
        val (response, queryCount) = hibernateQueryCounter.count {
            seminarService.createSeminar(seminarRequest, "token")
        }
        
        // Then
        assertThat(response.name).isEqualTo("spring")
        assertThat(queryCount).isEqualTo(8) // [N+1] but was 10
    }

    // Passed
    @Test
    fun `(createSeminar) request body에 필수 정보가 누락되면 400으로 응답`() {
        // Given
        val seminarRequest = SeminarRequest(null, 30, 6, "19:00", false)

        // When
        val response = assertThrows<Seminar400> { seminarService.createSeminar(seminarRequest, "token") }

        // Then
        assertThat(response.message).isEqualTo("입력하지 않은 값이 있습니다")
    }

    // Passed
    @Test
    fun `(createSeminar) name에 0글자가 오는 경우, capacity와 count에 양의 정수가 아닌 값이 오는 경우는 400으로 응답`() {
        // Given
        val seminarRequest1 = SeminarRequest("", 30, 6, "19:00", false)
        val seminarRequest2 = SeminarRequest("spring", 0, 6, "19:00", false)
        val seminarRequest3 = SeminarRequest("spring", 30, -2, "19:00", false)

        // When
        val response1 = assertThrows<Seminar400> { seminarService.createSeminar(seminarRequest1, "token") }
        val response2 = assertThrows<Seminar400> { seminarService.createSeminar(seminarRequest2, "token") }
        val response3 = assertThrows<Seminar400> { seminarService.createSeminar(seminarRequest3, "token") }

        // Then
        assertThat(response1.message).isEqualTo("형식에 맞지 않게 입력하지 않은 값이 있습니다")
        assertThat(response2.message).isEqualTo("형식에 맞지 않게 입력하지 않은 값이 있습니다")
        assertThat(response3.message).isEqualTo("형식에 맞지 않게 입력하지 않은 값이 있습니다")
    }

    // Passed
    @Test
    fun `(createSeminar) 진행자 자격이 없는 User가 요청하면 403으로 응답`() {
        // Given
        userRepository.deleteAll()
        userTestHelper.createParticipant("email@snu.ac.kr")
        val seminarRequest = SeminarRequest("spring", 30, 6, "19:00", false)

        // When
        val response = assertThrows<Seminar403> { seminarService.createSeminar(seminarRequest, "token") }

        // Then
        assertThat(response.message).isEqualTo("진행자만 세미나를 생성할 수 있습니다")
    }

    /*
    * updateSeminar()
    */

    // Failed: [N+1]
    @Test
    fun `(updateSeminar) 세미나 수정`() {
        // Given
        seminarRepository.save(SeminarEntity("spring", 30, 6, "19:00", false))
        val seminarRequest = SeminarRequest("spring", 300, 60, "20:00", true)

        // When
        val (response, queryCount) = hibernateQueryCounter.count {
            seminarService.updateSeminar(seminarRequest, "token")
        }

        // Then
        assertThat(response.capacity).isEqualTo(300)
        assertThat(response.count).isEqualTo(60)
        assertThat(response.time).isEqualTo("20:00")
        assertThat(response.online).isEqualTo(true)
        assertThat(queryCount).isEqualTo(4) // [N+1] but was 5
    }

    // Passed
    @Test
    fun `(updateSeminar) request body에 필수 정보가 누락되면 400으로 응답`() {
        // Given
        val seminarRequest = SeminarRequest(null, 30, 6, "19:00", false)

        // When
        val response = assertThrows<Seminar400> { seminarService.updateSeminar(seminarRequest, "token") }

        // Then
        assertThat(response.message).isEqualTo("입력하지 않은 값이 있습니다")
    }

    // Passed
    @Test
    fun `(updateSeminar) name에 0글자가 오는 경우, capacity와 count에 양의 정수가 아닌 값이 오는 경우는 400으로 응답`() {
        // Given
        val seminarRequest1 = SeminarRequest("", 30, 6, "19:00", false)
        val seminarRequest2 = SeminarRequest("spring", 0, 6, "19:00", false)
        val seminarRequest3 = SeminarRequest("spring", 30, -2, "19:00", false)

        // When
        val response1 = assertThrows<Seminar400> { seminarService.updateSeminar(seminarRequest1, "token") }
        val response2 = assertThrows<Seminar400> { seminarService.updateSeminar(seminarRequest2, "token") }
        val response3 = assertThrows<Seminar400> { seminarService.updateSeminar(seminarRequest3, "token") }

        // Then
        assertThat(response1.message).isEqualTo("형식에 맞지 않게 입력하지 않은 값이 있습니다")
        assertThat(response2.message).isEqualTo("형식에 맞지 않게 입력하지 않은 값이 있습니다")
        assertThat(response3.message).isEqualTo("형식에 맞지 않게 입력하지 않은 값이 있습니다")
    }

    // Passed
    @Test
    fun `(updateSeminar) 진행자 자격이 없는 User가 요청하면 403으로 응답`() {
        // Given
        userRepository.deleteAll()
        userTestHelper.createParticipant("email@snu.ac.kr")
        val seminarRequest = SeminarRequest("spring", 30, 6, "19:00", false)

        // When
        val response = assertThrows<Seminar403> { seminarService.updateSeminar(seminarRequest, "token") }

        // Then
        assertThat(response.message).isEqualTo("세미나를 수정할 자격이 없습니다")
    }

    // Failed: 에러가 발생하지 않고 정상적으로 수행됨
    @Test
    fun `(updateSeminar) 해당 세미나를 만들지 않은 User가 요청하면 403으로 응답`() {
        // Given
        userRepository.deleteAll()
        userTestHelper.createInstructor("email@snu.ac.kr")
        seminarRepository.save(SeminarEntity("spring", 30, 6, "19:00", false))
        val seminarRequest = SeminarRequest("spring", 300, 60, "20:00", true)

        // When
        val response = assertThrows<Seminar403> { seminarService.updateSeminar(seminarRequest, "token") }

        // Then
        assertThat(response.message).isEqualTo("진행자만 세미나를 생성할 수 있습니다")
    }


    /*
    * getSeminarById()
    */

    // Failed: [N+1]
    @Test
    fun `(getSeminarById) seminar id로 세미나 불러오기`() {
        // Given
        createFixtures()
        val id = 1L

        // When
        val (response, queryCount) = hibernateQueryCounter.count {
            seminarService.getSeminarById(id, "token")
        }

        // Then
        assertThat(response.name).isEqualTo("spring")
        assertThat(response.instructors).hasSize(1)
        assertThat(response.participants).hasSize(10)
        assertThat(queryCount).isEqualTo(4) // [N+1] but was 15
    }

    // Passed
    @Test
    fun `(getSeminarById) seminar_id에 해당하는 Seminar가 없는 경우 404로 응답`() {
        // Given
        val seminar = SeminarEntity("spring", 30, 6, "19:00", false)
        seminarRepository.save(seminar)
        val id = 100L

        // When
        val response = assertThrows<Seminar404> { seminarService.getSeminarById(id, "token") }

        // Then
        assertThat(response.message).isEqualTo("해당하는 세미나가 없습니다")
    }

    /*
    * getSeminars()
    */

    // Passed
    @Test
    fun `(getSeminars) 전체 세미나 불러오기`() {
        // Given
        createSeminars()

        // When
        val (response, queryCount) = hibernateQueryCounter.count {
            seminarService.getSeminars("token")
        }

        // Then
        assertThat(response).hasSize(10)
        assertThat(queryCount).isEqualTo(1) // [N+1] but was 15
    }

    // Passed
    @Test
    fun `(getSeminars) 특정 string을 포함한 세미나 불러오기`() {
        // Given
        createSeminars()

        // When
        val (response, queryCount) = hibernateQueryCounter.count {
            seminarService.getSeminars("token")
        }

        // Then
        assertThat(response).hasSize(10)
        assertThat(queryCount).isEqualTo(1) // [N+1] but was 15
    }

    /*
    * getSeminarByName()
    */

    @Test
    fun getSeminarByName() {
        // Given

        // When

        // Then
    }

    /*
    * joinSeminar()
    */

    @Test
    fun joinSeminar() {
        // Given

        // When

        // Then
    }

    /*
    * dropSeminar()
    */

    @Test
    fun dropSeminar() {
        // Given

        // When

        // Then
    }

    private fun createFixtures() {
        val instructor = userTestHelper.createInstructor("ins@tructor.com")
        val participants = (1..10).map { userTestHelper.createParticipant("par@ticipant#$it.com") }

        val seminar = SeminarEntity("spring", 30, 6, "19:00", false)
        val userSeminarList: MutableList<UserSeminarEntity> =
            participants.map { UserSeminarEntity(it, seminar, "participant", LocalDateTime.now()) } as MutableList<UserSeminarEntity>
        userSeminarList.add(UserSeminarEntity(instructor, seminar, "instructor", LocalDateTime.now()))
        seminar.userSeminars = userSeminarList
        seminarRepository.save(seminar)
        userSeminarList.forEach { userSeminarRepository.save(it) }
    }

    private fun createSeminars() {
        val seminarList = (1..10).map { SeminarEntity("spring$it", 30, 6, "19:00", false) }
        seminarList.forEach {seminarRepository.save(it)}
    }
}