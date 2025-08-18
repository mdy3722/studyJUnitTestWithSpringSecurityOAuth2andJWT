<h1 align="center">🔐 OAuth2 · JWT · Swagger · Redis · JUnit5 공부를 위한 실습</h1>
<p align="center">Spring Security 기반 인증/인가와 JUnit5 테스트 학습 레포지토리</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-17-007396">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F">
  <img alt="JUnit5" src="https://img.shields.io/badge/JUnit-5-25A162">
  <img alt="Redis" src="https://img.shields.io/badge/Redis-RT%20Store-EF3934">
  <img alt="Swagger" src="https://img.shields.io/badge/OpenAPI-Swagger%20UI-85EA2D">
</p>

---

## 📌 레포 목적
1. 프로젝트 요구사항에 맞춰 서비스 코드를 먼저 작성(TDD 아님)
2. 서비스를 가장 작은 단위로 나눠 **메서드 단위 테스트** 작성
3. 단위 테스트 통과 후 **모듈/클래스 간 통합 테스트** 수행

→ JUnit5에 익숙해지고 “적절한 테스트 코드”를 작성하는 역량을 목표로 합니다.

---

## ✅ 요구사항 & 구현
- Spring Security로 **인증/인가**
- OAuth Client로 **Google/Kakao OAuth 로그인**
- **일반 로그인**도 지원
- **REST API** 설계
- **Swagger(OpenAPI) 자동 문서화**
- **Redis**에 **Refresh Token 보관**
- **사용자 정보 조회**

---

## 🧪 진행한 단위 테스트
- **CustomOAuth2UserService**  
  신규 회원 → 회원가입, 기존 회원 → 로그인, 미지원 소셜 예외 처리
- **UserService**  
  일반 회원가입, 토큰 재발행, 사용자 정보 조회, 로그아웃
- **JwtUtil**  
  Access/Refresh 토큰 생성, 검증, 사용자 ID 추출

---

## 🛠 기술 스택
| 구분 | 기술 |
|---|---|
| Backend | Java 17, Spring Boot |
| DB | PostgreSQL, JPA, Redis |
| Auth | Spring Security, OAuth-Client, jjwt |
| OAuth API | Google, Kakao |
| Test | JUnit5, Mockito, AssertJ |

---

## 🚀 빠른 시작
```bash
# 실행
./gradlew bootRun

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## 🔑 인증 플로우 요약
- 소셜 로그인 진입: GET /oauth2/authorization/{kakao|google}
- 콜백(백엔드 수신): /login/oauth2/code/{registrationId}
- 일반 로그인: POST `/api/auth/login`
- 로그인 성공 후 access token은 Request Body, refresh token은 HttpOnly 쿠키로 응답
- 토큰 재발급: POST /api/users/refresh (refresh token 쿠키 필요)
- 로그아웃: POST /api/users/logout
  
---

## 📸 API 테스트 스냅샷
<details> <summary>브라우저 테스트</summary> <figure> <img src="./docs/c.png" width="820" alt="액세스 토큰 응답"> <figcaption>로그인 성공 후 액세스 토큰을 응답으로 수신</figcaption> </figure> <figure> <img src="./docs/d.png" width="820" alt="리프레시 토큰 쿠키"> <figcaption>리프레시 토큰은 HttpOnly 쿠키로 저장(SameSite=Lax, 로컬 http는 Secure=false)</figcaption> </figure> </details> <details> <summary>포스트맨 테스트</summary> <figure> <img src="./docs/f.png" width="820" alt="회원가입"> <figcaption>일반 회원가입</figcaption> </figure> <figure> <img src="./docs/g.png" width="820" alt="일반 로그인"> <figcaption>일반 로그인 → AT/RT 발급</figcaption> </figure> <figure> <img src="./docs/h.png" width="820" alt="내 정보 조회"> <figcaption>Bearer AT를 이용한 /api/users/me 응답</figcaption> </figure> </details> <details> <summary>DB 확인</summary> <figure> <img src="./docs/i.png" width="820" alt="DB 사용자 테이블 조회"> <figcaption>DB 사용자 테이블 조회</figcaption> </figure> </details> <details> <summary>Redis 리프레시 토큰 확인</summary> <figure> <img src="./docs/j.png" width="820" alt="로그아웃 시 RT 삭제"> <figcaption>로그아웃 시 Redis에서 Refresh Token 삭제</figcaption> </figure> <figure> <img src="./docs/k.png" width="820" alt="재발급 시 RT 회전"> <figcaption>재발급 시 Redis에 저장된 기존 Refresh Token 회전(갱신)</figcaption> </figure> </details> <details> <summary>스웨거 연동 (API 명세서)</summary> <figure> <img src="./docs/l.png" width="820" alt="Swagger UI"> <figcaption>Swagger UI 화면</figcaption> </figure> </details>

---

## 📖 스터디 방식
- 유튜브 강의
- AI 도구 보조 (Claude AI 활용)
- 기술 블로그 검색

---

## 📝 스터디 하면서 작성한 기술 블로그
[단위테스트 테스트 코드 작성](https://velog.io/@mdy3722/Junit5%EC%99%80-AssertJ%EB%A5%BC-%ED%99%9C%EC%9A%A9%ED%95%98%EC%97%AC-%EB%8B%A8%EC%9C%84%ED%85%8C%EC%8A%A4%ED%8A%B8%EB%A5%BC-%EC%A7%84%ED%96%89)  
[Mockito.Spy() 잘못된 사용으로 인한 문제 발생과 해결](https://velog.io/@mdy3722/%EC%9E%98%EB%AA%BB%EB%90%9C-Spy-%EC%82%AC%EC%9A%A9%EC%9D%B4-%EB%B6%80%EB%A5%B8-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%8B%A4%ED%8C%A8)











