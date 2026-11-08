# 백엔드 개선 백로그

기존 코드와 설정에서 개선이 필요한 항목을 우선순위별로 정리한 문서다. 신규 기능은 다루지 않는다.

- 기준 커밋: `e46dab0` (`upstream/main`, PR #156 머지 시점)
- 항목마다 **확인** 칸에 검증 수준을 적었다. "실행 재현"은 테스트나 실제 서버로 결과를 직접 확인한 것, "코드 추적"은 호출 경로를 끝까지 따라가 확인했지만 실행해 보지는 않은 것이다
- 인프라 항목은 우선순위를 낮게 두었다. 활성 사용자가 100명이 안 되는 규모라 EC2 한 대에 모든 컨테이너를 올린 구성은 의도된 것이고, 항목마다 **다시 볼 시점**을 적었다
- 항목을 처리하면 그 PR에서 이 문서의 해당 항목을 지울 것

## 요약

| ID | 항목 | 우선순위 |
|---|---|---|
| U2 | 프로필, 팀 이미지 URL을 조작해 버킷의 다른 S3 객체를 지울 수 있음 | 긴급 |
| H1 | 요청 시작 때 읽은 `User`를 저장해 다른 변경을 덮어씀 | 높음 |
| H2 | 리프레시 토큰이 사용자당 1개라 다른 기기 로그인 시 기존 기기가 로그아웃됨 | 높음 |
| H3 | 같은 모집글에 다른 포지션으로 동시에 지원하면 중복 지원이 들어감 | 높음 |
| H4 | 지원서 승인, 거절, 취소, 수정이 동시에 들어오면 상태와 멤버십이 어긋남 | 높음 |
| H5 | 모집글 수정, 삭제 권한이 작성 당시 기준이라 팀을 떠난 작성자가 지울 수 있음 | 높음 |
| M1 | 정렬 값이 ID가 아닌 목록의 커서가 ID 하나뿐이라 목록이 끊기거나 건너뜀 | 중간 |
| M2 | `@CurrentUser` 요청마다 사용자와 약관을 조회하는 고정 쿼리 | 중간 |
| M3 | 메시지 대화 조회가 매번 filesort | 중간 |
| M4 | MySQL 8.0 지원 종료 | 중간 |
| M5 | 유니크 제약 경합이 500과 Discord 알림으로 처리됨 | 중간 |
| M6 | 재가입 시 팔로우 복원으로 팔로워, 팔로잉 목록이 404나 500이 됨 | 중간 |
| M7 | 리프레시 토큰을 액세스 토큰으로도 쓸 수 있음 | 중간 |
| M8 | 완료된 팀이 계속 지원을 받고, 상태를 바꿀 때마다 리뷰 요청이 반복됨 | 중간 |
| M10 | 이미지 URL 컬럼 길이가 엔티티와 마이그레이션에서 다르고 요청 길이 제한이 없음 | 중간 |
| M11 | Presigned URL 업로드에 크기 제한이 없고 고아 객체가 쌓임 | 중간 |
| L1 | 약관 미동의가 400(`INVALID_STATE`) | 낮음 |
| L2 | 멤버 역할 정렬이 enum 문자열 순서에 의존 | 낮음 |
| L3 | 메시지 검색어의 연산자 문자가 검색 결과를 바꿈 | 낮음 |
| L4 | 대화 검색 결과의 미리보기가 검색어와 무관한 마지막 메시지로 나옴 | 낮음 |
| L5 | 실시간 메시지 전달마다 쿼리 3개로 재조회 | 낮음 |
| L6 | 셋업 가드 로직이 두 곳에 중복 | 낮음 |
| L7 | 좋아요 카운터를 실제 행 수로 다시 맞출 수단이 없음 | 낮음 |
| L8 | 엔티티에 `@Version`이 없어 동시 수정이 서로 덮어씀 (멤버, 모집 마감) | 낮음 |
| L9 | 팀원 리뷰가 동시에 들어오면 협업 온도에 리뷰 하나가 빠짐 | 낮음 |
| L10 | 포지션 삭제와 지원이 겹치면 대응 모집 정보가 없는 지원서가 생김 | 낮음 |
| L11 | 동시 refresh 요청이 재사용 탐지에 걸려 로그아웃될 수 있음 | 낮음 |
| L12 | 로그아웃에 유효한 액세스 토큰이 필요함 | 낮음 |
| L13 | 탈퇴자 이메일이 중복 검사에서 빠져 같은 이메일의 계정이 둘이 될 수 있음 | 낮음 |
| L14 | 선택 약관(마케팅) 동의를 철회할 수 없음 | 낮음 |
| L15 | 탈퇴 후 개인정보 보존 기한이 없음 | 낮음 |
| L16 | `COOKIE_SECURE` 기본값이 `false` | 낮음 |
| L17 | 팀 모집글 목록의 지원자 수가 일반 멤버에게도 보임 | 낮음 |
| L18 | 모집 정보가 빈 목록이어도 모집글이 만들어짐 | 낮음 |
| L19 | 페이지네이션이 없는 목록 API | 낮음 |
| L20 | 탈퇴 뒤에도 열려 있던 WebSocket 세션으로 메시지를 보낼 수 있음 | 낮음 |
| L21 | 알림 보관 기한이 없고 읽음 처리 ID 목록에 상한이 없음 | 낮음 |
| L22 | 일부 cascade 삭제가 행을 전부 읽은 뒤 하나씩 지움 | 낮음 |
| L23 | 쓰이지 않는 인덱스 `idx_messages_receiver_read` | 낮음 |
| L24 | Spring MVC 예외 응답이 내부 클래스명을 노출하거나 원인을 버림 | 낮음 |
| L25 | 알림 생성이 실패하면 영구 유실 (현재는 유실 허용) | 낮음 |
| L26 | `Recruitment.status`의 `final`로 기동 시 Hibernate 경고 | 낮음 |
| L27 | 테스트가 통합 테스트 위주이고 테스트가 없는 기능이 있음 | 낮음 |
| L28 | 쓰이지 않는 코드, 설정, 의존성 | 낮음 |
| I1 | DB 백업이 EBS 스냅샷뿐이고 복원을 시험한 적이 없음 | 낮음 (인프라) |
| I2 | 앱이 응답하지 않아도 인스턴스가 살아 있으면 알 수 없음 | 낮음 (인프라) |
| I3 | 배포가 앱을 내린 뒤에 이미지를 빌드하고 동시 실행을 막지 않음 | 낮음 (인프라) |
| I4 | 앱 컨테이너에 JVM 메모리 설정과 healthcheck가 없음 | 낮음 (인프라) |
| I5 | SSH 포트가 전체 인터넷에 열려 있음 | 낮음 (인프라) |
| I6 | 고정 IP(EIP)가 없어 인스턴스를 재시작하면 공인 IP가 바뀜 | 낮음 (인프라) |
| R1 | 삭제 cascade가 동기 이벤트 리스너라 실행 순서가 코드에 드러나지 않음 | 참고 |
| R2 | 값 채우기와 코드 전환을 한 마이그레이션에 묶는 방식은 중단 배포 전제 | 참고 |

---

## 긴급

### U2. 프로필, 팀 이미지 URL을 조작해 버킷의 다른 S3 객체를 지울 수 있음

- **위치**: `WebUrlValidator`, `S3StorageClient.delete`, `UserService.updateUser`와 `deactivateUser`, `TeamService`의 팀 수정과 삭제
- **문제**
  - `@WebUrl`은 `http(s)` 스킴이고 호스트에 점이 있는지만 본다. 우리 버킷 주소인지 확인하지 않는다
  - 이미지를 바꾸거나 탈퇴, 팀 삭제를 하면 **이전** URL로 `ImageDeleteEvent`를 발행한다
  - `S3StorageClient.delete`는 URL의 호스트를 버리고 경로만 떼어 우리 버킷에서 그 키를 지운다
- **영향**: 로그인한 사용자 누구나 다른 사용자, 팀, 모집글의 이미지를 영구 삭제할 수 있다. 피해자의 공개 프로필에서 이미지 URL을 알아낸 뒤, 자기 프로필 이미지를 그 URL(또는 호스트만 바꾼 URL)로 설정했다가 다른 값으로 바꾸기만 하면 된다.
  - `waggle-server` 버킷은 버저닝이 켜져 있고 이전 버전을 30일간 보관한다. 지워진 이미지는 30일 안에 이전 버전으로 되돌릴 수 있지만, 알아차리지 못하면 그대로 사라진다
  - EC2 역할(`waggle-ec2-s3-role`)은 `waggle-server/*` 전체에 `s3:DeleteObject`를 가진다. 같은 버킷의 `local/` 경로까지 지울 수 있다
- **확인**: 코드 추적 (요청 DTO부터 검증, 이벤트, S3 삭제 호출까지). 버킷 설정과 역할 권한은 AWS 설정 조회로 확인. 실제 S3로 재현하지는 않음
- **권장 방향**
  - 저장 시점에 이미지 URL이 `S3_BASE_URL`과 허용 경로(`users/`, `teams/` 등)로 시작하는지 검증한다
  - 삭제 시점에도 `S3_BASE_URL`로 시작하지 않는 URL은 무시한다. OAuth 가입 때 저장된 Google, Kakao 외부 이미지 URL로 쓸데없는 삭제 요청이 나가는 것도 함께 막힌다
  - 가능하면 발급한 키를 사용자나 팀 ID와 묶어 검증한다
  - 역할의 `s3:DeleteObject`를 `waggle-server/prod/*`로 좁힌다

---

## 높음

### H1. 요청 시작 때 읽은 `User`를 저장해 다른 변경을 덮어씀

- **위치**: `UserService.setupProfile`, `updateUser`, `deactivateUser`의 `userRepository.save(user)`
- **문제**: `open-in-view: false`라 `@CurrentUser`로 받은 `User`는 `CurrentUserArgumentResolver`의 짧은 트랜잭션에서 읽힌 뒤 분리(detached)된 상태로 서비스에 들어온다. 분리된 엔티티를 `save()`하면 JPA가 `merge()`로 그 객체의 모든 필드를 DB에 덮어쓴다.
- **영향**: 요청이 처리되는 사이 다른 트랜잭션이 같은 사용자를 바꾸면 그 변경이 사라진다. 예) 프로필 수정 중 `MemberReviewService`가 협업 온도(`temperature`)를 갱신하면, 프로필 수정이 요청 시작 때 읽은 옛 온도로 되돌린다. 드물지만 에러 없이 생긴다.
- **원인**: 서비스가 엔티티를 받아 수정하는 방식은 OSIV가 켜져 있어야(요청 전체에서 영속성 컨텍스트 하나를 유지해야) 안전하다. 이 프로젝트는 커넥션을 요청 끝까지 붙잡지 않으려고 OSIV를 껐는데, 엔티티를 넘기는 방식은 그에 맞게 바뀌지 않았다.
- **OSIV를 다시 켜는 것은 해결책이 아니다.** 커넥션 풀 고갈과 컨트롤러, 직렬화 단계의 숨은 쿼리라는 더 큰 문제를 되살린다.
- **확인**: 코드 추적
- **권장 방향**: 둘 중 하나
  - 최소 수정: `User`를 수정하는 세 메서드만 트랜잭션 안에서 `userRepository.findByIdOrNull(user.id)`로 다시 읽어 수정하고 `save()`를 뺀다. 나머지 서비스는 받은 `User`를 읽기(ID, 권한 확인, 응답 DTO)에만 쓰므로 그대로 둬도 된다
  - 전면 정리: `@CurrentUser`가 엔티티 대신 `userId`나 가벼운 인증 객체(예: `AuthUser(id, role)`)를 주게 바꾸고, 서비스가 필요할 때 직접 조회한다. 서비스가 웹 계층에 묶이지 않는다. 다만 셋업 가드가 프로필 완성 여부를 보려면 사용자 조회가 필요해 M2의 쿼리가 완전히 사라지지는 않는다
- **참고**: M2의 `User` 컬렉션 LAZY 전환은 이 항목이 먼저 풀려야 가능하다. 분리된 `User`의 컬렉션을 서비스와 DTO가 직접 건드리기 때문이다 (`UserService.updateUser`가 부르는 `User.update`의 `skills.clear()`, 댓글과 모집글 작성 응답의 `UserSimpleResponse.from(user)` 등).

### H2. 리프레시 토큰이 사용자당 1개라 다른 기기 로그인 시 기존 기기가 로그아웃됨

- **위치**: `AuthService` (`refresh-token:$userId` 키)
- **문제**: Redis에 사용자당 리프레시 토큰을 하나만 저장하고, 저장된 값과 다른 토큰이 오면 재사용 탐지로 보고 삭제한다.
- **영향**: 기기 B에서 로그인하면 기기 A의 토큰이 덮어써져, A가 다음에 갱신할 때 재사용 탐지로 강제 로그아웃된다. 웹과 안드로이드 앱을 함께 쓰는 사용자가 겪는다.
- **확인**: 코드 추적
- **권장 방향**: 로그인마다 세션 ID를 발급해 리프레시 토큰(JWT `jti` 등)에 넣고, 키를 `refresh-token:{userId}:{sessionId}`로 바꿔 재사용 탐지를 세션 단위로 한다. M7(토큰 종류 구분)과 L11(동시 refresh)을 같이 처리하면 비용이 작다.
- **정할 것**: 사용자당 동시 세션 상한, "모든 기기에서 로그아웃" 제공 여부, 배포 시 기존 토큰 처리 (키 형식이 바뀌면 기존 로그인 사용자 전원이 다시 로그인해야 함)

### H3. 같은 모집글에 다른 포지션으로 동시에 지원하면 중복 지원이 들어감

- **위치**: `ApplicationService.applyToTeam`
- **문제**: "한 모집글에 사용자당 지원 1건"을 `existsByPostIdAndUserId` 검사로만 지킨다. 그 앞의 비관적 락(`findForUpdateByPostIdAndPosition`)은 `(postId, position)` 단위라, 같은 글의 서로 다른 포지션에 동시에 지원하면 두 요청이 모두 검사를 통과한다. V12의 인덱스 `idx_applications_post_user`는 unique가 아니다.
- **영향**: 같은 사용자의 지원서가 한 모집글에 두 건 생긴다.
- **확인**: 코드 추적
- **권장 방향**: 둘 중 하나
  - 락을 `posts` 행 단위로 올린다
  - DB에서 강제한다. soft delete 때문에 단순 unique는 걸 수 없으므로, 삭제되지 않은 행만 값을 갖는 생성 컬럼을 두고 그 컬럼에 unique 인덱스를 건다
- **주의**: V12의 주석("한 모집글에 사용자당 1지원만")은 실제와 다르지만, 적용된 마이그레이션은 체크섬 때문에 수정할 수 없다. 제약을 걸 때 새 마이그레이션으로 처리한다.

### H4. 지원서 승인, 거절, 취소, 수정이 동시에 들어오면 상태와 멤버십이 어긋남

- **위치**: `ApplicationService`의 `approveApplication`, `rejectApplication`, `deleteApplication`, `updateApplication`, `Application.updateStatus`
- **문제**: 네 경로 모두 지원서를 잠금 없이 읽고, 메모리의 상태가 `PENDING`인지만 확인한 뒤 변경 감지로 저장한다. `@Version`도 없고 Hibernate는 모든 컬럼을 UPDATE하므로 나중에 커밋한 쪽이 앞의 변경을 덮는다.
- **영향**
  - 매니저 둘이 동시에 승인과 거절을 하면 지원서는 `REJECTED`인데 멤버로 가입되어 있고, 합류 알림과 거절 알림이 둘 다 간다
  - 지원자 취소와 승인이 겹치면 취소가 무효가 되거나(승인이 `deleted_at`을 NULL로 덮음), 취소된 지원서의 지원자가 멤버가 된다
  - 수정이 나중에 커밋되면 상태가 `PENDING`으로 돌아가 같은 지원서를 다시 처리할 수 있다
- **확인**: 코드 추적. 실행 재현은 안 함
- **권장 방향**: 상태 전이를 조건부 UPDATE(`... WHERE id = ? AND status = 'PENDING' AND deleted_at IS NULL`)로 바꿔 영향 행 수로 판정하거나, 처리 경로에서 지원서를 `PESSIMISTIC_WRITE`로 읽는다 (`RecruitmentRepository.findForUpdate...` 전례). `@Version`을 쓰면 충돌 예외를 409로 매핑한다.

### H5. 모집글 수정, 삭제 권한이 작성 당시 기준이라 팀을 떠난 작성자가 지울 수 있음

- **위치**: `PostService.updatePost`, `deletePost`
- **문제**: 생성과 마감은 "현재 팀 MANAGER 이상"을 요구하는데, 삭제는 작성자 본인인지(`checkOwnership`)만 보고 팀 멤버십을 보지 않는다. 수정은 작성자인지 본 뒤 멤버를 찾고, 역할은 보지 않는다. 탈퇴나 추방 때 그 사람이 쓴 모집글은 그대로 남는다.
- **영향**
  - 추방된 매니저가 팀 모집글을 지울 수 있다. 지원서, 댓글, 좋아요, 북마크, 알림까지 함께 정리된다
  - 작성자가 팀을 떠나면 그 글은 누구도 수정하거나 삭제할 수 없다. 작성자는 멤버 조회에서 404, 다른 리더와 매니저는 작성자 검사에서 403이다
  - MEMBER로 강등된 작성자도 모집 포지션을 계속 바꿀 수 있다
- **확인**: 코드 추적
- **권장 방향**: 수정과 삭제 권한을 "현재 팀 MANAGER 이상"(필요하면 "작성자이면서 현재 멤버" 또는 "매니저")으로 맞추고, 멤버 검사를 작성자 검사보다 앞에 둔다.

---

## 중간

### M1. 정렬 값이 ID가 아닌 목록의 커서가 ID 하나뿐이라 목록이 끊기거나 건너뜀

- **위치**
  - `PostRepositoryCustomImpl`의 조회순, 인기순 커서 (`afterViewCountCursor`, `afterLikeCountCursor`)
  - `ApplicationService.getTeamApplications`의 팀 지원 목록 커서 (`statusPriority`, `id` 순 정렬)
- **문제**: `ORDER BY like_count DESC, id DESC`처럼 ID가 아닌 값으로 정렬하면, 다음 페이지 위치를 정하는 데 마지막 항목의 정렬 값이 필요하다. 그런데 커서로 ID만 받아서, 서버가 매 요청마다 커서 항목의 **현재** 정렬 값을 다시 읽는다. 최신순, 오래된순은 ID 자체가 정렬 기준이라 `id < cursor`만으로 충분해서 해당하지 않는다.
- **영향**
  - 커서 항목이 그사이 삭제되면 모집글 목록은 서브쿼리가 NULL이 되어 다음 페이지가 빈 목록으로 오고, 팀 지원 목록은 404가 난다. 무한 스크롤이 멈춘다
  - 커서 항목의 정렬 값이 그사이 바뀌면 시작 위치가 따라 움직인다. 값이 오르면 이미 본 항목이 다시 나오고(중복), 내리면 일부를 건너뛴다. 조회수는 계속 오르므로 조회순에서 특히 생기기 쉽다
  - 팀 지원 목록에서는 관리자가 목록을 보며 승인하는 핵심 흐름이 깨진다. 1페이지 마지막 지원서를 승인하면 그 지원서가 "처리됨" 구간으로 옮겨 가서, 다음 페이지가 남은 대기 지원서를 전부 건너뛰고 처리된 지원서부터 보여 준다
  - 페이지마다 조회가 1개 추가된다
- **확인**: 코드 추적
- **권장 방향**: 첫 페이지를 볼 때의 정렬 값을 커서에 함께 담는다. 예) `(like_count: 10, id: 50)` → `WHERE like_count < 10 OR (like_count = 10 AND id < 50)`. 서버가 커서 항목을 다시 읽지 않으므로 삭제와 값 변경에 영향받지 않는다. 보통 이 값들을 하나의 문자열로 인코딩해 `nextCursor`로 내려 준다.
- **주의**: `CursorGetQuery`, `CursorResponse` 계약이 바뀌므로 프론트, 앱과 동시에 배포해야 한다. 정렬 방식마다 커서 형식이 달라지므로, 공통 DTO를 쓰는 다른 목록 API에 영향이 없게 범위를 정할 것

### M2. `@CurrentUser` 요청마다 사용자와 약관을 조회하는 고정 쿼리

- **위치**: `CurrentUserArgumentResolver`, `User`의 `@ElementCollection(fetch = FetchType.EAGER)` 2개, `TermService.checkAllRequiredAgreed`
- **문제**: 인증된 요청마다 컨트롤러 본문 전에 사용자 조회, 컬렉션 2개(`skills`, `portfolioUrls`), 최신 약관 조회, 동의 개수 조회가 실행된다. 대부분의 요청은 컬렉션을 쓰지 않는다.
- **확인**: 코드 추적
- **권장 방향**
  - 최신 약관 조회 캐싱: 약관은 마이그레이션으로만 추가되어 재시작 시 갱신되므로 캐시가 낡을 위험이 작다. Spring 캐시를 켜면 classpath의 Redis가 캐시 저장소로 자동 선택되므로 `spring.cache.type=simple`을 명시할 것
  - 컬렉션 LAZY 전환: H1을 먼저 처리해야 함. 지금 전환하면 프로필 설정과 수정, 댓글 작성, 모집글 작성과 수정이 `LazyInitializationException`으로 깨짐

### M3. 메시지 대화 조회가 매번 filesort

- **위치**: `MessageRepository.findMessageHistoryBefore`, `findMessageHistoryAfter`
- **문제**: `(A→B) OR (B→A)` 조건이라 인덱스에서 떨어진 두 구간을 읽고, MySQL은 두 구간을 `id` 순으로 병합하지 못해 정렬을 따로 한다. 인덱스 구성을 바꿔도 해결되지 않는다.
- **영향**: 대화가 길어질수록 페이지마다 그 대화 전체를 읽고 정렬한다.
- **확인**: MySQL 8.0.45 EXPLAIN
- **권장 방향**: 방향별로 `ORDER BY id DESC LIMIT n`을 한 뒤 `UNION ALL`로 합쳐 최대 `2n`개만 다시 정렬한다.

### M4. MySQL 8.0 지원 종료

- **위치**: `docker-compose.yml`, `TestContainers.kt` (`mysql:8.0.45`로 고정)
- **문제**: MySQL 8.0은 2026-04-30에 지원이 종료되어 보안 패치가 더 나오지 않는다. 다음 LTS는 8.4 (2032-04까지 지원 예정).
- **권장 방향**: 8.4 LTS로 올린다. 두 파일의 이미지를 함께 바꾸고, 테스트 컨테이너로 전체 테스트와 주요 쿼리 EXPLAIN을 다시 확인한 뒤 운영에 반영한다. 8.4의 변경 사항(기본 인증 방식 등)은 업그레이드 전에 공식 릴리스 노트로 확인할 것

### M5. 유니크 제약 경합이 500과 Discord 알림으로 처리됨

- **위치**: `GlobalExceptionHandler` (`DataIntegrityViolationException` 처리가 없어 `Exception` 처리로 떨어짐)
- **문제**: 여러 곳이 "있는지 확인한 뒤 저장"하는 구조라, 동시 요청이 둘 다 확인을 통과하면 뒤의 요청이 DB 유니크 제약에 걸린다. 이 예외가 500, ERROR 로그, Discord 알림으로 이어진다.
  - 북마크 토글, 팔로우 토글(더블클릭)
  - 프로필 설정의 username (`uk_users_username`)
  - 약관 동의 중복 제출, OAuth 콜백 중복 (`(user_id, term_id)`, `(provider, provider_id)`)
  - 팀원 리뷰(`uk_member_reviews_reviewer_reviewee_team`), 지원서 열람 기록(`application_reads (application_id, user_id)`), 같은 지원서 동시 승인(`members (user_id, team_id)`)
- **영향**: 409나 400이어야 할 응답이 500이 되고, 장애 알림 채널에 잡음이 쌓인다. Discord에 `Duplicate entry '<값>' ...` 같은 SQL 메시지와 값이 그대로 들어간다.
  - 지원서 열람 기록은 **재가입한 매니저에게 영구적으로 실패**한다. 탈퇴 때 열람 기록을 soft delete하는데 유니크 제약은 삭제된 행까지 포함하고, 존재 확인은 `@SQLRestriction` 때문에 삭제된 행을 못 봐서, 예전에 읽은 지원서를 열 때마다 INSERT가 충돌한다
- **확인**: 핸들러 부재, 유니크 제약, 확인 후 저장 구조는 코드 추적. 동시 요청 재현은 안 함
- **권장 방향**
  - `DataIntegrityViolationException`을 409 `DUPLICATE_RESOURCE`로 매핑하는 핸들러를 두고, 응답과 알림에 SQL 메시지를 넣지 않는다
  - 멱등해야 하는 곳(북마크, 팔로우, 약관 동의, 열람 기록)은 `INSERT IGNORE`나 `ON DUPLICATE KEY UPDATE`로 바꾼다. 열람 기록은 `ON DUPLICATE KEY UPDATE deleted_at = NULL`로 복원까지 겸한다
  - username은 409로 "이미 사용 중"을 알린다

### M6. 재가입 시 팔로우 복원으로 팔로워, 팔로잉 목록이 404나 500이 됨

- **위치**: `FollowRepository`의 복원 쿼리, `CustomOAuth2UserService`의 재가입 처리, `FollowService`의 목록 조회, `UserSimpleResponse.from`
- **문제**
  - 재가입 시 `UPDATE follows SET deleted_at = NULL WHERE (follower_id = :userId OR followee_id = :userId) ...`로 상대방 상태와 상관없이 전부 복원한다
  - 목록은 상대 사용자를 못 찾으면 전체를 실패시킨다 (`userById[...] ?: throw ENTITY_NOT_FOUND`)
  - `UserSimpleResponse.from`은 `user.username!!`인데, 탈퇴하면 username이 null이 되고 재가입 직후에도 null로 남는다
- **영향**
  - A가 B를 팔로우한 상태에서 둘 다 탈퇴하고 B만 재가입하면, 탈퇴 상태인 A와의 팔로우가 복원되어 B의 팔로워 목록이 404가 된다. 공개 팔로우 수에도 탈퇴자가 포함된다
  - A가 B를 팔로우한 상태에서 B가 탈퇴 후 재가입하고 프로필을 아직 설정하지 않으면, A의 팔로잉 목록이 NPE로 500이 된다. B가 프로필을 설정할 때까지 계속된다
- **확인**: 코드 추적. 이 조합을 다루는 테스트가 없음
- **권장 방향**: 복원 쿼리에 "상대방이 활성 사용자"라는 조건을 추가한다. 개수와 목록은 활성이면서 프로필이 완성된 사용자만 세도록 JOIN 쿼리로 바꾸고, 목록은 실패 대신 건너뛴다. 팔로우 대상 검증에 프로필 완성 여부를 포함한다.

### M7. 리프레시 토큰을 액세스 토큰으로도 쓸 수 있음

- **위치**: `JwtProvider.generateAccessToken`, `generateRefreshToken`, `JwtAuthenticationFilter`
- **문제**: 두 토큰을 같은 키, 같은 claim(`sub`, `role`)으로 만들고 유효 기간만 다르다(1시간, 7일). 인증 필터는 토큰이 유효한지만 확인한다. 로그아웃은 Redis 키만 지운다.
- **영향**: 리프레시 토큰이 유출되면 `Authorization: Bearer <리프레시 토큰>`으로 모든 API를 7일간 쓸 수 있고, 사용자가 로그아웃해도 막히지 않는다. 쿠키가 HttpOnly에 `Path=/auth`라 유출 경로는 좁은 편이다.
- **확인**: 코드 추적
- **권장 방향**: 토큰 종류 claim(`typ`)을 넣어 인증 필터는 액세스 토큰만, refresh는 리프레시 토큰만 받는다. H2에서 `jti`를 도입할 때 함께 하면 비용이 작다.

### M8. 완료된 팀이 계속 지원을 받고, 상태를 바꿀 때마다 리뷰 요청이 반복됨

- **위치**: `Team.updateStatus`, `TeamService`의 상태 변경, `NotificationEventListener.handleTeamCompleted`, `ApplicationService.applyToTeam`과 `approveApplication`, `PostService.updatePost`
- **문제**
  - 팀 상태는 같은 상태로 바꾸는 것만 막아서 완료와 진행 중을 자유롭게 오갈 수 있다. 완료로 바꿀 때마다 `TeamCompletedEvent`가 발행되어 전 멤버에게 리뷰 요청 알림이 다시 간다
  - 팀이 완료되어도 모집글과 모집 정보는 그대로 열려 있다. 지원과 승인, 모집글 수정은 팀 상태를 보지 않는다 (모집글 생성만 막음)
- **영향**
  - 상태를 토글하면 리뷰 요청 알림이 반복된다
  - 끝난 프로젝트에 지원이 쌓인다. 완료 후 승인된 사람은 프로젝트에 참여하지 않았는데도 즉시 다른 멤버를 리뷰할 수 있어(리뷰 자격이 "현재 활성 멤버"뿐), 협업 온도의 신뢰성이 떨어진다
- **확인**: 코드 추적
- **정할 것**: 완료를 되돌릴 수 없는 최종 상태로 둘지, 정원(`Recruitment.count`)을 넘는 승인을 막을지 (지금은 표시용으로만 쓰임)
- **권장 방향**: 허용하는 상태 전이를 엔티티에 명시하고, 완료 알림은 처음 완료될 때만 보낸다. 완료 시 열린 모집을 닫거나, 지원과 승인, 모집글 수정에서 완료된 팀을 거부한다.

### M10. 이미지 URL 컬럼 길이가 엔티티와 마이그레이션에서 다르고 요청 길이 제한이 없음

- **위치**: `V1__init.sql`(`users.profile_image_url`, `teams.profile_image_url` 모두 `varchar(255)`), `User`와 `Team` 엔티티(`VARCHAR(2083)`), 요청 DTO의 `profileImageUrl`, `portfolioUrls`
- **문제**: 마이그레이션과 엔티티의 길이가 다르고, 이후 길이를 바꾼 마이그레이션이 없다. `ddl-auto: validate`는 길이를 검증하지 않는다. 요청 DTO에 길이 제한(`@Size`)이 없고 포트폴리오 URL 목록 개수에도 제한이 없다.
- **영향**: 255자를 넘는 이미지 URL이나 2048자를 넘는 포트폴리오 URL을 보내면 DB 오류로 500이 된다. 엔티티의 `VARCHAR(2083)`만 보고 길이를 믿으면 안 된다.
- **확인**: 코드, 마이그레이션, 운영 DB 확인. 운영의 `users.profile_image_url`, `teams.profile_image_url`은 `varchar(255)`로 V1과 같다. 포트폴리오 URL은 V1과 엔티티 모두 `varchar(2048)`로 일치한다
- **권장 방향**: DTO에 `@Size(max = 255)`(이미지), `@Size(max = 2048)`(포트폴리오)와 목록 개수 제한을 둔다. 엔티티의 `columnDefinition`을 `VARCHAR(255)`로 고쳐 실제 DB와 맞춘다. U2의 URL 접두사 검증과 함께 처리하면 좋다.

### M11. Presigned URL 업로드에 크기 제한이 없고 고아 객체가 쌓임

- **위치**: `S3StorageClient`의 presigned URL 발급, 사용자, 팀, 모집글 본문 이미지 발급 API
- **문제**: Content-Type은 서명에 포함하지만 크기는 제한하지 않는다(단일 PUT 최대 5GB). 발급만 받고 쓰지 않은 객체, 모집글을 수정하거나 삭제해도 남는 본문 이미지를 정리하는 경로가 없다.
- **영향**: 가입한 계정이 URL을 반복 발급받아 큰 파일을 올릴 수 있다. S3 저장, 전송 비용이 늘고 버킷에 쓰레기가 쌓인다.
- **확인**: 코드 추적
- **권장 방향**: 크기 제한이 필요하면 presigned POST(`content-length-range`)로 바꾼다. 임시 경로에 올렸다가 실제로 쓰일 때 확정하는 방식과 버킷 수명 주기 규칙을 검토한다. 모집글 삭제 시 본문 이미지 정리도 함께 본다.

---

## 낮음

### L1. 약관 미동의가 400(`INVALID_STATE`)

- **위치**: `TermService.checkAllRequiredAgreed`
- **문제**: 의미상 403이고, 일반 유효성 오류와 같은 코드라 클라이언트가 구분할 수 없다.
- **보류 이유**: 웹 프론트는 `GET /terms`로 동의 여부를 미리 확인해 약관 모달을 띄우므로, 지금은 이 응답에 의존하지 않는다. 또 웹의 `RouteErrorPage`는 403이면 "권한 없음" 페이지를 보여 줘서, 응답 코드만 바꾸면 안내 문구가 오히려 틀어진다.
- **권장 방향**: 약관 개정 시 재동의를 받는 흐름을 설계할 때, 전용 `ErrorCode`와 프론트 처리를 함께 정한다.

### L2. 멤버 역할 정렬이 enum 문자열 순서에 의존

- **위치**: `MemberRepository`의 `...OrderByRoleAscIdAsc` 메서드들과 native `ORDER BY role ASC, id ASC`
- **문제**: `role`이 VARCHAR라 알파벳순(`LEADER < MANAGER < MEMBER`)으로 정렬되는데, 이게 권한 순서와 우연히 일치할 뿐이다.
- **영향**: 역할을 추가하면 정렬이 조용히 틀어진다.
- **권장 방향**: 정렬용 값(`MemberRole.level`)을 DB에서 쓸 수 있게 하거나(`CASE` 또는 별도 컬럼), 애플리케이션에서 `level`로 정렬한다.

### L3. 메시지 검색어의 연산자 문자가 검색 결과를 바꿈

- **위치**: `MessageRepository.searchByContent`, `ConversationRepository.searchByUsernameOrContent`
- **문제**: 검색어를 `MATCH ... AGAINST(:q IN BOOLEAN MODE)`에 그대로 넣어, `+`, `-`, `(`, `"`, `@` 같은 문자가 연산자로 해석된다. 예) `C++`는 0건, 괄호 짝이 안 맞으면 0건. 500 에러는 나지 않는다 (MySQL 8.0.45, ngram에서 약 60개 입력으로 확인). 사용자명 검색의 `LIKE CONCAT('%', :q, '%')`도 `%`, `_`를 이스케이프하지 않는다.
- **권장 방향**: 연산자 문자를 제거하거나 각 토큰을 `"..."`로 감싸서 넘기고, `LIKE`에는 `ESCAPE`를 쓴다. 모집글 제목 검색은 QueryDSL `contains`가 이미 이스케이프한다. L4와 같은 쿼리를 건드리므로 함께 처리하면 좋다.

### L4. 대화 검색 결과의 미리보기가 검색어와 무관한 마지막 메시지로 나옴

- **위치**: `ConversationService`의 검색, `MessageRepository.searchByContent`
- **문제**: 대화 목록은 커서로 페이지를 나누는데, 미리보기에 쓸 매칭 메시지는 커서 없이 전체에서 상위 `size`명만 가져온다. 매칭 메시지를 못 찾은 대화는 마지막 메시지를 미리보기로 쓴다.
- **영향**: 2페이지 이후의 대화나, 최근 메시지는 있지만 검색어가 들어간 메시지가 오래된 대화는 검색어와 무관한 메시지가 미리보기로 나온다. 왜 검색 결과에 나왔는지 보이지 않는다.
- **확인**: 코드 추적
- **권장 방향**: 매칭 메시지 조회를 현재 페이지의 상대 ID 목록으로 한정한다.

### L5. 실시간 메시지 전달마다 쿼리 3개로 재조회

- **위치**: `MessageSubscriber.onMessage`
- **문제**: Redis 이벤트에 메시지 ID만 실어, 받는 쪽에서 메시지, 보낸 사람, 받는 사람을 다시 조회한다. 보내는 쪽은 이미 셋 다 가지고 있다.
- **권장 방향**: 이벤트 전용 페이로드에 필요한 값을 실어 보낸다. API 응답 DTO를 그대로 넣지 말 것 (CLAUDE.md 7절).

### L6. 셋업 가드 로직이 두 곳에 중복

- **위치**: `CurrentUserArgumentResolver`, `SetupCompleteInterceptor`
- **문제**: 두 곳이 "사용자 조회 → `checkProfileComplete` → `checkAllRequiredAgreed`"를 각자 구현한다. 면제 애너테이션의 적용 범위도 다르다 (`@AllowIncompleteSetup`은 리졸버에만, `@AllowMissingTermAgreement`는 둘 다).
- **권장 방향**: 검사 로직을 공통 컴포넌트로 추출한다. 두 메커니즘의 용도 구분(CLAUDE.md 3절)은 유지하되, 면제 범위 차이가 의도인지 정한 뒤 통일한다.

### L7. 좋아요 카운터를 실제 행 수로 다시 맞출 수단이 없음

- **위치**: `posts.like_count`, `comments.like_count`
- **문제**: 카운터는 좋아요 처리와 삭제 cascade의 native UPDATE로만 유지되고, `LikeCountTest`가 불변식(카운터 = 실제 `likes` 행 수)을 검증한다. 하지만 DB를 수동으로 고치거나, 새 삭제 경로를 추가하면서 카운터 갱신을 빠뜨리면 어긋날 수 있다.
- **권장 방향**: 불일치를 확인하는 쿼리를 운영 점검에 두고, 필요하면 주기적으로 다시 맞추는 배치를 둔다.

  ```sql
  SELECT COUNT(*) FROM posts p
  WHERE p.like_count <> (SELECT COUNT(*) FROM likes l WHERE l.type = 'POST' AND l.target_id = p.id);
  -- comments도 type = 'COMMENT'로 같은 방식
  ```

### L8. 엔티티에 `@Version`이 없어 동시 수정이 서로 덮어씀 (멤버, 모집 마감)

- **위치**: `MemberService`의 팀 이탈, 추방, 리더 위임, `PostService.updatePost`와 모집 마감, `RecruitmentScheduler`
- **문제**: 코드베이스 전체에 `@Version`과 `@DynamicUpdate`가 없어, 변경 감지 UPDATE가 모든 컬럼을 읽은 시점 값으로 다시 쓴다. H4(지원서)와 같은 원인이다.
- **영향** (발생 창은 좁음)
  - 2인 팀에서 리더와 멤버가 동시에 나가면 각자 상대가 남아 있다고 보고 통과해, 활성 멤버 0명인 팀이 남는다. 아무도 관리하거나 지울 수 없다
  - 역할 변경과 추방이 겹쳐 역할 변경이 나중에 커밋되면 추방된 멤버가 되살아난다
  - 모집글 수정 중 매니저가 모집을 마감하면, 수정이 옛 상태(`RECRUITING`)로 덮어써 마감이 풀린다. 재모집 금지 규칙이 깨진다
- **확인**: 코드 추적. 타이밍 의존이라 재현은 안 함
- **권장 방향**: 해당 엔티티에 `@Version`을 두고 충돌을 409로 매핑하거나, 팀 단위 행 잠금으로 직렬화한다. 마감은 조건부 UPDATE로 바꾸는 방법도 있다.

### L9. 팀원 리뷰가 동시에 들어오면 협업 온도에 리뷰 하나가 빠짐

- **위치**: `MemberReviewService` (온도 재계산)
- **문제**: 리뷰마다 좋아요, 싫어요 개수를 세어 온도를 다시 계산해 엔티티 필드로 저장한다. 두 리뷰어가 같은 사람을 동시에 리뷰하면 각 트랜잭션이 상대의 리뷰를 보지 못한 채 계산하고, 나중에 커밋한 쪽의 온도가 남는다. 사용자 엔티티 전체 UPDATE라 같은 시점의 다른 사용자 정보 변경도 덮을 수 있다 (H1과 같은 원인).
- **영향**: 다음 리뷰가 올 때까지 온도가 틀린 채로 남는다. 팀 완료 직후 리뷰가 몰리는 시점에 생기기 쉽다.
- **확인**: 코드 추적
- **권장 방향**: 온도만 바꾸는 native UPDATE로 바꾸고, 계산은 커밋 후 다시 세거나 사용자 행을 잠가 직렬화한다.

### L10. 포지션 삭제와 지원이 겹치면 대응 모집 정보가 없는 지원서가 생김

- **위치**: `PostService.updatePost`의 포지션 삭제, `ApplicationService.applyToTeam`
- **문제**: 포지션 삭제는 잠금 없이 "지원서가 없음"을 확인한 뒤 지우고, 지원은 모집 정보 행을 잠근 뒤 지원서를 넣는다. 확인과 삭제 사이에 지원이 커밋되면 삭제가 그대로 진행된다. `updatePost`의 주석이 막으려던 상황이다.
- **확인**: 코드 추적 (MySQL REPEATABLE READ 기준 추론, 재현은 안 함)
- **권장 방향**: 삭제할 모집 정보를 지원과 같은 `findForUpdate...`로 먼저 잠근 뒤 지원서 존재를 확인한다.

### L11. 동시 refresh 요청이 재사용 탐지에 걸려 로그아웃될 수 있음

- **위치**: `AuthService.refresh`
- **문제**: 저장된 토큰을 읽고, 비교하고, 새 토큰을 쓰는 과정이 원자적이지 않고, 불일치하면 키를 지운다. 같은 토큰으로 refresh가 거의 동시에 두 번 오면 두 번째가 재사용으로 판정된다.
- **영향**: 웹 프론트는 한 탭 안에서 동시 refresh를 하나로 합치지만(`axiosInstance`의 `refreshPromise`), 탭이 여러 개면 합쳐지지 않아 산발적으로 로그아웃될 수 있다. H2를 고쳐도 같은 기기 안의 경합이라 남는다.
- **확인**: 서버 쪽은 코드 추적. 실제 빈도는 미확인
- **권장 방향**: 비교와 교체를 Lua 스크립트로 원자화하거나, 직전 토큰을 수 초간 허용해 같은 새 토큰을 돌려준다.

### L12. 로그아웃에 유효한 액세스 토큰이 필요함

- **위치**: `SecurityConfig` (`/auth/logout`이 공개 경로가 아님), `AuthController.logout`
- **문제**: 액세스 토큰이 만료된 상태로 로그아웃하면 401이 나고 리프레시 쿠키와 Redis 토큰이 남는다. 웹 프론트는 401을 받으면 토큰을 갱신한 뒤 다시 시도하므로 대부분은 동작하지만, 리프레시 토큰까지 만료되면 서버 쪽 정리가 안 된다.
- **권장 방향**: 리프레시 쿠키만으로 로그아웃할 수 있게 공개 경로로 둔다. `SecurityPermitAllTest`와 Swagger 공개 표시(`@SecurityRequirements`)를 함께 맞춘다.

### L13. 탈퇴자 이메일이 중복 검사에서 빠져 같은 이메일의 계정이 둘이 될 수 있음

- **위치**: `CustomOAuth2UserService`, `UserRepository.existsByEmail`
- **문제**: 신규 가입 시 이메일 중복 검사가 `@SQLRestriction` 때문에 탈퇴자를 제외한다. Google 계정으로 탈퇴한 뒤 같은 이메일로 Kakao 신규 가입을 하고, 다시 Google로 로그인하면 기존 계정이 복원되어 같은 이메일의 활성 계정이 둘이 된다.
- **확인**: 코드 추적
- **권장 방향**: "이메일당 계정 하나" 정책을 유지하려면 재가입 복원 분기에서도 활성 사용자의 이메일 중복을 확인한다.

### L14. 선택 약관(마케팅) 동의를 철회할 수 없음

- **위치**: `TermService.agreeToTerms`, `UserTermAgreement.withdrawnAt`
- **문제**: 동의(`agreed = true`)만 저장하고 미동의는 무시한다. `withdrawn_at` 컬럼을 쓰는 코드가 없다.
- **영향**: 마케팅 수신에 동의한 사용자가 해제해도 서버에는 동의 상태로 남는다. 마케팅 발송을 붙이면 철회한 사람에게도 발송되어 정보통신망법상 수신 동의 철회 요건과 충돌할 수 있다. **마케팅 발송을 붙이기 전에 반드시 처리할 것**
- **권장 방향**: 미동의로 바뀐 기존 동의 행에 `withdrawn_at = UTC_TIMESTAMP(6)`을 기록하고, 다시 동의하면 비운다.

### L15. 탈퇴 후 개인정보 보존 기한이 없음

- **위치**: `User.deactivate`
- **문제**: 탈퇴 시 username과 프로필 이미지만 지우고 이메일, 소개, 스킬, 포트폴리오, 포지션은 기한 없이 남긴다. 재가입 복원 설계와 맞물린 선택이다.
- **권장 방향**: 개인정보처리방침의 파기 조항과 맞춰 보존 기한을 정하고, 기한이 지나면 파기하는 배치를 둔다. 정책 결정이 먼저다.

### L16. `COOKIE_SECURE` 기본값이 `false`

- **위치**: `application.yaml` (`secure: ${COOKIE_SECURE:false}`)
- **문제**: 운영에서 환경 변수가 빠지면 리프레시 쿠키가 `Secure` 없이 나간다.
- **확인**: 설정 파일 확인. 운영 환경 변수 값은 미확인
- **권장 방향**: 운영 프로필(`application-prod.yaml`)에 `secure: true`를 고정한다.

### L17. 팀 모집글 목록의 지원자 수가 일반 멤버에게도 보임

- **위치**: `PostService.getTeamPosts`
- **문제**: 팀 멤버이기만 하면 지원자 수와 안 읽은 지원서 수를 채운다. 지원서를 보고 처리하는 API는 모두 MANAGER 이상을 요구한다. 일반 멤버에게 보이는 "안 읽은 지원서 수"는 그 사람이 읽을 수 없는 지원서 기준이라 의미도 없다.
- **권장 방향**: 판정을 "MANAGER 이상"으로 바꾼다.

### L18. 모집 정보가 빈 목록이어도 모집글이 만들어짐

- **위치**: `PostCreateRequest`, `PostUpdateRequest`의 `recruitments`
- **문제**: `@Valid @NotNull @UniquePosition`만 있고 최소 개수 제약이 없다. 빈 목록으로 만들면 처음부터 모집 마감 상태인 글이 되어 아무도 지원할 수 없다.
- **권장 방향**: `@field:Size(min = 1)`을 둔다. 상한은 포지션 종류 수로 함께 둔다.

### L19. 페이지네이션이 없는 목록 API

- **위치**: 팔로워, 팔로잉 목록(`FollowService`), 내 북마크 목록(`BookmarkService`), 팀 모집글 목록(`PostService.getTeamPosts`)
- **문제**: 전체를 한 번에 읽는다. 팔로우 목록은 사용자를 `findAllById`로 한꺼번에 읽어 EAGER 컬렉션 조회까지 붙는다.
- **영향**: 항목이 많은 사용자일수록 응답 크기와 쿼리 비용이 선형으로 는다. 지금 규모에서는 급하지 않다.
- **권장 방향**: `CursorGetQuery`, `CursorResponse`로 `id` 기준 커서를 적용한다. 팔로우 목록은 M6의 JOIN 쿼리 전환과 함께 한다.

### L20. 탈퇴 뒤에도 열려 있던 WebSocket 세션으로 메시지를 보낼 수 있음

- **위치**: `MessageService.sendMessage`, `WebSocketAuthHandshakeHandler`
- **문제**: 사용자 식별은 연결할 때 한 번만 하고, 메시지를 보낼 때 발신자가 아직 활성인지 확인하지 않는다. 탈퇴 처리에서 WebSocket 세션을 정리하지 않는다.
- **영향**: 탈퇴한 뒤에도 열려 있던 앱 화면에서 메시지를 보내면 저장되고, 받는 쪽 대화 목록에 이름이 빈 상대가 생긴다. 실시간 전달은 발신자 조회 실패로 깨진다.
- **권장 방향**: `sendMessage`에 발신자 존재 확인을 넣는다. 세션 강제 종료는 범위가 커서 선택 사항이다.

### L21. 알림 보관 기한이 없고 읽음 처리 ID 목록에 상한이 없음

- **위치**: `NotificationRepository`, `ReadNotificationsRequest`
- **문제**: 알림은 팀, 모집글 삭제와 탈퇴 cascade로만 지워지고 기간 기준 정리가 없다. 배지용 개수 조회는 그 사용자의 알림 전체를 훑는다. 읽음 처리 요청의 ID 목록에는 `@NotEmpty`만 있고 크기 상한이 없다.
- **영향**: 활동이 많은 사용자는 알림이 계속 쌓여 배지 조회가 느려진다. ID를 아주 많이 보내면 거대한 `IN` 절이 만들어진다.
- **권장 방향**: 읽은 지 일정 기간 지난 알림을 지우는 `@Scheduled` 작업을 둔다. 요청에 `@field:Size(max = 100)` 정도를 둔다.

### L22. 일부 cascade 삭제가 행을 전부 읽은 뒤 하나씩 지움

- **위치**: `NotificationRepository.deleteByUserId`, `LikeRepository.deleteByIdTypeAndIdTargetId`, `deleteByIdUserId`, `BookmarkRepository.deleteByIdUserId`
- **문제**: 이름으로 만든 삭제 메서드(derived delete)는 대상 엔티티를 모두 읽어 영속성 컨텍스트에 올린 뒤 하나씩 지운다. 바로 옆의 다른 cascade 삭제는 모두 native 벌크 DELETE다.
- **영향**: 좋아요가 수천 개인 모집글을 지우거나 알림이 많은 사용자가 탈퇴하면 한 트랜잭션에서 수천 개를 읽고 지운다.
- **권장 방향**: 주변 쿼리처럼 `@Modifying` native DELETE로 바꾼다. 좋아요 카운터 갱신과의 순서(`LikeCascadeListener`)는 그대로 유지한다.

### L23. 쓰이지 않는 인덱스 `idx_messages_receiver_read`

- **위치**: `Message` 엔티티, `V1__init.sql`
- **문제**: `(receiver_id, read_at)` 조건으로 조회하는 쿼리가 없다. 읽음 처리는 `(sender_id, receiver_id)` 인덱스를 쓰고, `receiver_id`로 시작하는 조회는 `idx_messages_receiver_sender`가 맡는다.
- **영향**: 메시지 저장과 읽음 처리(`read_at` 갱신)마다 인덱스 갱신 비용만 든다.
- **권장 방향**: 새 마이그레이션으로 지우고 엔티티의 `@Table(indexes)`에서도 뺀다.

### L24. Spring MVC 예외 응답이 내부 클래스명을 노출하거나 원인을 버림

- **위치**: `GlobalExceptionHandler`의 `handleExceptionInternal`, `handleMethodArgumentNotValid`
- **문제**: `handleExceptionInternal`은 원인 없이 상태 문구만 돌려줘서, 요청 본문을 읽지 못한 경우(필수 필드 누락 등) 클라이언트가 어느 필드가 문제인지 알 수 없다. 반대로 `handleMethodArgumentNotValid`는 타입 변환 실패 메시지를 그대로 내보내 `io.waggle...Position` 같은 내부 클래스명이 노출된다. MVC 경로의 5xx는 Discord로 알리지 않는다.
- **권장 방향**: 400 계열에는 필드명 수준의 안전한 설명을 넣고, 타입 변환 실패는 고정 문구로 바꾼다. 5xx 분기에서도 Discord를 호출한다.

### L25. 알림 생성이 실패하면 영구 유실 (현재는 유실 허용)

- **위치**: `NotificationEventListener` (`@Async` + `@TransactionalEventListener(phase = AFTER_COMMIT)` + `REQUIRES_NEW`)
- **문제**: 본 트랜잭션이 커밋된 뒤 별도 스레드에서 알림을 만든다. 알림 저장이 실패하거나 그 사이 서버가 재시작되면 재시도 수단이 없고 로그만 남는다.
- **영향**: 드물게 알림이 사라진다. 사용자는 지원이나 댓글이 있었는지 모르게 된다.
- **현재 판단**: 인앱 알림은 핵심 기능이 아니라서 유실을 허용한다(best-effort). 알림이 중요해지면(푸시 알림 도입, 알림 누락 문의 발생 등) 다시 본다.
- **권장 방향**: Transactional Outbox. 본 트랜잭션 안에서 알림 이벤트를 outbox 테이블에 저장하고, 스케줄러가 처리 후 표시한다.
- **주의**: 삭제 cascade 리스너는 `@EventListener` + `@Transactional(MANDATORY)` 동기 방식이라 이 문제가 없다. 알림 **생성** 경로에만 해당한다.

### L26. `Recruitment.status`의 `final`로 기동 시 Hibernate 경고

- **위치**: `Recruitment.status` (`final var ... private set`)
- **문제**: `private set`을 쓰려고 `final`을 붙여, 기동 시 "Getter methods of lazy classes cannot be final" 경고가 난다.
- **영향**: 지금은 이 엔티티를 프록시로 쓰는 곳이 없어 무해하다. 연관관계나 `getReferenceById`로 프록시를 쓰게 되면 지연 로딩이 동작하지 않는다.
- **권장 방향**: 프록시를 쓰게 될 때 캡슐화 방식을 바꾼다 (예: 백킹 필드와 읽기 전용 프로퍼티 분리).

### L27. 테스트가 통합 테스트 위주이고 테스트가 없는 기능이 있음

- **현황**
  - 테스트 클래스 28개 중 22개가 Testcontainers 통합 테스트이고, 전체 빌드에 약 3~6분 걸린다
  - 쓰기 API 중 DB에 실제로 반영되는지 확인하는 테스트가 없는 것이 있다. 응답만 보는 테스트로는 트랜잭션 설정 실수처럼 저장이 조용히 빠지는 결함을 잡지 못한다
- **권장 방향**: 도메인 규칙(엔티티 상태 전이, 계산 로직 등)은 단위 테스트로 빼고, 통합 테스트는 쿼리와 cascade 검증에 집중한다. 쓰기 API는 최소한 "실제로 DB가 바뀌는지"를 확인하는 통합 테스트를 하나씩 둔다.

### L28. 쓰이지 않는 코드, 설정, 의존성

- **코드**: `memberreview/enums/ReviewQueryType`, `MemberReviewRepository`의 `MemberReviewCount` (사용처 없음)
- **빌드**: asciidoctor 플러그인, `spring-restdocs-mockmvc`, snippetsDir 설정 (REST Docs 테스트와 `src/docs`가 없음)
- **설정**
  - `application-local.yaml`의 `org.hibernate.type.descriptor.sql.BasicBinder` 로그 설정은 Hibernate 6에서 쓰이지 않는다 (현재 이름은 `org.hibernate.orm.jdbc.bind`)
  - `application.yaml`의 `batch_size`, `order_inserts`는 엔티티 키가 IDENTITY라 INSERT 배치에 적용되지 않는다. 오해를 부를 수 있다
- **권장 방향**: 지운다.

---

## 인프라

운영 서버 구성: EC2 `t3.small`(서울 리전, RAM 1.9GB, CPU 크레딧 unlimited) 한 대에 nginx, 앱, MySQL, Redis, fail2ban 컨테이너를 함께 띄우고, EBS gp3 20GB 볼륨 하나를 쓴다. 이미지는 S3 `waggle-server` 버킷(공개 읽기, `prod/`와 `local/` 경로)에 둔다. AWS 설정은 읽기 전용 IAM 사용자로, 서버 내부는 SSH 읽기 명령으로 확인했다.

평상시 메모리: 앱 약 600MB, MySQL 약 400MB, 나머지 컨테이너 합계 약 30MB. 사용 가능 메모리는 약 400MB이고, 2GB 스왑 파일(`/swapfile`, `vm.swappiness=10`)을 메모리가 모자랄 때의 안전판으로 둔다.

백업과 장애 대응 구성
- DLM 정책 `policy-08a73cb9ddb699ce3`: 태그 `Name=waggle-backend`인 인스턴스의 볼륨을 매일 18:00 UTC(한국 03:00)에 스냅샷하고 7개 보관
- 인스턴스 종료 보호 켜짐. 볼륨은 `DeleteOnTermination = true`라 보호를 끄고 종료하면 볼륨도 삭제됨
- CloudWatch 알람: `waggle-ec2-cpu-high`(CPU 80%), `waggle-ec2-status-instance`(인스턴스 상태 검사 실패 시 재부팅), `waggle-ec2-status-system`(시스템 상태 검사 실패 시 복구). 모두 SNS `waggle-server-health-alarm` → Lambda로 알림
- S3 `waggle-server` 버저닝 켜짐. 이전 버전은 30일 뒤 삭제

### I1. DB 백업이 EBS 스냅샷뿐이고 복원을 시험한 적이 없음

- **위치**: DLM 스냅샷 정책
- **문제**: 백업은 볼륨 스냅샷 하나뿐이다. MySQL이 쓰는 중에 찍히므로 복원하면 InnoDB 크래시 복구를 거친다. 실제로 스냅샷에서 볼륨을 만들어 DB를 띄워 본 적이 없다.
- **영향**: 복원이 필요한 순간에 절차를 처음 해 보게 된다. 스냅샷은 하루 단위라 마지막 스냅샷 이후의 데이터는 잃는다.
- **확인**: AWS 설정 조회
- **권장 방향**: 최근 스냅샷으로 임시 볼륨을 만들어 다른 인스턴스(또는 로컬 Docker)에서 MySQL을 띄워 보고, 절차를 운영 문서로 남긴다. 테이블 단위 복구가 필요해지면 `mysqldump`를 별도 비공개 S3 버킷으로 올리는 논리 백업을 추가한다.
- **다시 볼 시점**: 데이터가 쌓여 하루치 유실도 문제가 될 때, 또는 운영 담당자가 바뀔 때

### I2. 앱이 응답하지 않아도 인스턴스가 살아 있으면 알 수 없음

- **위치**: CloudWatch 알람
- **문제**: 상태 검사 알람은 인스턴스나 하드웨어가 멈출 때만 울린다. 앱 컨테이너만 멈추거나 응답이 느려진 경우는 잡지 못한다. 메모리는 CloudWatch 에이전트가 없어 수집되지 않고, SSM 관리 대상이 아니라 SSH가 막히면 콘솔에서 할 수 있는 일이 재부팅뿐이다.
- **영향**: 앱이 응답하지 않아도 CPU가 80%를 넘지 않는 한 사용자 문의로만 알게 된다.
- **확인**: AWS 설정 조회
- **권장 방향**: 외부에서 `/actuator/health`를 확인하는 업타임 체크(Route 53 상태 검사나 무료 외부 서비스)를 두고 같은 SNS 주제로 알린다. 메모리를 보려면 CloudWatch 에이전트를 설치한다. SSM은 I5와 함께 본다.
- **다시 볼 시점**: 앱만 멈춘 장애를 사용자 문의로 처음 알게 됐을 때

### I3. 배포가 앱을 내린 뒤에 이미지를 빌드하고 동시 실행을 막지 않음

- **위치**: `.github/workflows/deploy.yml`
- **문제**: 서버에서 `docker-compose down` → `docker-compose build --no-cache` → `up -d` 순서로 실행한다. nginx 이미지까지 기반 이미지부터 다시 받는다. 워크플로에 `concurrency`가 없고, 배포 빌드는 테스트를 건너뛴다(`-x test`, 테스트는 PR에서만 실행).
- **영향**
  - 빌드 시간만큼 중단 시간이 길어진다
  - Docker Hub 요청 한도나 장애로 빌드가 실패하면 서비스가 내려간 채로 남는다
  - 머지가 연달아 일어나면 두 배포가 동시에 내리고 올린다
- **확인**: 워크플로 파일 확인. 배포 빌드가 테스트를 건너뛰는 것은 main ruleset이 `spotless`와 `test`를 필수 상태 체크로 요구해 머지 전에 테스트 통과가 보장되므로 문제없다
- **권장 방향**: 중단 배포 방식은 유지하되 순서를 `build` → `down` → `up`으로 바꾸고 `--no-cache`를 뺀다. `concurrency` 그룹을 추가한다. 파일 몇 줄이라 비용이 작아 먼저 해도 된다. `--no-cache`를 빼면 배포마다 바뀌는 디스크 블록이 줄어 스냅샷 비용도 준다.
- **다시 볼 시점**: 배포 중단 시간이 문의로 이어지거나 배포가 실패해 서비스가 내려간 채로 남은 적이 생길 때. 무중단 배포가 필요해지면 R2와 CLAUDE.md 9절의 단일 인스턴스 전제를 함께 본다

### I4. 앱 컨테이너에 JVM 메모리 설정과 healthcheck가 없음

- **위치**: `Dockerfile` (`java -jar app.jar`), `docker-compose.yml`의 `app` 서비스
- **문제**: 힙은 호스트 메모리의 기본 25%(약 480MB)로 암묵적으로 정해지고, 스레드와 메타스페이스는 제한이 없다. OutOfMemoryError가 나도 프로세스가 살아 있어 `restart: unless-stopped`가 동작하지 않는다. healthcheck가 없어 응답하지 않는 앱도 "실행 중"으로 보인다. 다섯 컨테이너 모두 메모리 제한이 없어, 한 컨테이너가 메모리를 늘리면 MySQL까지 함께 압박받는다.
- **확인**: 저장소 파일과 서버 확인. 운영 앱 컨테이너의 실행 명령이 `java -jar app.jar`이고, 전 컨테이너의 메모리 제한이 0(무제한)이다. healthcheck는 MySQL과 fail2ban에만 있다
- **권장 방향**: `-XX:MaxRAMPercentage`(또는 `-Xmx`)와 `-XX:+ExitOnOutOfMemoryError`를 명시하고, compose에 `mem_limit`과 `/actuator/health` 기반 healthcheck를 둔다.
- **다시 볼 시점**: 인스턴스 유형을 바꾸거나 컨테이너를 추가할 때, 또는 앱이 응답 없이 살아 있는 장애가 생길 때

### I5. SSH 포트가 전체 인터넷에 열려 있음

- **위치**: 보안 그룹 `sg-06165af210044cdab`
- **문제**: 22번 포트 인바운드가 `0.0.0.0/0`이다. 80, 443은 서비스용이라 공개가 맞다.
- **현재 판단**: 관리자 PC의 IP가 DHCP로 계속 바뀌고, GitHub Actions 배포도 매번 다른 IP에서 SSH로 접속하므로 IP 제한 대신 전체 허용 + pem 키 인증 + fail2ban으로 막고 있다. sshd는 비밀번호 로그인이 꺼져 있어(`passwordauthentication no`) 키가 유출되지 않으면 안전하다.
- **영향**: 무차별 대입 시도가 계속 들어와 로그와 CPU를 조금 쓴다. pem 키 하나가 유출되면 누구나 어디서든 접속할 수 있다.
- **확인**: AWS 설정 조회와 서버의 `sshd -T` 확인
- **권장 방향**: SSM Session Manager로 접속하게 바꾸고 22번을 닫는다. IP가 아니라 IAM 권한으로 접속을 제어하므로 IP가 바뀌어도 문제가 없다. 인스턴스 역할에 `AmazonSSMManagedInstanceCore`를 붙이면 되고, 콘솔 접속 경로가 생겨 I2의 복구 수단도 된다. GitHub Actions 배포도 SSH 대신 `aws ssm send-command`로 바꿔야 한다.
- **다시 볼 시점**: 배포 방식을 바꿀 때, pem 키를 가진 사람이 팀을 떠날 때 (키 교체 필요)

### I6. 고정 IP(EIP)가 없어 인스턴스를 재시작하면 공인 IP가 바뀜

- **위치**: EC2 `waggle-backend` (EIP 0개)
- **문제**: 중지 후 시작하면 공인 IP가 바뀌어 `api.waggle.lol`의 DNS 레코드와 배포 워크플로의 호스트 값을 고쳐야 한다. 재부팅은 IP가 유지된다.
- **확인**: AWS 설정 조회
- **권장 방향**: EIP를 할당해 연결한다. 2024년부터 공인 IPv4는 EIP 여부와 상관없이 같은 요금이라 추가 비용이 없다.
- **다시 볼 시점**: 인스턴스 유형 변경이나 하드웨어 장애로 중지 후 시작이 필요할 때. 미리 해 두면 그때 DNS 전파를 기다리지 않아도 된다

---

## 참고 (결함은 아니지만 알아 둘 구조)

### R1. 삭제 cascade가 동기 이벤트 리스너라 실행 순서가 코드에 드러나지 않음

- 모집글, 팀, 댓글 삭제와 탈퇴의 연관 데이터 정리를 `@EventListener` + `@Transactional(propagation = MANDATORY)` 리스너들이 맡는다.
- 도메인끼리 직접 의존하지 않는 대신, 무엇이 어떤 순서로 실행되는지는 이벤트를 받는 쪽을 모두 찾아봐야 보인다.
- 순서가 중요한 곳이 있다. 예) `LikeCascadeListener.onUserDeactivated`는 탈퇴자가 누른 좋아요만큼 카운터를 빼는 쿼리를 `likes` 행 삭제보다 먼저 실행해야 한다. 새 cascade를 추가할 때 같은 리스너 안의 순서와 다른 리스너와의 관계를 확인할 것

### R2. 값 채우기와 코드 전환을 한 마이그레이션에 묶는 방식은 중단 배포 전제

- V30(좋아요 수 비정규화)처럼 "컬럼 추가 → 기존 데이터로 값 채우기 → 새 코드가 그 컬럼을 갱신"을 한 번에 배포해도 안전한 이유는, 배포가 앱을 내린 뒤 올리는 방식이라 값을 채우는 동안 옛 코드가 데이터를 쓰지 않기 때문이다.
- 무중단 배포(블루그린, 롤링)로 바꾸면 이런 마이그레이션은 단계를 나눠야 한다. 새 코드가 먼저 양쪽에 쓰게 배포하고, 그다음 값을 채우고, 마지막에 읽기를 전환하는 식이다. 단일 인스턴스 전제는 CLAUDE.md 9절 참고
