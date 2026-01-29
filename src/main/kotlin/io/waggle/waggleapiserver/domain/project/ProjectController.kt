package io.waggle.waggleapiserver.domain.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.CurrentUser
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationCreateRequest
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.member.service.MemberService
import io.waggle.waggleapiserver.domain.project.dto.request.ProjectUpsertRequest
import io.waggle.waggleapiserver.domain.project.dto.response.ProjectDetailResponse
import io.waggle.waggleapiserver.domain.project.service.ProjectService
import io.waggle.waggleapiserver.domain.recruitment.dto.request.RecruitmentUpsertRequest
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.recruitment.service.RecruitmentService
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

@Tag(name = "프로젝트")
@RequestMapping("/projects")
@RestController
class ProjectController(
    private val applicationService: ApplicationService,
    private val memberService: MemberService,
    private val projectService: ProjectService,
    private val recruitmentService: RecruitmentService,
) {
    @Operation(summary = "프로젝트 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProject(
        @Valid @RequestBody request: ProjectUpsertRequest,
        @CurrentUser user: User,
    ): ProjectDetailResponse = projectService.createProject(request, user)

    @Operation(
        summary = "프로젝트 지원",
        description = "사용자가 해당 프로젝트 합류를 지원함",
    )
    @PostMapping("/{projectId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    fun applyProject(
        @PathVariable projectId: Long,
        @Valid @RequestBody request: ApplicationCreateRequest,
        @CurrentUser user: User,
    ): ApplicationResponse = applicationService.applyProject(projectId, request, user)

    @Operation(
        summary = "프로젝트 모집 정보 생성",
        description = "프로젝트의 직무별 모집 목록을 생성함",
    )
    @PostMapping("/{projectId}/recruitments")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProjectRecruitments(
        @PathVariable projectId: Long,
        @Valid @RequestBody request: List<RecruitmentUpsertRequest>,
        @CurrentUser user: User,
    ): List<RecruitmentResponse> = recruitmentService.createRecruitments(projectId, request, user)

    @Operation(summary = "프로젝트 상세 조회")
    @GetMapping("/{projectId}")
    fun getProject(
        @PathVariable projectId: Long,
    ): ProjectDetailResponse = projectService.getProject(projectId)

    @Operation(
        summary = "프로젝트 지원 목록 조회",
        description = "프로젝트 멤버 권한 사용자가 프로젝트 지원 목록을 조회함",
    )
    @GetMapping("/{projectId}/applications")
    fun getProjectApplications(
        @PathVariable projectId: Long,
        @CurrentUser user: User,
    ): List<ApplicationResponse> = applicationService.getProjectApplications(projectId, user)

    @Operation(summary = "프로젝트 수정")
    @PutMapping("/{projectId}")
    fun updateProject(
        @PathVariable projectId: Long,
        @Valid @RequestBody request: ProjectUpsertRequest,
        @CurrentUser user: User,
    ): ProjectDetailResponse = projectService.updateProject(projectId, request, user)

    @Operation(summary = "프로젝트 삭제")
    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProject(
        @PathVariable projectId: Long,
        @CurrentUser user: User,
    ) {
        projectService.deleteProject(projectId, user)
    }

    @Operation(
        summary = "프로젝트 이탈",
        description = "멤버가 본인 혼자일 때는 이탈 불가, 본인이 리더일 때는 리더 위임 후 이탈",
    )
    @DeleteMapping("/{projectId}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leaveProject(
        @PathVariable projectId: Long,
        @CurrentUser user: User,
    ) {
        memberService.leaveProject(projectId, user)
    }
}
