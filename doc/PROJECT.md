# publishing-erp-mini

## 목적과 범위

사용자 PRD v1.0의 한국어 출판 업무 ERP 포트폴리오·시연 프로젝트다. 합성 데이터만 사용한다. 이번 구현 범위는 Phase 0과 Phase 1이다. 주문·출고·반품·재고 원장·보고서·로그인·권한은 후속 단계이며 Docker·서버 호스팅은 진행하지 않았다.

## 현재 상태 (2026-10-17)

- Phase 0 완료: Java 21 ARM64, Maven Wrapper, Spring Boot 실행 WAR, JSP/JSTL, 실제 MariaDB 연결, HTTP 상태 확인.
- Phase 1 완료: 도서·거래처 등록/조회/수정/비활성화, 검색·상태 필터·10건 페이지네이션, 한국어 검증, ISBN 체크섬·고유 키 처리.
- Flyway 업무 스키마와 demo/test 전용 합성 시드, 신규 도서 재고 0행 자동 생성(동일 트랜잭션).
- 실제 MariaDB·HTTP 테스트 29개 통과. H2나 테스트 컨테이너를 사용하지 않는다.
- 지정 기술 스택을 유지한다. Java 21.0.12.1 / Spring Boot 3.5.16 / MyBatis Starter 3.0.5 / MariaDB 11.4.13.
- Mac ARM64 RAM 8GB에서 네이티브 로컬 실행을 검증했다.
- GitHub: https://github.com/songsunglyong/publishing-erp-mini (비공개). 이번 구현은 로컬 커밋이며 아직 푸시하지 않았다.

## 실제 구조

```text
publishing-erp-mini/
├── AGENTS.md, README.md, PRD_Publishing_ERP_Mini.md
├── pom.xml, mvnw, mvnw.cmd, .mvn/  # Java 21 / Maven 3.9.11
├── scripts/                      # DB 시작·종료, 앱 실행, 검증
├── src/main/java/com/portfolio/erp/
│   ├── ErpApplication.java
│   ├── book/                     # Controller, Service, Mapper, Form, entity
│   ├── partner/                  # Controller, Service, Mapper, Form, entity
│   └── common/                   # 페이지, 검증, CSRF, 오류, 홈/상태 확인
├── src/main/resources/
│   ├── application*.yml
│   ├── mapper/                   # MyBatis XML
│   ├── db/migration/             # V1 업무 스키마
│   ├── db/demo/                  # V1.1 가상 데이터
│   └── static/                   # CSS, jQuery 사용 코드
├── src/main/webapp/WEB-INF/jsp/   # book, partner, common, home
├── src/test/java/com/portfolio/erp/
└── doc/                          # 프로젝트·실행·설계·검증 문서
```

`.local/`(DB 데이터·생성 암호·실행 로그)와 `target/`(빌드·테스트 산출물)은 Git에서 제외한다. jQuery와 Bootstrap은 Maven WebJars로 제공되어 런타임 CDN 요청이 없다.

## 다음 단계

현재 Phase 1에서 멈춘다. 다음 구현은 Phase 2 주문·출고·반품·재고 원장이며, 사용자 지시에 따라 시작한다. 상세 검증과 남은 작업은 `TEST_RESULTS.md`, `TODO.md`를 참고한다.
