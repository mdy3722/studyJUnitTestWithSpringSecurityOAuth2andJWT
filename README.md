# ✍🏻 레포지토리의 주 목적 : JUni5를 활용하여 단위 테스트와 통합 테스트를 학습
1. 프로젝트의 요구사항에 맞게 먼저 서비스 코드를 작성 (즉, TDD 방식은 아님‼️)
2. 작성된 서비스 코드를 가장 작은 단위로 나누어 메소드 별로 단위 테스트를 진행
3. 단위 테스트가 성공한 후, 연관된 모듈/클래스 간의 통합 테스트 진행

위 작업을 기반으로 JUnit5에 보다 익숙해지고 적절한 테스트 코드를 작성하는 역량을 기르기 위함.

---

### 😊 요구사항과 구현할 기능
- Spring Security를 활용하여 인증/인가 시스템 구현
- OAuth-Client를 활용해 Google/Kakao OAuth 로그인 API 구현
- 일반 로그인도 허용
- REST API를 설계할 것
- Swagger 기반 API 자동 문서화

---
### 🔧 기술 스택
🔧 기술 스택

| 구분      | 기술                                      |
|-----------|-------------------------------------------|
| Backend   | Java 17, Spring Boot                      |
| DB        | PostgreSQL, JPA                           |
| Auth      | Spring Security, OAuth-Client, jjwt       |
| OAuth API | Google, Kakao                             |
| Test      | JUnit5, Mockito, AssertJ                   |

---

### 📖 스터디 방식
- 유튜브 강의
- Claude AI 활용
- 기술 블로그 활용

### 📝 스터디 하면서 작성한 기술 블로그
[단위테스트 테스트 코드 작성](https://velog.io/@mdy3722/Junit5%EC%99%80-AssertJ%EB%A5%BC-%ED%99%9C%EC%9A%A9%ED%95%98%EC%97%AC-%EB%8B%A8%EC%9C%84%ED%85%8C%EC%8A%A4%ED%8A%B8%EB%A5%BC-%EC%A7%84%ED%96%89)
[Mockito.Spy() 잘못된 사용으로 인한 문제 발생과 해결](https://velog.io/@mdy3722/%EC%9E%98%EB%AA%BB%EB%90%9C-Spy-%EC%82%AC%EC%9A%A9%EC%9D%B4-%EB%B6%80%EB%A5%B8-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%8B%A4%ED%8C%A8)

