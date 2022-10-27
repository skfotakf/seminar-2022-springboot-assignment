package com.wafflestudio.seminar.core.user.service

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.wafflestudio.seminar.common.Seminar400
import com.wafflestudio.seminar.core.user.api.request.UpdateProfileRequest
import com.wafflestudio.seminar.core.user.api.response.GetProfile
import com.wafflestudio.seminar.core.user.database.*
import com.wafflestudio.seminar.core.user.domain.QInstructorProfileEntity
import com.wafflestudio.seminar.core.user.domain.QParticipantProfileEntity
import com.wafflestudio.seminar.core.user.domain.QUserEntity
import com.wafflestudio.seminar.core.user.domain.UserEntity
import com.wafflestudio.seminar.core.user.dto.*
import org.modelmapper.ModelMapper
import org.springframework.stereotype.Service


@Service
class UserService(
    private val userRepository: UserRepository,
    private val participantProfileRepository: ParticipantProfileRepository,
    private val instructorProfileRepository: InstructorProfileRepository,
    private val authTokenService: AuthTokenService,
    private val queryFactory: JPAQueryFactory,
    private val modelMapper: ModelMapper
) {
    fun getProfile(email : String, token: String): GetProfile {
        val findByEmailEntity = userRepository.findByEmail(email)

        val qUserEntity: QUserEntity = QUserEntity.userEntity
        val qParticipantProfileEntity: QParticipantProfileEntity? = QParticipantProfileEntity.participantProfileEntity
        val qInstructorProfileEntity: QInstructorProfileEntity? = QInstructorProfileEntity.instructorProfileEntity

        val userProfileDto =makeUserProfileDto(email, qUserEntity, qParticipantProfileEntity, qInstructorProfileEntity)

        val userEntity = userProfileDto[0].userEntity
        val participantProfileEntity = userProfileDto[0].participantProfileEntity
        val instructorProfileEntity = userProfileDto[0].instructorProfileEntity
        
        return if(findByEmailEntity.participant != null && findByEmailEntity.instructor == null) {
            GetProfile(
                userEntity?.id, 
                userEntity?.username, 
                userEntity?.email, 
                userEntity?.lastLogin,
                userEntity?.dateJoined,
                GetProfileParticipantDto(participantProfileEntity?.id, participantProfileEntity?.university, participantProfileEntity?.isRegistered),
               null
            )
            
        } else if(findByEmailEntity.participant == null && findByEmailEntity.instructor != null){
            GetProfile(
                userEntity?.id, 
                userEntity?.username, 
                userEntity?.email, 
                userEntity?.lastLogin, 
                userEntity?.dateJoined,
               null,
                GetProfileInstructorDto(instructorProfileEntity?.id, instructorProfileEntity?.company, instructorProfileEntity?.year)
            )
        } else if(findByEmailEntity.participant != null && findByEmailEntity.instructor != null){
            GetProfile(
                userEntity?.id, 
                userEntity?.username, 
                userEntity?.email, 
                userEntity?.lastLogin, 
                userEntity?.dateJoined,
                GetProfileParticipantDto(participantProfileEntity?.id,participantProfileEntity?.university, participantProfileEntity?.isRegistered),
                GetProfileInstructorDto(instructorProfileEntity?.id, instructorProfileEntity?.company, instructorProfileEntity?.year)
            )
            
        } else{
            throw Seminar400("오류")
        }
        
    }
    
    fun updateProfile(user: UpdateProfileRequest, token: String): GetProfile{
        //todo: email 못찾았으면 예외 제공
        //todo: year 음수이면 예외 제공
        
        val userEntity = userRepository.findByEmail(authTokenService.getCurrentEmail(token))

        
        if(userEntity.participant != null && userEntity.instructor == null){
            val participantProfileEntity = participantProfileRepository.findByEmailParticipant(authTokenService.getCurrentEmail(token))

            userEntity.let {
                it.username = user.username
                it.password = user.password
                it.participant?.university = user.participant?.university.toString()

            }
            participantProfileEntity.let {
                it.university = user.participant?.university ?: ""
            }
            userRepository.save(userEntity)
            participantProfileRepository.save(participantProfileEntity)
            
            return GetProfile(
                userEntity.id,
                userEntity.username,
                userEntity.email,
                userEntity.lastLogin,
                userEntity.dateJoined,
                GetProfileParticipantDto(participantProfileEntity.id,participantProfileEntity.university, participantProfileEntity.isRegistered),
                null)
        } else if(userEntity.participant == null && userEntity.instructor != null){
            val instructorProfileEntity = instructorProfileRepository.findByEmailInstructor(authTokenService.getCurrentEmail(token))
           
            userEntity.let {
                it.username = user.username
                it.password = user.password
                it.instructor?.company = user.instructor?.company.toString()
                it.instructor?.year = user.instructor?.year
            }
            instructorProfileEntity.let { 
                it.company = user.instructor?.company ?: ""
                it.year = user.instructor?.year 
            }
            userRepository.save(userEntity)
            instructorProfileRepository.save(instructorProfileEntity)

            return GetProfile(
                userEntity.id,
                userEntity.username,
                userEntity.email,
                userEntity.lastLogin,
                userEntity.dateJoined,
               null,
                GetProfileInstructorDto(instructorProfileEntity.id, instructorProfileEntity.company, instructorProfileEntity.year)
            )
        } else if(userEntity.participant != null && userEntity.instructor != null){
            val participantProfileEntity = participantProfileRepository.findByEmailParticipant(authTokenService.getCurrentEmail(token))
            val instructorProfileEntity = instructorProfileRepository.findByEmailInstructor(authTokenService.getCurrentEmail(token))
            userEntity.let {
                it.username = user.username
                it.password = user.password
                it.participant?.university = user.participant?.university.toString()
                it.instructor?.company = user.instructor?.company.toString()
                it.instructor?.year = user.instructor?.year
            }
            participantProfileEntity.let {
                it.university = user.participant?.university ?: ""
            }
            instructorProfileEntity.let {
                it.company = user.instructor?.company ?: ""
                it.year = user.instructor?.year
            }
            userRepository.save(userEntity)
            participantProfileRepository.save(participantProfileEntity)
            instructorProfileRepository.save(instructorProfileEntity)
            
            return GetProfile(
                userEntity.id,
                userEntity.username,
                userEntity.email,
                userEntity.lastLogin,
                userEntity.dateJoined,
                GetProfileParticipantDto(participantProfileEntity.id,participantProfileEntity.university, participantProfileEntity.isRegistered),
                GetProfileInstructorDto(instructorProfileEntity.id, instructorProfileEntity.company, instructorProfileEntity.year)
            )
        } else{
            throw Seminar400("오류입니다")
        }
    }
    
    private fun UserProfile(user: UserEntity, token: String) = user.run {

        UpdateProfileRequest(
            id = authTokenService.getCurrentUserId(token),
            username = user.username,
            password = user.password,
          
            participant = UpdateParticipantProfileDto(user.participant?.university),
            instructor = UpdateInstructorProfileDto(user.instructor?.company, user.instructor?.year)
            
            
        )
    }

    
    private fun makeUserProfileDto(email: String, qUserEntity: QUserEntity, qParticipantProfileEntity: QParticipantProfileEntity?, qInstructorProfileEntity: QInstructorProfileEntity?):List<UserProfileDto>{
        return queryFactory.select(Projections.constructor(
            UserProfileDto::class.java,
            qUserEntity,
            qParticipantProfileEntity,
            qInstructorProfileEntity
        ))
            .from(qUserEntity)
            .leftJoin(qParticipantProfileEntity).on(qUserEntity.participant.id.eq(qParticipantProfileEntity?.id))
            .leftJoin(qInstructorProfileEntity).on(qUserEntity.instructor.id.eq(qInstructorProfileEntity?.id))
            .where(qUserEntity.email.eq(email))
            .fetch()
    }
  
}