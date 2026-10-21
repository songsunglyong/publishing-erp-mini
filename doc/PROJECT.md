# publishing-erp-mini

## 목적과 범위

한국어 출판 업무 ERP 포트폴리오·Spring Boot 학습 프로젝트다. PHP 개발 경험을 Java/Spring MVC·트랜잭션·SQL·보안 설명 능력으로 연결한다. 합성 데이터만 사용한다. PRD v1.0과 최신 추가 보안·오류 요구사항을 따른다.

## 현재 상태 (2026-10-20)

- Phase 0·1 완료: ARM64 Java 21, 실행 WAR/JSP, 네이티브 MariaDB, 도서·거래처 CRUD·검색·상태·페이지네이션.
- Phase 2 완료: 다품목 주문, 주문 당시 단가/명칭, 확정·출고·취소, 입고·ADMIN 조정·원장, 전체/부분 반품, 중복 방지, 잠금·원자적 롤백.
- Phase 3 완료: 월별/도서별/거래처별 매출과 현재 부족 재고. 로그인·ADMIN/MANAGER/USER·CSRF·세션·직원 객체 권한·공통 오류. 실제 금액 대사·접근 차단 검증.
- Phase 4 산출물 작성·네이티브 검증 완료: README/ERD/구조/시연 대본/실제 화면 5개/유지보수 실패→성공 사례/Dockerfile·Compose. **원격 fresh clone과 Compose 기동 gate는 미완료**. Docker 실행과 서버 호스팅은 기존 사용자 지시에 따라 하지 않았다.
- 전체 자동 테스트 91개, 실행 WAR 빌드, 새 소스 사본+빈 DB 마이그레이션·전체 검증 성공. 세부 결과는 TEST_RESULTS.md.
- 버전: Java 21.0.12.1 / Boot 3.5.16 / Security 6.5.11 / MyBatis Starter 3.0.5 / MariaDB 11.4.13 / Maven Wrapper 3.9.11.
- Mac ARM64 RAM 8GB: 네이티브 DB 버퍼 128MB, 앱 힙 768MB, Maven 384MB, 테스트 512MB·단일 fork, 풀 앱 5/테스트 2개.
- GitHub: https://github.com/songsunglyong/publishing-erp-mini (비공개). 원격 연결은 기존에 완료했으나 현재 추가 구현은 작업 트리이며 푸시하지 않았다. 현재 원격 복제본에 최신 코드가 있다고 보장하지 않는다.

## 실제 구조

```text
publishing-erp-mini/
├── pom.xml, mvnw, .mvn/          # Java 21, Maven, 실행 WAR
├── Dockerfile, compose.yaml     # 로컬 보조 구성, 실행 미검증
├── scripts/                     # 네이티브 DB·앱·초기 계정·검증
├── src/main/java/com/portfolio/erp/
│   ├── book/, partner/          # 공동 기준정보
│   ├── order/                   # 주문·상태·출고·반품
│   ├── inventory/               # 입고·조정·재고 원장
│   ├── report/                  # 기간 매출·현재 부족 재고
│   ├── security/                # 인증·권한·세션·계정 관리
│   └── common/                  # 검증·페이지·공통 오류·추적 ID
├── src/main/resources/
│   ├── application*.yml, messages*.properties
│   ├── mapper/                  # #{} SQL 바인딩
│   ├── db/migration/            # V1 기준정보, V2 인증, V3 거래
│   ├── db/demo/                 # V1.1 합성 시드
│   └── static/                  # CSS·jQuery 코드
├── src/main/webapp/WEB-INF/jsp/  # 기준정보·주문·재고·보고서·보안
├── src/test/java/com/portfolio/erp/
└── doc/                         # 문서·검증 증거·실제 화면
```

`.local/`(암호·DB·로그)와 target은 Git에서 제외한다. jQuery/Bootstrap은 WebJars로 제공해 런타임 CDN 요청이 없다. 소스만으로 로그인할 수 있는 공개 기본 비밀번호는 없으며 초기 계정은 명시적 bootstrap으로 생성한다.

## 다음 작업

최신 요청 범위의 기능 구현은 완료했다. 이후에는 PHASE2_4.md를 따라 작은 단위로 학습하고 이해를 확인한다. 배포/호스팅·공개 전환·원격 푸시는 별도 요청 대상이다. Phase 4 미검증 항목과 운영 한계는 TODO.md 및 CONTAINERS.md에 남겼다.
