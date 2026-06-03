# Gourming Backend

Gourming 프로젝트의 백엔드 서버입니다.

Spring Boot 기반 REST API 서버로, 사용자 인증/인가, 장소 검색, 리뷰, 댓글, 좋아요, 팔로우, 맛집 저장, 공지/이벤트 게시글 기능을 담당합니다.

## 기술 스택

| 구분        | 기술                                                          |
| ----------- | ------------------------------------------------------------- |
| Language    | Java 21                                                       |
| Framework   | Spring Boot 3.5.14                                            |
| Build Tool  | Maven                                                         |
| Database    | MySQL                                                         |
| Persistence | MyBatis                                                       |
| Security    | Spring Security                                               |
| API Docs    | Springdoc OpenAPI / Swagger UI                                |
| Test        | JUnit 5, Spring Boot Test, MyBatis Test, Spring Security Test |
| Utility     | Lombok, Validation                                            |

## 프로젝트 구조

```text
backend
├── .mvn
│   └── wrapper
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com/ssafy/gourming
│   │   │       ├── aspect
│   │   │       ├── config
│   │   │       ├── controller
│   │   │       ├── interceptor
│   │   │       ├── model
│   │   │       │   ├── dao
│   │   │       │   ├── dto
│   │   │       │   └── service
│   │   │       ├── security
│   │   │       └── GourmingApplication.java
│   │   └── resources
│   │       └── application.properties
│   └── test
│       └── java
│           └── com/ssafy/gourming
│               └── GourmingApplicationTests.java
├── target
│   ├── classes
│   ├── generated-sources
│   ├── generated-test-sources
│   └── test-classes
├── pom.xml
├── mvnw
└── mvnw.cmd
```

`src/main/java/com/ssafy/gourming` 아래에서 계층별 패키지를 관리합니다.
`target` 디렉터리는 Maven 빌드 및 테스트 실행 시 생성되는 산출물입니다.

## 주요 기능

- 인증/인가: 회원가입, 로그인, 로그아웃, 토큰 재발급, 아이디/비밀번호 찾기
- 장소: 카카오 장소 기반 검색, 장소 상세 조회, 장소별 리뷰 조회
- 리뷰: 리뷰 작성/조회/수정/삭제, 이미지 업로드, 평점, 좋아요
- 댓글: 리뷰 댓글 작성/조회/수정/삭제
- 유저: 프로필 조회/수정, 유저 검색, 팔로워/팔로잉 관리
- 맛집: 내 맛집 저장/수정/삭제, 맛집 그룹 관리, 유저별 맛집 조회
- 게시글: 공지사항 및 이벤트 게시글 조회/관리

## 주요 도메인

| 도메인       | 설명                                     |
| ------------ | ---------------------------------------- |
| `Users`      | 회원 계정, 프로필, 핸들, 자기소개 정보   |
| `Places`     | 카카오 장소 기반 식당/카페 정보          |
| `Reviews`    | 장소에 대한 방문 리뷰, 이미지, 평점 정보 |
| `Comments`   | 리뷰에 작성되는 댓글                     |
| `Likes`      | 리뷰 좋아요 정보                         |
| `Follows`    | 유저 팔로우/팔로잉 관계                  |
| `Groups`     | 유저가 맛집을 분류하는 그룹              |
| `GoodPlaces` | 유저가 저장한 맛집과 그룹 연결 정보      |
| `Posts`      | 공지사항 및 이벤트 게시글                |

## 실행 환경

- JDK 21
- MySQL 8.x
- Maven Wrapper 사용 권장

## 로컬 설정

공통 설정은 `src/main/resources/application.properties`에서 관리합니다.

개인별 DB 계정, 비밀번호, 로컬 환경 값은 Git에 올리지 않고 `src/main/resources/application-local.properties`에 작성합니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/gourming?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=DB_USERNAME
spring.datasource.password=DB_PASSWORD

jwt.secret=gourming-secret-key-minimum-32-characters-long!!
jwt.expiration-ms=3600000
```

`application.properties`에서 아래 설정으로 로컬 설정 파일을 선택적으로 불러옵니다.

```properties
spring.config.import=optional:classpath:application-local.properties
```

## 실행 방법

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
./mvnw spring-boot:run
```

기본 실행 주소:

```text
http://localhost:8080
```

## 테스트

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

macOS / Linux:

```bash
./mvnw test
```

## 빌드

Windows PowerShell:

```powershell
.\mvnw.cmd clean package
```

macOS / Linux:

```bash
./mvnw clean package
```

빌드 결과물은 `target/` 디렉터리에 생성됩니다.

## API 문서

서버 실행 후 Swagger UI에서 API 문서를 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

구현 기준 API 목록은 루트의 `명세/API 명세서` 디렉터리에 있는 CSV 파일을 참고합니다.

## 주요 설정

### 파일 업로드

현재 multipart 업로드 설정이 포함되어 있습니다.

| 설정               | 값                     |
| ------------------ | ---------------------- |
| 임시 저장 위치     | `${user.home}/uploads` |
| 파일 1개 최대 크기 | `10MB`                 |
| 요청 1회 최대 크기 | `50MB`                 |

### MyBatis

Mapper XML은 아래 경로에서 읽습니다.

```properties
mybatis.mapper-locations=classpath:/mappers/**/*.xml
```

DTO/VO 타입 별칭 패키지는 아래와 같이 설정되어 있습니다.

```properties
mybatis.type-aliases-package=com.ssafy.gourming.domain.*.dto,com.ssafy.gourming.domain.*.vo
```

## 참고 문서

프로젝트 루트의 `명세` 디렉터리에 있는 Markdown/CSV 명세서를 기준으로 백엔드 기능을 구현합니다.

| 문서                             | 설명                        |
| -------------------------------- | --------------------------- |
| `명세/요구사항 명세서`           | 전체 기능 요구사항          |
| `명세/API 명세서`                | API 분류, 메서드, 경로      |
| `명세/데이터 구조 명세서`        | 데이터 모델 및 주요 필드    |
| `명세/용어 사전`                 | 프로젝트 용어와 네이밍 기준 |
| `명세/페이지별 기능 논리 흐름도` | 화면별 기능 흐름            |

## 협업 규칙

- 민감 정보는 `application-local.properties`에 작성하고 Git에 커밋하지 않습니다.
- API 변경 시 Swagger 문서와 README 내용을 함께 갱신합니다.
- DB 스키마나 주요 정책이 변경되면 루트의 명세서와 구현 내용을 함께 확인합니다.
- 용어와 네이밍은 `명세/용어 사전`을 우선 기준으로 맞춥니다.
