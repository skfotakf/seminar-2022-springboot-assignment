package com.wafflestudio.seminar.core.user.api

import com.wafflestudio.seminar.common.Authenticated
import com.wafflestudio.seminar.core.user.api.request.*
import com.wafflestudio.seminar.core.user.api.response.*
import com.wafflestudio.seminar.core.user.service.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class Controller(

    private var authService: AuthService,
    private var authTokenService: AuthTokenService,
    private var userService: UserService,
    private var seminarService: SeminarService
) {
    
    @PostMapping("signup")
    fun signup(@RequestBody user: SignUpRequest) : AuthToken {
        
        authService.signup(user)

       
        return authTokenService.generateTokenByEmail(user.email)
    }
    
    @PostMapping("login")
    fun login(@RequestBody userLogin: LoginRequest) : AuthToken {
        
      return  authService.login(userLogin)
    }
    
    @Authenticated
    @GetMapping("user/{user_id}")
    fun getProfile(@PathVariable user_id: Long, @RequestHeader("Authentication") token: String): GetProfile {
        
        return userService.getProfile(user_id,token)

    }
    
    @Authenticated
    @PutMapping("user/me")
    fun updateProfile(@RequestBody userProfile: UpdateProfileRequest, @RequestHeader("Authentication") token: String): GetProfile{
        return userService.updateProfile(userProfile, token)
    }

    @PostMapping("user/participant")
    fun beParticipant(@RequestBody participant: BeParticipantRequest, @RequestHeader("Authentication") token: String): GetProfile{
        return userService.beParticipant(participant, token)
    }
    
    @PostMapping("seminar")
    fun createSeminar(@RequestBody seminar: SeminarRequest, @RequestHeader("Authentication") token: String): GetSeminarInfo {
        
        return seminarService.createSeminar(seminar, token)
    }

    @PutMapping("seminar")
    fun updateSeminar(@RequestBody seminar: SeminarRequest, @RequestHeader("Authentication") token: String): UpdateSeminarInfo {

        return seminarService.updateSeminar(seminar, token)
    }
    
    @GetMapping("seminar/{seminar_id}")
    fun getSeminarById(@PathVariable seminar_id: Long, @RequestHeader("Authentication") token: String):GetSeminarInfo{
        return seminarService.getSeminarById(seminar_id,token)
    }
    

    /*
    * 해당하는 api가 없습니다.
    * seminars -> seminar로 수정되어야 합니다.
    */
    @GetMapping("seminars")
    fun getSeminars(@RequestHeader("Authentication") token: String): List<GetSeminars>{
        return seminarService.getSeminars(token)
    }
    
    @GetMapping("seminar")
    fun getSeminarByName(@RequestParam name: String, @RequestParam order: String, @RequestHeader("Authentication") token: String): GetSeminarInfoByName {
        return seminarService.getSeminarByName(name, order, token)
    }
    
    @PostMapping("seminar/{seminar_id}/user")
    fun joinSeminar(@PathVariable seminar_id: Long, @RequestBody role: Map<String,String>, @RequestHeader("Authentication") token: String): JoinSeminarInfo{
        
        return seminarService.joinSeminar(seminar_id,role, token)
    }
    
    @DeleteMapping("seminar/{seminar_id}/user")
    fun dropSeminar(@PathVariable seminar_id: Long,@RequestHeader("Authentication") token: String): GetSeminarInfo {
        return seminarService.dropSeminar(seminar_id,token)
        
    }
    
     
    
    

}