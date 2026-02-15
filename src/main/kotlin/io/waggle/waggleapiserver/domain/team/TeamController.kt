package io.waggle.waggleapiserver.domain.team

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.CurrentUser
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationCreateRequest
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.member.service.MemberService
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.recruitment.service.RecruitmentService
import io.waggle.waggleapiserver.domain.team.dto.request.TeamUpsertRequest
import io.waggle.waggleapiserver.domain.team.dto.response.TeamDetailResponse
import io.waggle.waggleapiserver.domain.team.service.TeamService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "팀")
@RequestMapping("/teams")
@RestController
class TeamController(
    private val applicationService: ApplicationService,
    private val memberService: MemberService,
    private val teamService: TeamService,
    private val recruitmentService: RecruitmentService,
) {
    @Operation(summary = "팀 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTeam(
        @Valid @RequestBody request: TeamUpsertRequest,
        @CurrentUser user: User,
    ): TeamDetailResponse = teamService.createTeam(request, user)

    @Operation(
        summary = "팀 지원",
        description = "사용자가 해당 팀 합류를 지원함",
    )
    @PostMapping("/{teamId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    fun applyToTeam(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: ApplicationCreateRequest,
        @CurrentUser user: User,
    ): ApplicationResponse = applicationService.applyToTeam(teamId, request, user)

    @Operation(
        summary = "팀 모집 정보 생성",
        description = "팀의 직무별 모집 목록을 생성함",
    )
    @PostMapping("/{teamId}/recruitments")
    @ResponseStatus(HttpStatus.CREATED)
    fun createTeamRecruitments(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: List<RecruitmentUpsertRequest>,
        @CurrentUser user: User,
    ): List<RecruitmentResponse> = recruitmentService.createRecruitments(teamId, request, user)

    @Operation(summary = "팀 상세 조회")
    @GetMapping("/{teamId}")
    fun getTeam(
        @PathVariable teamId: Long,
    ): TeamDetailResponse = teamService.getTeam(teamId)

    @Operation(
        summary = "팀 지원 목록 조회",
        description = "팀 멤버 권한 사용자가 팀 지원 목록을 조회함",
    )
    @GetMapping("/{teamId}/applications")
    fun getTeamApplications(
        @PathVariable teamId: Long,
        @CurrentUser user: User,
    ): List<ApplicationResponse> = applicationService.getTeamApplications(teamId, user)

    @Operation(summary = "팀 수정")
    @PutMapping("/{teamId}")
    fun updateTeam(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: TeamUpsertRequest,
        @CurrentUser user: User,
    ): TeamDetailResponse = teamService.updateTeam(teamId, request, user)

    @Operation(summary = "팀 삭제")
    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTeam(
        @PathVariable teamId: Long,
        @CurrentUser user: User,
    ) {
        teamService.deleteTeam(teamId, user)
    }

    @Operation(
        summary = "팀 이탈",
        description = "멤버가 본인 혼자일 때는 이탈 불가, 본인이 리더일 때는 리더 위임 후 이탈",
    )
    @DeleteMapping("/{teamId}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leaveTeam(
        @PathVariable teamId: Long,
        @CurrentUser user: User,
    ) {
        memberService.leaveTeam(teamId, user)
    }
}
