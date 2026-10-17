# publishing-erp-mini

## 프로젝트 목적

`PRD_Publishing_ERP_Mini.md` v1.0에 정의된 한국어 출판사 내부 업무용 ERP 포트폴리오·시연 프로젝트다. 도서, 거래처, 주문, 출고, 재고 원장과 매출 요약을 다루며 합성 데이터만 사용한다. 운영용 ERP, 회계·세금계산서·급여·결제 연동은 범위에서 제외한다.

## 현재 상태

- 2026-10-17 최초 확인 시 작업 폴더는 비어 있었으며 기존 문서·소스 코드·Git 이력이 없었다.
- 프로젝트 운영 문서와 Git 기본 설정을 완료했다.
- GitHub: https://github.com/songsunglyong/publishing-erp-mini (비공개), `origin`, 기본 브랜치 `main`.
- 문서 작성 중 루트에 PRD v1.0이 추가되었고 이를 읽어 계획에 반영했다.
- 애플리케이션, 데이터베이스, 테스트, 빌드·배포 설정은 아직 없다.
- PRD 지정 스택: Java 21, Maven Wrapper, Spring Boot 3.5.x, Spring MVC, JSP/JSTL, jQuery, Bootstrap 5, MariaDB, MyBatis XML. 정확한 의존성 버전과 호환성은 구현 전에 검증한다.
- 기본 개발 환경은 Docker 없는 로컬 실행이며 Docker Compose는 후속 단계다.
- 다음 구현 단계는 Phase 0(환경·JSP·DB 연결 검증)이며 아직 시작하지 않았다.

## 현재 구조

```text
publishing-erp-mini/
├── .gitignore          # 로컬 환경 파일 및 비밀정보 제외
├── AGENTS.md           # 저장소 전체 작업 규칙 진입점
├── PRD_Publishing_ERP_Mini.md # 사용자 제공 요구사항 명세(현재 로컬 파일)
└── doc/
    ├── PROJECT.md      # 프로젝트 목적과 현재 구조
    ├── AGENTS.md       # Codex 작업 규칙 원본
    ├── TODO.md         # 앞으로 할 작업과 완료 상태
    ├── CHANGELOG.md    # 완료된 변경과 검증 기록
    ├── DECISIONS.md    # 중요한 설계 결정과 이유
    └── README.md       # 설치·실행·검증 방법
```

## 다음 단계

`TODO.md`와 PRD 단계별 통과 조건에 따라 다음 미완료 단계부터 구현한다. PRD의 `docs/` 문서 경로 예시는 이번 요청에 맞추어 `doc/`로 통일한다. 실제 구현에 맞추어 이 문서의 구조와 상태를 갱신한다.
