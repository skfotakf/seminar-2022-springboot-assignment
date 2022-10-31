package com.wafflestudio.seminar.core.user.service

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.wafflestudio.seminar.common.Seminar400
import com.wafflestudio.seminar.common.Seminar403
import com.wafflestudio.seminar.common.Seminar404
import com.wafflestudio.seminar.core.user.api.request.SeminarRequest
import com.wafflestudio.seminar.core.user.api.response.*
import com.wafflestudio.seminar.core.user.database.SeminarRepository
import com.wafflestudio.seminar.core.user.database.UserRepository
import com.wafflestudio.seminar.core.user.database.UserSeminarRepository
import com.wafflestudio.seminar.core.user.domain.*
import com.wafflestudio.seminar.core.user.dto.seminar.*
import org.springframework.stereotype.Service
import java.time.LocalDateTime


@Service
class SeminarService(
    private val authTokenService: AuthTokenService,
    private val seminarRepository: SeminarRepository,
    private val userSeminarRepository: UserSeminarRepository,
    private val userRepository: UserRepository,
    private val queryFactory: JPAQueryFactory,
) {


    private final val qSeminarEntity: QSeminarEntity = QSeminarEntity.seminarEntity
    private final val qUserSeminarEntity: QUserSeminarEntity = QUserSeminarEntity.userSeminarEntity
    private final val qUserEntity: QUserEntity = QUserEntity.userEntity

    
    
    fun createSeminar(seminar: SeminarRequest, token: String): GetSeminarInfo {
        //todo: online 여부 외에는 하나라도 빠지면 400으로 응답하며, 적절한 에러 메시지를 포함합니다.
        //todo: name에 0글자가 오는 경우, capacity와 count에 양의 정수가 아닌 값이 오는 경우는 400으로 응답합니다.
        //todo: 세미나 진행자 자격을 가진 User만 요청할 수 있으며, 그렇지 않은 경우 403으로 응답
        if(seminar.name == null || seminar.capacity == null || seminar.count == null || seminar.time == null ) {
            throw Seminar400("입력하지 않은 값이 있습니다")
            
            
        } else {
            if(seminar.name == "" || seminar.capacity <= 0 || seminar.count <= 0) {
                throw Seminar400("형식에 맞지 않게 입력하지 않은 값이 있습니다")
            }
        }
        
        if(userRepository.findByEmail(authTokenService.getCurrentEmail(token)).instructor == null) {
            throw Seminar403("진행자만 세미나를 생성할 수 있습니다")
        }
        
        
        val saveSeminarEntity = seminarRepository.save(SeminarEntity(seminar, token))
        userSeminarRepository.save(userSeminarInstructorEntity(seminar, token))

        val seminarInfoDto = queryFactory.select(Projections.constructor(
            SeminarInfoDto::class.java,
            qSeminarEntity,
            qUserSeminarEntity,
            qUserEntity
        ))
            .from(qSeminarEntity)
            .innerJoin(qUserSeminarEntity).on(qSeminarEntity.id.eq(qUserSeminarEntity.seminar.id))
            .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id)).where(qSeminarEntity.id.eq(saveSeminarEntity.id))
            .where(qUserSeminarEntity.role.eq("instructor")).fetch()
        

        val seminarEntity = seminarInfoDto[0].seminarEntity
        val userSeminarEntity = seminarInfoDto[0].userSeminarEntity
        val userEntity = seminarInfoDto[0].userEntity

        val studentList = queryFactory.select(Projections.constructor(
            SeminarInfoDto::class.java,
            qSeminarEntity,
            qUserSeminarEntity,
            qUserEntity
        ))
            .from(qSeminarEntity)
            .innerJoin(qUserSeminarEntity).on(qSeminarEntity.id.eq(qUserSeminarEntity.seminar.id))
            .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id)).where(qSeminarEntity.name.eq(seminar.name)).where(qUserSeminarEntity.role.eq("participant")).fetch()

        val newList = mutableListOf<StudentDto>()

        for(i in 0 until studentList.size){
            val studentEntity = studentList[i].userEntity
            val studentSeminarEntity = studentList[i].userSeminarEntity
            newList.add(
                StudentDto(
                    studentEntity?.id,
                    studentEntity?.username,
                    studentEntity?.email,
                    studentSeminarEntity?.joinedAt,
                    studentSeminarEntity?.isActive,
                    studentSeminarEntity?.droppedAt

                )
            )
        }
        
        return GetSeminarInfo(
            seminarEntity?.id,
            seminarEntity?.name,
            seminarEntity?.capacity,
            seminarEntity?.count,
            seminarEntity?.time,
            seminarEntity?.online,
            List(seminarInfoDto.size) { _ ->
                TeacherDto(
                    userEntity?.id,
                    userEntity?.username,
                    userEntity?.email,
                    userSeminarEntity?.joinedAt
                )

            },newList

        )


    }
    
    fun updateSeminar(seminar: SeminarRequest, token: String): UpdateSeminarInfo {

        if(seminar.name == null || seminar.capacity == null || seminar.count == null || seminar.time == null ) {
            throw Seminar400("입력하지 않은 값이 있습니다")


        } else {
            if(seminar.name == "" || seminar.capacity <= 0 || seminar.count <= 0) {
                throw Seminar400("형식에 맞지 않게 입력하지 않은 값이 있습니다")
            }
        }

        if(userRepository.findByEmail(authTokenService.getCurrentEmail(token)).instructor == null) {
            throw Seminar403("진행자만 세미나를 생성할 수 있습니다")
        }
        
        val seminarEntity = seminarRepository.findByName(seminar.name)
        
        seminarEntity.let { 
            it.name = seminar.name
            it.capacity = seminar.capacity
            it.count = seminar.count
            it.time = seminar.time
            it.online = seminar.online
        }
        
        seminarRepository.save(seminarEntity)
        
        return UpdateSeminarInfo(
            seminarEntity.id,
            seminarEntity.name,
            seminarEntity.capacity,
            seminarEntity.count,
            seminarEntity.time,
            seminarEntity.online,
            
        )
    }


    fun getSeminarById(id: Long, token: String):GetSeminarInfo{

        val seminarList = queryFactory.select(Projections.constructor(
            SeminarDto::class.java,
            qSeminarEntity
        ))
            .from(qSeminarEntity)
            .innerJoin(qUserSeminarEntity).on(qSeminarEntity.id.eq(qUserSeminarEntity.seminar.id))
            .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id))
            .where(qSeminarEntity.id.eq(id)).fetch()
        
        val seminarEntity = seminarList[0].seminarEntity

        val instructorList = queryFactory.select(Projections.constructor(
            UserSeminarAndUserDto::class.java,
            qUserSeminarEntity,
            qUserEntity
        ))
            .from(qUserSeminarEntity)
            .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id))
            .where(qUserSeminarEntity.seminar.id.eq(id))
            .where(qUserSeminarEntity.role.eq("instructor")).fetch()

        val teacherList = mutableListOf<TeacherDto>()

        for(i in 0 until instructorList.size){
            val teacherEntity = instructorList[i].userEntity
            val teacherSeminarEntity = instructorList[i].userSeminarEntity
            teacherList.add(
                TeacherDto(
                    teacherEntity?.id,
                    teacherEntity?.username,
                    teacherEntity?.email,
                    teacherSeminarEntity?.joinedAt

                )
            )
        }
        
        val participantList = queryFactory.select(Projections.constructor(
            SeminarInfoDto::class.java,
            qSeminarEntity,
            qUserSeminarEntity,
            qUserEntity
        ))
            .from(qSeminarEntity)
            .innerJoin(qUserSeminarEntity).on(qSeminarEntity.id.eq(qUserSeminarEntity.seminar.id))
            .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id)).where(qSeminarEntity.id.eq(id))
            .where(qUserSeminarEntity.role.eq("participant")).fetch()

        
        
        val studentList = mutableListOf<StudentDto>()

        for(i in 0 until participantList.size){
            val studentEntity = participantList[i].userEntity
            val studentSeminarEntity = participantList[i].userSeminarEntity
            studentList.add(
                StudentDto(
                    studentEntity?.id,
                    studentEntity?.username,
                    studentEntity?.email,
                    studentSeminarEntity?.joinedAt,
                    studentSeminarEntity?.isActive,
                    studentSeminarEntity?.droppedAt

                )
            )
        }
        return GetSeminarInfo(
            seminarEntity?.id,
            seminarEntity?.name,
            seminarEntity?.capacity,
            seminarEntity?.count,
            seminarEntity?.time,
            seminarEntity?.online,
            teacherList,
            studentList

        )

    }


 
    
    fun getSeminars(token: String): List<GetSeminars> {

        val seminarList = queryFactory.select(Projections.constructor(
            SeminarDto::class.java,
            qSeminarEntity
        ))
            .from(qSeminarEntity)
            .fetch()
        
        val seminars = mutableListOf<GetSeminars>()

        for(i in 0 until seminarList.size) {
            val seminarEntity = seminarList[i].seminarEntity
            

            seminars.add(
                GetSeminars(
                    seminarEntity?.id,
                    seminarEntity?.name,
                    seminarEntity?.capacity,
                    seminarEntity?.count,
                    seminarEntity?.time,
                    seminarEntity?.online
                )
            )
        }
        return seminars
       
        
    }
    
     

    fun getSeminarByName(name: String, order: String, token: String):GetSeminarInfoByName{
        val seminarInfoDto : List<SeminarInfoDto>
        
        if(order=="earliest") {
            seminarInfoDto = queryFactory.select(Projections.constructor(
                SeminarInfoDto::class.java,
                qSeminarEntity,
                qUserSeminarEntity,
                qUserEntity
            ))
                .from(qSeminarEntity)
                .innerJoin(qUserSeminarEntity).on(qSeminarEntity.id.eq(qUserSeminarEntity.seminar.id))
                .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id)).where(qSeminarEntity.name.contains(name))
                .orderBy(qUserSeminarEntity.joinedAt.asc()).fetch()

            val seminarEntity = seminarInfoDto[0].seminarEntity
            val userSeminarEntity = seminarInfoDto[0].userSeminarEntity
            val userEntity = seminarInfoDto[0].userEntity

            val studentList = queryFactory.select(Projections.constructor(
                SeminarInfoDto::class.java,
                qSeminarEntity,
                qUserSeminarEntity,
                qUserEntity
            ))
                .from(qSeminarEntity)
                .innerJoin(qUserSeminarEntity).on(qSeminarEntity.id.eq(qUserSeminarEntity.seminar.id))
                .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id)).where(qSeminarEntity.name.contains(name))
                .where(qUserSeminarEntity.role.eq("participant")).fetch()


            val newList = mutableListOf<StudentDto>()

            for(i in 0 until studentList.size){
                val studentEntity = studentList[i].userEntity
                val studentSeminarEntity = studentList[i].userSeminarEntity
                newList.add(
                    StudentDto(
                        studentEntity?.id,
                        studentEntity?.username,
                        studentEntity?.email,
                        studentSeminarEntity?.joinedAt,
                        studentSeminarEntity?.isActive,
                        studentSeminarEntity?.droppedAt

                    )
                )
            }
            
            return GetSeminarInfoByName(
                seminarEntity?.id,
                seminarEntity?.name,
                List(seminarInfoDto.size) { _ ->
                    TeacherDto(
                        userEntity?.id,
                        userEntity?.username,
                        userEntity?.email,
                        userSeminarEntity?.joinedAt
                    )

                }.distinct()
                ,newList.filter { it.isActive ==true }.size
            )
        } else {
            seminarInfoDto = queryFactory.select(Projections.constructor(
                SeminarInfoDto::class.java,
                qSeminarEntity,
                qUserSeminarEntity,
                qUserEntity
            ))
                .from(qSeminarEntity)
                .innerJoin(qUserSeminarEntity).on(qSeminarEntity.id.eq(qUserSeminarEntity.seminar.id))
                .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id)).where(qSeminarEntity.name.contains(name)).orderBy(qUserSeminarEntity.joinedAt.desc()).fetch()

            val seminarEntity = seminarInfoDto[0].seminarEntity
            val userSeminarEntity = seminarInfoDto[0].userSeminarEntity
            val userEntity = seminarInfoDto[0].userEntity

            val studentList = queryFactory.select(Projections.constructor(
                SeminarInfoDto::class.java,
                qSeminarEntity,
                qUserSeminarEntity,
                qUserEntity
            ))
                .from(qSeminarEntity)
                .innerJoin(qUserSeminarEntity).on(qSeminarEntity.id.eq(qUserSeminarEntity.seminar.id))
                .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id)).where(qSeminarEntity.name.contains(name)).where(qUserSeminarEntity.role.eq("participant")).fetch()

            val newList = mutableListOf<StudentDto>()

            for(i in 0 until studentList.size){
                val studentEntity = studentList[i].userEntity
                val studentSeminarEntity = studentList[i].userSeminarEntity
                newList.add(
                    StudentDto(
                        studentEntity?.id,
                        studentEntity?.username,
                        studentEntity?.email,
                        studentSeminarEntity?.joinedAt,
                        studentSeminarEntity?.isActive,
                        studentSeminarEntity?.droppedAt

                    )
                )
            }
            
            return GetSeminarInfoByName(
                seminarEntity?.id,
                seminarEntity?.name,
                List(seminarInfoDto.size) { _ ->
                    TeacherDto(
                        userEntity?.id,
                        userEntity?.username,
                        userEntity?.email,
                        userSeminarEntity?.joinedAt
                    )

                }.distinct(),
                newList.filter { it.isActive ==true }.size

            )
        }

    }


 
    fun joinSeminar(id: Long, role: Map<String, String>, token: String): JoinSeminarInfo {
        
        val seminarFindByIdEntity = seminarRepository.findById(id)
        
        val userFindByIdEntity = userRepository.findById(authTokenService.getCurrentUserId(token))

        
        if(userSeminarRepository.findByUser(userFindByIdEntity.get())?.filter { 
            it.seminar.id == id && it.isActive == true
                
            } != emptyList<UserSeminarEntity>()) {
            
            throw Seminar400("이미 세미나에 참여하고 있습니다")
        }
        if(seminarFindByIdEntity.isEmpty) {
            
            throw Seminar404("해당하는 세미나가 없습니다.")
            
        }
        if(userFindByIdEntity.get().participant != null) {
            
            if(userFindByIdEntity.get().participant?.isRegistered == false) {
                throw Seminar400("등록되어 있지 않습니다")
            }
        }

        if(userSeminarRepository.findByUser(userFindByIdEntity.get())?.filter {
                it.isActive == false
            } != emptyList<UserSeminarEntity>()) {

            throw Seminar400("드랍한 세미나는 다시 신청할 수 없습니다")
        }
        val saveUserSeminarEntity : UserSeminarEntity
        
        if(role["role"] == "participant"){
            
            if(userFindByIdEntity.get().participant != null) {
                saveUserSeminarEntity = userSeminarRepository.save(
                    UserSeminarEntity(
                        user = userRepository.findById(authTokenService.getCurrentUserId(token)).get(),
                        seminar = seminarRepository.findById(id).get(),
                        role = "participant",
                        joinedAt = LocalDateTime.now(),
                        isActive = true,
                        droppedAt = null
                    )
                )
                
            } else {
                
                throw Seminar403("수강생이 아닙니다")
            }
            
        } else if (role["role"] == "instructor") {
            if(userFindByIdEntity.get().instructor != null){
                if(userSeminarRepository.findByUser(userFindByIdEntity.get()) == emptyList<UserSeminarEntity>()) {
                    saveUserSeminarEntity = userSeminarRepository.save(
                        UserSeminarEntity(
                            user = userRepository.findById(authTokenService.getCurrentUserId(token)).get(),
                            seminar = seminarRepository.findById(id).get(),
                            role = "instructor",
                            joinedAt = LocalDateTime.now(),
                            isActive = null,
                            droppedAt = null
                        )
                    ) 
                } else {
                    throw Seminar400("이미 다른 세미나를 진행하고 있습니다.")
                }
                
            } else {
                throw Seminar403("진행자가 아닙니다")
            }
            
        } else {
            throw Seminar400("진행자 혹은 수강자가 아닙니다.")
        }

        val seminarList = queryFactory.select(Projections.constructor(
            SeminarDto::class.java,
            qSeminarEntity
        ))
            .from(qSeminarEntity)
            .where(qSeminarEntity.id.eq(id)).fetch()

        val seminarEntity = seminarList[0].seminarEntity
        
        val instructorList = queryFactory.select(Projections.constructor(
            UserSeminarAndUserDto::class.java,
            qUserSeminarEntity,
            qUserEntity
        ))
            .from(qUserSeminarEntity)
            .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id))
            .where(qUserSeminarEntity.seminar.id.eq(id))
            .where(qUserSeminarEntity.role.eq("instructor")).fetch()

        
        val teacherSeminarEntity = instructorList[0].userSeminarEntity
        val teacherEntity = instructorList[0].userEntity

        val teacherList = mutableListOf<TeacherDto>()

        for(i in 0 until instructorList.size){
            val teacherEntity = instructorList[i].userEntity
            val teacherSeminarEntity = instructorList[i].userSeminarEntity
            teacherList.add(
                TeacherDto(
                    teacherEntity?.id,
                    teacherEntity?.username,
                    teacherEntity?.email,
                    teacherSeminarEntity?.joinedAt

                )
            )
        }
        
        val participantList = queryFactory.select(Projections.constructor(
            SeminarInfoDto::class.java,
            qSeminarEntity,
            qUserSeminarEntity,
            qUserEntity
        ))
            .from(qSeminarEntity)
            .innerJoin(qUserSeminarEntity).on(qSeminarEntity.id.eq(qUserSeminarEntity.seminar.id))
            .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id)).where(qSeminarEntity.id.eq(id))
            .where(qUserSeminarEntity.role.eq("participant")).fetch()


        val studentList = mutableListOf<StudentDto>()
        
        for(i in 0 until participantList.size){
            val studentEntity = participantList[i].userEntity
            val studentSeminarEntity = participantList[i].userSeminarEntity
            studentList.add(
                StudentDto(
                    studentEntity?.id,
                    studentEntity?.username,
                    studentEntity?.email,
                    studentSeminarEntity?.joinedAt,
                    studentSeminarEntity?.isActive,
                    studentSeminarEntity?.droppedAt
                    
                )
            )
        }
        
        if(studentList.size > seminarRepository.findById(id).get().capacity!!) {
                userSeminarRepository.delete(saveUserSeminarEntity)
                throw Seminar400("세미나의 인원이 다 찼습니다")
        }
        
        return JoinSeminarInfo(
            id = seminarRepository.findById(id).get().id,
            name = seminarRepository.findById(id).get().name,
            capacity = seminarRepository.findById(id).get().capacity,
            count = seminarRepository.findById(id).get().count,
            time = seminarRepository.findById(id).get().time,
            online = seminarRepository.findById(id).get().online,
            instructors = teacherList,
            
            participants = studentList
        )
    }
    
    fun dropSeminar(id: Long, token: String) : GetSeminarInfo{
        val findByEmailEntity = userRepository.findByEmail(authTokenService.getCurrentEmail(token))

        if(userSeminarRepository.findByUser(findByEmailEntity)?.filter { 
            it.seminar.id == id
            } == emptyList<UserSeminarEntity>()){
            throw Seminar404("해당 세미나를 신청한 적이 없습니다")
        }
        
        if(userSeminarRepository.findByUser(findByEmailEntity)?.filter { 
            it.role == "instructor"
            } != emptyList<UserSeminarEntity>()){
            throw Seminar403("진행자는 세미나를 드랍할 수 없습니다")
        }
        val seminarInfoDto = queryFactory.select(Projections.constructor(
            SeminarInfoDto::class.java,
            qSeminarEntity,
            qUserSeminarEntity,
            qUserEntity
        ))
            .from(qSeminarEntity)
            .innerJoin(qUserSeminarEntity).on(qSeminarEntity.id.eq(qUserSeminarEntity.seminar.id))
            .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id))
            .where(qSeminarEntity.id.eq(id))
            .where(qUserEntity.email.eq(authTokenService.getCurrentEmail(token))).fetch()

        val seminarEntity = seminarInfoDto[0].seminarEntity
        var userSeminarEntity = seminarInfoDto[0].userSeminarEntity
        val userEntity = seminarInfoDto[0].userEntity
        
        userSeminarEntity?.isActive = false
        userSeminarEntity?.droppedAt = LocalDateTime.now()
        userSeminarEntity?.let { userSeminarRepository.save(it) }
        val studentList = queryFactory.select(Projections.constructor(
            SeminarInfoDto::class.java,
            qSeminarEntity,
            qUserSeminarEntity,
            qUserEntity
        ))
            .from(qSeminarEntity)
            .innerJoin(qUserSeminarEntity).on(qSeminarEntity.id.eq(qUserSeminarEntity.seminar.id))
            .innerJoin(qUserEntity).on(qUserSeminarEntity.user.id.eq(qUserEntity.id)).where(qSeminarEntity.id.eq(id)).where(qUserSeminarEntity.role.eq("participant")).fetch()

        val newList = mutableListOf<StudentDto>()

        for(i in 0 until studentList.size){
            val studentEntity = studentList[i].userEntity
            val studentSeminarEntity = studentList[i].userSeminarEntity
            newList.add(
                StudentDto(
                    studentEntity?.id,
                    studentEntity?.username,
                    studentEntity?.email,
                    studentSeminarEntity?.joinedAt,
                    studentSeminarEntity?.isActive,
                    studentSeminarEntity?.droppedAt

                )
            )
        }

        return GetSeminarInfo(
            seminarEntity?.id,
            seminarEntity?.name,
            seminarEntity?.capacity,
            seminarEntity?.count,
            seminarEntity?.time,
            seminarEntity?.online,
            List(seminarInfoDto.size) { _ ->
                TeacherDto(
                    userEntity?.id,
                    userEntity?.username,
                    userEntity?.email,
                    userSeminarEntity?.joinedAt
                )

            },newList

        )
    }
    
    
     
     
    
    private fun SeminarEntity(seminar: SeminarRequest, token: String) = seminar.run{
        com.wafflestudio.seminar.core.user.domain.SeminarEntity(

            name = seminar.name,
            capacity = seminar.capacity,
            count = seminar.count,
            time = seminar.time,//LocalTime.parse(seminar.time, DateTimeFormatter.ISO_TIME),
            online = true,
          

        )
    }

    private fun userSeminarInstructorEntity(seminar: SeminarRequest, token: String) : UserSeminarEntity {
        return UserSeminarEntity(
            user = userRepository.findByEmail(authTokenService.getCurrentEmail(token)),
            seminar= seminarRepository.findByName(seminar.name),
            role = "instructor",
            joinedAt = LocalDateTime.now(),
            isActive = true,
            droppedAt = null
        )
    }

   

}
