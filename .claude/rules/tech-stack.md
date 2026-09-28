# 기술 스택 지침

다음은 Spring Initializr에서 생성하고 실제 빌드로 검증한 구현 기준이다. 빌드 설정과 Wrapper를 버전의 기준으로 사용한다.

| 영역 | 기준 |
| --- | --- |
| JVM | JDK 21, toolchain과 Kotlin JVM target 일치 |
| 언어·빌드 | Kotlin 2.3.21, Gradle 9.7.1 Kotlin DSL, Gradle Wrapper |
| 프레임워크 | Spring Boot 4.1.1, Spring MVC |
| 기본 패키지·Gradle group | `com.handys.assignment` (메인·테스트 동일) |
| 영속성·검증 | Spring Data JPA, Jakarta Validation, H2 메모리 DB |
| 테스트 | Boot 기본 JUnit Jupiter, Spring Boot Test, MockMvc |

## 버전·의존성

- 프로젝트 생성 시 Spring Initializr에서 Boot 4.1.1에 대응하는 Kotlin과 Wrapper 버전을 확인하여 정확한 버전으로 고정한다. 지원 조합을 확인하지 못하면 임의 버전으로 진행하지 말고 제약을 보고한다.
- `latest`, `+`, SNAPSHOT 등 동적·미리보기 버전은 사용하지 않는다. Spring 의존성과 테스트 라이브러리는 Boot의 의존성 관리를 우선한다.
- Kotlin JVM·Spring·JPA 플러그인 버전을 일치시킨다. JPA 엔티티의 no-arg 생성자와 프록시에 필요한 open 처리를 설정으로 지원한다.
- 해당 Boot 버전의 공식 starter와 JSON Kotlin 지원을 사용한다. 다른 메이저 버전의 import·테스트 설정을 확인 없이 복사하지 않는다.
- 버전의 최종 기준은 생성된 빌드 설정과 Wrapper다. 변경 시 README와 이 문서를 함께 갱신한다.

## 범위

- 로컬 시연과 테스트에 H2를 사용한다. 재시작 시 초기화되는 정책을 유지하고 실제 고객 데이터를 넣지 않는다.
- WebFlux, 코루틴, Redis, 메시지 브로커, 외부 LLM API, 컨테이너, 클라우드 인프라를 기본 의존성에 추가하지 않는다.
- 예약·청소 작업은 샘플 데이터로 준비한다. 인증·외부 연동·배포 등 범위 확장은 사용자 요청 없이 도입하지 않는다.

근거: [Spring Boot 요구사항](https://docs.spring.io/spring-boot/system-requirements.html), [Kotlin 지원](https://docs.spring.io/spring-boot/reference/features/kotlin.html), [Spring Initializr](https://start.spring.io/).
