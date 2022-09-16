package com.wafflestudio.seminar.survey.database

import com.wafflestudio.seminar.survey.domain.SurveyResponse
import org.springframework.stereotype.Repository

interface SurveyResponseRepository {
   fun findAll(): List<SurveyResponse>
   fun findById(id: Long): SurveyResponse
}

 