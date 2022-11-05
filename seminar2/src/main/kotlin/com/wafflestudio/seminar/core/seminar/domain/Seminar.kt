package com.wafflestudio.seminar.core.seminar.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.wafflestudio.seminar.core.seminar.database.SeminarEntity
import java.time.LocalTime

data class Seminar(
    val id: Long,
    val name: String,
    @JsonFormat(pattern = "HH:mm")
    val time: LocalTime,
    val count: Int,
    val capacity: Int,
    val online: Boolean,
    val instructors: List<SeminarInstructor>,
    val participants: List<SeminarParticipant>,
) {
    companion object {
        fun of(
            entity: SeminarEntity,
            instructors: List<SeminarInstructor>,
            participants: List<SeminarParticipant>,
        ) = entity.run {
            Seminar(
                id = id,
                name = name,
                time = time,
                count = count,
                capacity = capacity,
                online = online,
                instructors = instructors,
                participants = participants,
            )
        }
    }
}