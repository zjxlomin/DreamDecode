# DreamDecode

꿈을 기록하고 AI로 심리 분석해주는 커뮤니티 기반 플랫폼

> "Discover your hidden emotions through your dreams."

## 프로젝트 개요

DreamDecode는 사용자가 자신의 꿈을 기록하고, AI 기술을 활용하여 심리적으로 해석해주는 웹 애플리케이션입니다. 꿈을 통해 자신의 감정과 무의식을 탐구하고, 다른 사용자들과 꿈을 공유하며 서로의 경험을 나눌 수 있는 커뮤니티 플랫폼을 제공합니다.

## 주요 기능

### 꿈 관리
- **꿈 등록 및 수정**: 제목, 내용, 공개 여부 설정
- **내 꿈 조회**: 개인 대시보드에서 내가 작성한 꿈 목록 확인
- **꿈 삭제**: 본인이 작성한 꿈만 삭제 가능

### AI 자동 분석
- **Alan AI 심리 분석**: 꿈 내용을 기반으로 주요 장면 분석, 심리적 해석, 조언 제공
- **Google NLP 감정 분석**: 감정 점수(-1.0 ~ 1.0) 및 감정 강도 측정
- **자동 카테고리 분류**: 꿈의 주제를 카테고리로 자동 분류
- **태그 생성**: 꿈의 주요 키워드를 태그로 자동 생성

### 꿈 탐색
- **공개 꿈 목록**: 모든 사용자가 공개된 꿈을 조회 가능
- **검색 기능**: 제목 키워드로 검색
- **필터링**: 카테고리별, 태그별 필터링
- **페이지네이션**: 대량 데이터 효율적 처리

### 사용자 인증
- **회원가입**: 이메일 기반 회원가입
- **이메일 인증**: 회원가입 시 이메일 인증 필수
- **JWT 인증**: Access Token(2시간) + Refresh Token(14일)
- **보안**: HttpOnly 쿠키 사용, XSS 공격 방지

## 기술 스택

### Backend
- Spring Boot 3.5.7
- Java 17
- PostgreSQL
- Spring Security + JWT
- Google Cloud Natural Language API
- Alan AI API

### Frontend
- HTML5, JavaScript (ES6+)
- jQuery 3.7.1
- Bootstrap 5.3.3
- Thymeleaf

## AI 분석 프로세스

1. **꿈 등록/수정**: 사용자가 꿈을 등록하거나 수정하면 자동으로 AI 분석이 트리거됩니다.

2. **Alan AI 분석**: 
   - 꿈 내용을 프롬프트로 전달
   - 주요 장면 2~4개 추출 및 감정 분석
   - 심리적 해석 및 조언 생성
   - 카테고리 및 태그 자동 생성

3. **Google NLP 감정 분석**:
   - Alan AI가 생성한 요약(summary)을 Google NLP로 전달
   - 감정 점수(Score) 및 감정 강도(Magnitude) 계산

4. **결과 저장**: 분석 결과를 데이터베이스에 저장하여 상세 조회 시 표시

## API 문서

Swagger UI를 통해 API 문서를 확인할 수 있습니다.

- **로컬**: http://localhost:8080/swagger-ui.html

## 프로젝트 구조

```
DreamDecode/
├── src/main/java/est/DreamDecode/
│   ├── config/          # 설정 클래스 (Security, JWT, OpenAPI)
│   ├── controller/      # REST API 컨트롤러
│   ├── domain/          # JPA 엔티티 (Dream, User, Analysis, Scene)
│   ├── dto/             # 데이터 전송 객체 (Request, Response)
│   ├── exception/       # 커스텀 예외 처리
│   ├── repository/      # JPA Repository 인터페이스
│   ├── service/         # 비즈니스 로직 (Dream, Analysis, Auth 등)
│   └── util/            # 유틸리티 클래스
└── src/main/resources/
    ├── templates/       # Thymeleaf 템플릿 (HTML)
    └── static/          # 정적 리소스 (CSS, JS, Images)
```

## 주요 특징

- **RESTful API**: 표준 REST API 설계 원칙 준수
- **JWT 기반 인증**: 안전한 사용자 인증 및 권한 관리
- **AI 통합**: Alan AI와 Google NLP를 활용한 자동 분석
- **페이지네이션**: 대량 데이터 효율적 처리
- **Swagger 문서화**: API 자동 문서화
- **보안**: Spring Security를 통한 보안 강화

## 라이선스

이 프로젝트는 교육 목적으로 개발되었습니다.
