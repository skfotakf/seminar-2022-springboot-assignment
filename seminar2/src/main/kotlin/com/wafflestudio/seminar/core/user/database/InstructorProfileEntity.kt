package com.wafflestudio.seminar.core.user.database

import com.wafflestudio.seminar.common.BaseTimeEntity
import javax.persistence.*

@Entity
@Table(name="InstructorProfile")
class InstructorProfileEntity(
    
    @Column
    var emailInstructor: String,
    
    @Column
    var company: String = "",
    
    @Column
    var year: Int? = null,

   
) :BaseTimeEntity(){
}