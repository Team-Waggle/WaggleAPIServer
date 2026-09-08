# 모집글 마감 기한

## 0. 요약

모집글(`Post`)에 마감 기한을 추가한다. **API는 마감일(`LocalDate`)을 주고받고 서버가 만료 시각(`Instant`)으로 변환해 `expires_at`에 저장한다.** `NULL`은 무기한 모집을 뜻하는 정식 값이다.

마감 여부는 저장하지 않고 조회 시점에 `expires_at <= now`로 파생한다. 크론이 있으나 **표시 일치용이며 판정 권위는 파생에 있다.**

## 1. 현황

`Post`에는 상태 필드가 없다. "모집 중"은 자식 `Recruitment`에서 파생된다.

```kotlin
// PostSimpleResponse.of / PostDetailResponse.of / TeamPostSimpleResponse.of
recruiting = recruitments.any { it.status == RecruitmentStatus.RECRUITING }

// ApplicationService.applyToTeam — 지원 차단은 포지션 단위
if (!recruitment.isRecruiting()) throw BusinessException(INVALID_STATE, "...")
```

즉 현재 마감은 **포지션별 수동 `CLOSED`**로만 존재한다. 여기에 모집글 단위의 시간 기반 마감을 얹는다.

## 2. 결정 사항

### 2.1 API는 날짜, 저장은 인스턴트

사용자가 고르는 값은 달력의 **날짜 하나**다. `Instant`는 비교·크론·인덱스를 위한 저장 계층의 선택이므로 API 계약으로 새어나가면 안 된다.

```
요청/응답:  deadline: LocalDate     "2026-09-30"
저장:       expires_at DATETIME(6)  2026-09-30T15:00:00Z
```

> **이전 결정을 뒤집은 항목이다.** 초안은 "서버는 타임존을 몰라야 한다"는 이유로 인스턴트를 받고 KST 자정인지 검증(`@KstMidnight`)했다. 그러나 **검증기를 두는 순간 서버는 이미 KST를 안다.** 서버가 어차피 안다면 검증보다 변환이 엄격히 낫다.

| | 인스턴트 + 검증 (기각) | 날짜 + 서버 변환 (채택) |
|---|---|---|
| 서버가 KST를 아는가 | 안다 (검증기) | 안다 (변환) |
| 클라이언트가 틀릴 수 있는가 | 가능 — `00:00Z`, `14:59:59Z` 등 오답이 여럿 | 불가능 — 날짜에는 시각이 없다 |
| 표시 보정 | 클라이언트가 하루를 빼야 함 | 불필요 |
| 규칙이 사는 곳 | 서버(검증) + 클라이언트(생성·표시) 2곳 | 서버 1곳 |

일반화하면 **호출자가 내 변환을 제대로 했는지 검증하고 있다면 그 변환은 경계의 반대편에 있다.** 검증으로만 존재하던 400 응답도 함께 사라진다.

### 2.2 컬럼명 `expires_at`, API 필드명 `deadline`

API 타입과 저장 타입이 다르므로 이름도 각자의 타입에 맞춘다.

| | 이름 | 타입 | 의미 |
|---|---|---|---|
| API | `deadline` | `LocalDate` | 마감**일** — 기획 용어 그대로 |
| 컬럼 | `expires_at` | `DATETIME(6)` | 만료 **시각** |

`expires_at`은 `invitations.expires_at`과 같은 이름·같은 성격이다. 엔티티의 파생 프로퍼티 `isExpired`와도 짝이 맞는다.

> 초안은 `_at`을 기각하며 "미래시제 `_at`은 코드베이스에 전례가 없음"을 근거로 들었으나 **사실이 아니다.** `invitations.expires_at`(V1 초기 스키마)이 아직 일어나지 않은 만료 시점을 담고 있다.

### 2.3 정밀도 `DATETIME(6)`

값이 항상 KST 자정이라 소수부는 언제나 0이고 `DATETIME(0)`으로도 손실이 없다. 그럼에도 `(6)`을 쓰는 이유는 **마이그레이션 전체의 datetime 컬럼 54개가 모두 `(6)`이고**, CLAUDE.md가 "컬럼이 `datetime(6)`이므로 `UTC_TIMESTAMP(6)`을 쓸 것"을 규약으로 두기 때문이다. 3바이트를 위해 54:1의 예외를 만들지 않는다.

### 2.4 배타적 경계 — 마감일 익일 KST 0시

```
마감일 2026-09-30  →  KST 2026-10-01 00:00:00  =  UTC 2026-09-30T15:00:00Z
```

반열린 구간 `[시작, 마감)`이므로 만료 판정은 `expires_at <= now`다. 변환은 서버가 소유하므로 **클라이언트는 이 규칙을 알 필요가 없다.**

### 2.5 `NULL` = 무기한 모집

기존 모집글은 마감 기한 없이 생성되어 있고, 무기한 모집은 앞으로도 유효하다. 따라서 `NULL`은 레거시 잔여물이 아니라 정식 값이다.

- 컬럼: `NULL` 허용
- 요청 DTO: optional (`@field:NotNull` 없음)
- 백필 없음

### 2.6 판정은 파생, 크론은 표시 일치용

기획의 "마감일이 지나면 자동으로 마감 상태로 넘어간다"는 파생 판정으로 충족된다. `recruiting` 계산과 지원 차단이 모두 `isExpired`를 보므로 **크론 없이도 정확하다.**

그 위에 크론을 얹어 기한이 지난 모집글의 `recruitments.status`를 `CLOSED`로 물리적으로 전환한다. 응답의 포지션별 상태가 `recruiting = false`와 어긋나 보이지 않게 하기 위함이다.

> **크론은 안전장치가 아니다.** 지연·실패해도 지원은 이미 파생 판정이 막고 있어 구멍이 생기지 않는다. 크론을 유일한 방어선으로 삼으면 배치 주기만큼 마감된 글에 지원이 열리는 창이 생긴다.

## 3. 스키마

```sql
-- V27__add_posts_expires_at.sql
ALTER TABLE posts
    ADD COLUMN expires_at DATETIME(6) NULL;
```

인덱스는 만들지 않는다. 커서 페이지네이션이 `id` 기준이라 `expires_at`은 필터일 뿐이고 선택도도 낮다. 마감된 모집글을 목록에서 제외하거나 마감임박순 정렬을 도입하면 그때 재검토한다.

## 4. 변환 규칙

날짜 ↔ 인스턴트 변환은 한 곳에만 둔다. 두 계층에 흩어지면 화면의 마감일과 실제 차단 시점이 어긋난다.

```kotlin
object Deadline {
    private val KST = ZoneId.of("Asia/Seoul")

    fun toExpiresAt(deadline: LocalDate): Instant = deadline.plusDays(1).atStartOfDay(KST).toInstant()

    fun toDeadline(expiresAt: Instant): LocalDate = expiresAt.atZone(KST).toLocalDate().minusDays(1)

    fun todayExpiresAt(): Instant = toExpiresAt(LocalDate.now(KST))
}
```

## 5. 엔티티

```kotlin
@Column(name = "expires_at")
var expiresAt: Instant? = null

// NULL은 무기한 모집이라 만료가 아니고, 배타적 경계라 만료 시각에 도달하면 만료됨
val isExpired: Boolean
    get() = expiresAt?.let { it <= Instant.now() } == true

fun checkNotExpired() {
    if (isExpired) {
        throw BusinessException(ErrorCode.INVALID_STATE, "Post is closed: $id")
    }
}
```

`checkOwnership`과 같은 결의 가드 메서드이고, `isExpired`는 `Team.isCompleted`·`Comment.isReply` 전례와 같은 파생 프로퍼티다.

`Instant.now()`를 엔티티 안에서 호출하므로 테스트는 시스템 시계에 묶인다. 서비스에서 `now`를 주입하는 대안이 있으나 기존 파생 프로퍼티 전례와 어긋나고 호출처가 늘어나 채택하지 않는다. 테스트는 `now` 기준 상대 시각으로 작성한다.

`update()` 시그니처에 `expiresAt`이 추가된다.

## 6. API

### 요청

`PostCreateRequest` / `PostUpdateRequest`에 **필수 필드**로 추가한다. 값은 nullable이지만 키는 반드시 보내야 한다.

```kotlin
@JsonProperty(required = true)
@Schema(
    description = "모집 마감일. 그날 24시(KST)까지 모집. 무기한이면 null",
    example = "2026-09-30",
    types = ["string", "null"],
)
val deadline: LocalDate?,
```

기본값을 두지 않는 이유는 수정 API 때문이다. `updatePost`는 전체 치환이라 키를 빠뜨리면 기존 마감일이 조용히 지워진다. 키를 강제하면 무기한 전환은 `null`을 명시해야 하고, 누락은 400이 된다.

**필드는 반드시 목록 맨 끝에 둔다.** `PostService`가 위치 기반 구조분해를 쓰기 때문이다.

```kotlin
val (teamId, title, content, recruitments, deadline) = request   // createPost
val (title, content, recruitments, deadline) = request           // updatePost
```

중간에 끼우면 타입이 맞는 경우 조용히 어긋난다. CLAUDE.md §4의 "할당되는 자리에 끼워넣기"는 응답 DTO 정적 팩토리에 대한 규약이고, 여기는 구조분해 안전성이 우선한다.

### 과거 마감일 거부 — 애너테이션이 아니라 도메인 검사

**마감일은 과거로 지정할 수 없다.** 즉시 마감은 `updatePostRecruitmentStatus`가 담당하며, 마감일을 과거로 미는 것은 그 경로를 우회하는 조작이다.

`@FutureOrPresent`를 쓰지 않는다. **Hibernate Validator는 JVM 기본 타임존으로 "오늘"을 판단하는데 서버 컨테이너는 UTC다**(`docker-compose.yml`의 `TZ: Asia/Seoul`은 DB 컨테이너에만 있다). KST 새벽 0~9시에는 KST 기준 어제 날짜가 통과해 생성 즉시 만료된 모집글이 된다.

대신 생성·수정 모두 같은 KST 기준 함수를 쓴다.

```kotlin
// Deadline — 오늘을 마감일로 고르는 것은 정당하므로 오늘의 경계까지는 과거가 아님
fun isPast(expiresAt: Instant): Boolean = expiresAt < todayExpiresAt()
```

| 경로 | 검사 위치 | 조건 |
|---|---|---|
| 생성 | `PostService.createPost` | 값이 있으면 항상 |
| 수정 | `Post.update()` | **값이 바뀔 때만** |

수정에서 "값이 바뀔 때만"인 이유는, 수정 요청이 기존 마감일을 그대로 실어 보내기 때문이다. 무조건 검사하면 이미 마감된 모집글의 본문 오타조차 고칠 수 없다.

| 요청 | 판정 |
|---|---|
| 마감일 그대로 두고 본문만 수정 | 통과 — 마감된 글도 편집 가능 |
| 과거 날짜로 지정·변경 | **거부** (`INVALID_INPUT_VALUE`) |
| 미래로 연장 / 오늘로 / 무기한(`NULL`)으로 | 통과 |

`INVALID_STATE`가 아니라 **`INVALID_INPUT_VALUE`**다. 과거 마감일은 잘못된 입력이지 잘못된 상태가 아니며, 클라이언트가 입력 검증 실패와 비즈니스 상태 오류를 구분할 수 있어야 한다.

### 응답

`PostSimpleResponse`, `PostDetailResponse`, `TeamPostSimpleResponse`, **`BookmarkedPostResponse`** 에 `deadline`을 노출하고 `recruiting` 계산에 만료를 반영한다.

```kotlin
recruiting = !post.isExpired && recruitments.any { it.status == RecruitmentStatus.RECRUITING }
deadline = post.expiresAt?.let { Deadline.toDeadline(it) }
```

네 번째(`BookmarkedPostResponse`)를 빠뜨리면 북마크 목록에서만 마감된 글이 "모집 중"으로 보인다.

정적 팩토리 파라미터는 CLAUDE.md §4대로 생성자 할당 순서와 같은 자리에 끼운다.

### 지원 차단

`ApplicationService.applyToTeam`에서 `teamId` 검증 직후, 비관적 락 획득 **전**에 검사한다.

```kotlin
post.checkNotExpired()
```

락을 잡기 전에 튕겨내 불필요한 잠금을 피한다.

## 7. 마감 이후 허용/차단

기획의 "마감 뒤에도 모집글은 상세조회에서 계속 볼 수 있다 (지원만 불가)"를 따른다.

| 동작 | 마감 후 | 근거 |
|---|---|---|
| 모집글 상세·목록 조회 | 허용 | 기획 명시 |
| 댓글 작성·조회 | 허용 | 마감은 지원에 대한 제약이며 토론과 무관 |
| 모집글 수정 | 허용 | 오타 수정·기한 연장 경로가 필요 |
| **지원** | **차단** | 기획 명시 |
| 포지션 마감 | 허용하되 무효 | 이미 파생으로 마감된 상태 |

두 축(포지션 상태 · 기한)이 AND로 결합한다. 기한을 연장하면 아직 `RECRUITING`인 포지션은 다시 모집 중이 된다.

### 수동 마감은 마감일도 당긴다

기한이 남은 모집글을 수동 마감하면 포지션만 닫는 것이 아니라 `expires_at`을 **오늘 24시(KST)** 로 당긴다. 마감일 표시가 오늘이 된다.

```kotlin
fun limitDeadlineToToday() {
    val today = Deadline.todayExpiresAt()
    val current = expiresAt
    if (current == null || current > today) {
        expiresAt = today
    }
}
```

**어제로 당기지 않는 이유는 지원 이력과 어긋나기 때문이다.** 오늘 들어온 지원이 마감일 이후 기록이 되어, 나중에 "이 지원은 왜 마감 후에 들어왔나"라는 질문에 답할 수 없게 된다. 오늘 24시로 두면 기존 지원의 `created_at`이 모두 `expires_at` 이하로 유지된다.

`isExpired`는 오늘 밤까지 `false`로 남지만 실질 영향이 없다. 포지션이 전부 `CLOSED`이므로 `recruiting`은 이미 `false`이고, 지원 차단도 포지션 단위 검사가 담당한다.

> **이 안전성은 포지션 검사에만 의존한다.** 지원 검증을 `checkNotExpired()` 중심으로 바꾸면 기한이 남은 수동 마감 글에 지원이 뚫린다. `RecruitmentCloseTest`의 "수동 마감 후에는 기한이 남아도 지원할 수 없다"가 이 지점을 고정한다.

**이미 지난 기한은 건드리지 않는다.** 무조건 덮어쓰면 기한이 지난 모집글을 수동 마감할 때 마감일이 미래로 밀려 기록이 왜곡된다.

### 포지션 추가는 기한 기준으로 막는다

새 포지션은 `RECRUITING`으로 삽입되므로 마감된 모집글이 되살아난다. `updatePost`에서 신규 포지션이 있으면 `post.checkNotExpired()`로 막는다.

**포지션 상태가 아니라 기한을 기준으로 삼는다.** 수동 마감했지만 기한이 남은 글에 포지션을 추가하는 것은 정당한 조작이기 때문이다. 검사는 `post.update()` **이후**에 두어, 같은 요청에서 기한을 연장하며 포지션을 추가하는 경로를 허용한다.

| 상황 | 포지션 추가 |
|---|---|
| 기한 남음 (수동 마감 여부 무관) | 허용 — `RECRUITING`으로 들어감 |
| 기한 지남 | **거부** |
| 같은 요청에서 기한 연장 + 포지션 추가 | 허용 |

### 포지션 삭제 금지 두 가지

`updatePost`는 요청에 없는 포지션을 삭제한다. 여기에 가드가 없으면 두 가지 문제가 생긴다.

```kotlin
private fun checkDeletableRecruitment(recruitment: Recruitment) {
    if (!recruitment.isRecruiting()) { throw ... }
    if (applicationRepository.existsByPostIdAndPosition(...)) { throw ... }
}
```

**① `CLOSED` 포지션 삭제 금지** — 지웠다 다시 추가하면 `RECRUITING`으로 되살아나 재개 금지가 우회된다. `status`를 `private set`으로 막고 API에서 `RECRUITING` 요청을 거부한 것이 전부 무력해진다.

**② 지원서가 있는 포지션 삭제 금지** — `Application`은 `recruitment`가 아니라 `position`을 참조하므로 삭제해도 지원서가 남아, 대응하는 모집 정보가 없는 지원서가 생긴다. ①은 MANAGER 권한과 의도적인 2회 요청이 필요하지만 ②는 포지션 하나를 빼며 저장하는 것만으로 밟힌다.

> **부작용**: 수동 마감은 모든 포지션을 닫으므로, 마감된 모집글은 포지션 목록이 사실상 동결된다. 기한을 연장해 새 포지션을 추가할 수는 있으나 기존 포지션은 지원서가 없어도 지울 수 없다. 완화하려면 "지원서 없는 `CLOSED` 포지션은 삭제 허용"으로 좁혀야 하는데 그러면 ①의 우회로가 다시 열린다. 재개 기능이 도입되면 함께 재검토한다.

## 8. 함께 수정한 Recruitment 결함

마감 기한을 얹기 전 드러난 기존 결함들이다. 마감 정책과 직접 맞물려 같은 브랜치에서 처리했다.

| 결함 | 수정 |
|---|---|
| `CLOSED → RECRUITING` 재개가 가능했다 | `status`를 생성자에서 빼고 `private set`. `close()`만 상태를 바꾼다 |
| 재개 요청이 API로 통과했다 | `request.status != CLOSED`면 거부 |
| 상태가 섞이면 마감·재개가 **둘 다 영구 실패**했다 | 열린 포지션만 추려 닫고, 하나도 없을 때만 "이미 마감됨" 에러 |

세 번째가 실제 도달 가능한 버그였다. 마감된 모집글에 포지션을 추가하면 새 포지션만 `RECRUITING`이 되는데, 당시 `updateStatus`가 동일 상태에 예외를 던지고 `forEach`라 트랜잭션 전체가 롤백되어 어느 방향으로도 상태를 바꿀 수 없었다.

`status`에 `private set`을 걸려면 `final`이 필요하다. JPA allopen 플러그인이 프로퍼티를 open으로 만들어 `private set`과 충돌하기 때문이다.

## 9. 하위호환

| 변경 | 영향 |
|---|---|
| 응답에 `deadline` 추가 | additive. 기존 클라이언트 무영향 |
| 요청에 필수 `deadline` 추가 | **breaking.** 키를 안 보내던 클라이언트는 400. 무기한이면 `null`을 명시해야 한다 |
| `recruiting` 의미 확장 | 기존 행은 `expires_at IS NULL`이라 동작 불변 |
| 포지션 재개 거부 | 재개를 쓰던 클라이언트가 있으면 400. 기획상 재개는 허용되지 않는 조작이다 |

## 10. 변경 지점

```
src/main/resources/db/migration/V27__add_posts_expires_at.sql   신규

domain/post/Deadline.kt                                 신규 — 변환, todayExpiresAt, isPast
domain/post/Post.kt                                     expiresAt, isExpired, checkNotExpired,
                                                        limitDeadlineToToday, update() 과거 이동 금지
domain/post/dto/request/PostCreateRequest.kt            deadline (맨 끝)
domain/post/dto/request/PostUpdateRequest.kt            deadline (맨 끝)
domain/post/dto/response/PostSimpleResponse.kt          deadline 노출, recruiting 계산
domain/post/dto/response/PostDetailResponse.kt          동일
domain/post/dto/response/TeamPostSimpleResponse.kt      동일
domain/post/dto/response/BookmarkedPostResponse.kt      동일
domain/post/service/PostService.kt                      구조분해 + 변환 + 과거 마감일 거부
                                                        + 포지션 추가·삭제 가드 + 마감 로직 수정
domain/application/repository/ApplicationRepository.kt  existsByPostIdAndPosition
domain/application/service/ApplicationService.kt        마감 가드
domain/recruitment/Recruitment.kt                       status private set, close()
domain/recruitment/repository/RecruitmentRepository.kt  만료 모집글 벌크 마감
domain/recruitment/service/RecruitmentScheduler.kt      신규 — UTC 15시 크론
```

### 테스트

- `DeadlineTest` — 변환 왕복 대칭성, 경계(마감일 당일 KST 오후는 아직 만료 아님)
- `PostDeadlineTest` — `isExpired`의 `NULL`/미래/경계, 마감 시 `recruiting = false`, 지원 차단, 마감 후 조회 허용, 과거 마감일 거부와 그 예외(값 미변경·연장·무기한 전환), 포지션 추가 가드(마감됨 / 기한 남음 / 연장과 동시), 포지션 삭제 가드(`CLOSED` / 지원서 존재)
- `RecruitmentCloseTest` — 일괄 마감, 섞인 상태에서의 마감, 중복 마감 거부, 재개 거부, 크론 대상 선별, 수동 마감의 마감일 당김(오늘 / 무기한 / 이미 지난 기한), **수동 마감 후 지원 차단**

## 11. 이번 스코프에서 제외

- **마감 임박 알림** — 기획에 없음. 필요해지면 `NotificationScheduler` 전례를 따라 추가
- **목록에서 마감글 숨김** — 노출 유지하되 `recruiting = false`로 표시. 숨기려면 `findWithFilter`에 조건과 인덱스가 추가됨
- **`PostSort`에 마감임박순** — 기획에 없음. 추가 시 `expires_at` 인덱스 재검토 필요
- **자정 아닌 마감** — API가 날짜만 받으므로 표현 불가. 저장은 이미 인스턴트라 **마이그레이션 없이** optional 필드 추가만으로 열 수 있다
- **포지션별 마감 기한** — 모집글 단위로만 둔다. 필요해지면 `recruitments.expires_at`을 추가하고 모집글 기한을 상한으로 삼는 구조가 된다

## 12. 기획 대비 기존 코드의 결함 (별도 브랜치)

같은 기획서의 나머지 항목을 코드와 대조한 결과다. 마감 기한과 성격이 달라 이 브랜치에 포함하지 않는다.

| 항목 | 기획 | 현재 코드 | 실제 통과량 |
|---|---|---|---|
| 제목 | 100 byte | `@Size(max = 100)` | 한글 100자 = 300 byte |
| 상세 내용 | 7000 byte | `@Size(max = 10000)` | 한글 10000자 = 30000 byte |
| 모집 인원 | **모집글 전체 합계** 최대 15명 | `@Positive`만 | 상한 없음 |

`@Size`는 문자 수를 세므로 byte 기준 제약은 커스텀 애너테이션이 필요하다 (`UniquePosition`, `WebUrl`과 같은 자리에 `@ByteSize`).
