# GitHub 업로드·배포 준비 기록

## 2026-11-01 — 비공개 제출본 업데이트 준비

사용자 요청으로 최근 Java 패치/SQL Injection 회귀·보안 점검·운영/전체 구조 설명과 쇼핑몰 주문 설계를 원격에 반영한다. 공개 전환 요청은 후속 지시로 보류됐으며 **비공개 유지**가 현재 결정이다. 로컬 Temurin 21.0.13+9에서 전체 93개/WAR 성공(01:10). Gitleaks로 기존 전체 Git 이력(13커밋)을 검사해 패턴 발견 0건이며, 제출 인덱스 검사를 거쳐 커밋·푸시한다. 실제 푸시/CI 결과는 완료 후 아래에 기록한다. 모든 개인정보 부재나 운영 배포 완료를 뜻하지 않는다.

## 2026-10-21 현재 결과

- 저장소: https://github.com/songsunglyong/publishing-erp-mini — 비공개 유지.
- main 업로드 성공: origin d7da63b → 5310b72. 기존 Phase 0·1 커밋도 포함했다.
- 새 구현 커밋: 2cc7eaf, 문서·지원서 초안 커밋: 5310b72. 기존 사용자 JSP 수정도 보존했다.
- 현재 소스 로컬 검증: 91개, 실패/오류/건너뜀 0, WAR 성공(33.431초). .local/release-verify.log.
- 비밀 파일/로그/산출물 제외 및 로컬 생성 암호·주요 토큰/개인키 패턴 비포함 검사 통과. 이는 포괄적 secret 스캔과 구분한다.
- 최초 원격 CI: https://github.com/songsunglyong/publishing-erp-mini/actions/runs/39538500092 — 전체 91개/WAR 성공. 취약점 단계는 성공으로 표시됐으나 Java 분석 대상이 0개여서 유효한 의존성 검사 완료로 인정하지 않는다.
- CI 수정: 1c8a8a2. WAR 전용 디렉터리 rootfs 검사, JSON 전체 패키지 목록과 Security/MyBatis/MariaDB 라이브러리 실제 분석 여부를 필수 검사한다. 재실행 39538865210에서 60개 고유 Java 패키지와 핵심 라이브러리 분석을 확인했으며 HIGH/CRITICAL 취약점 검사에서 실패했다. 취약점 통과로 표시하지 않는다. 246e38f에서 보고서의 CVE/패키지/현재 버전/수정 버전을 CI 로그에 출력하도록 보완해 상세 확인 중이다.
- 실제 GitHub 새 복제: 5310b72를 임시 작업 폴더에 clone, 빈 전용 테스트 DB에 마이그레이션 후 91개/WAR 성공(25.053초). 임시 DB/계정 정리 완료.

## 배포·지원 준비

서버는 아직 없으며 무료를 우선한다. 공식 조건을 확인한 Oracle Always Free ARM 후보와 Render 비교는 HOSTING_PLAN.md에 있다. 계정·결제·서버 생성·배포·공개 전환·지원서 제출은 수행하지 않았다.

Spring/JSP 신입 공통 초안은 APPLICATION_DRAFT.md다. PHP·Android·iOS 유지보수 경험 외 기간·언어·성과는 추정하지 않았다. 채용 공고와 실제 경험 상세를 받아 최종 수정한다. 비공개 저장소 링크는 심사자가 바로 볼 수 없으므로 초대 또는 공개 범위 결정을 따로 해야 한다.

## 보안 패치 후 현재 상태

- 605d6eb: Boot 3.5.16 유지, Tomcat 10.1.55 → 10.1.61, Jackson BOM 2.21.4 → 2.21.7. 패치 후 로컬 91개/WAR 성공(25.877초), WAR 라이브러리 버전 확인. .local/security-patch-verify.log.
- Spring webmvc 6.2.19의 CVE-2026-47884/47890는 남아 있다. 현재 XSLT/SSE 기능은 없으나 라이브러리 보안 패치 완료로 처리하지 않았다. 무료 수정 계열은 Spring 7이며 기존 Boot 3.5 유지/Boot 4 전환 방향을 사용자에게 질문했다.
- 현재 운영/공개 배포를 수행하지 않았다. 전체 취약점 해결 또는 배포 준비 완료로 표현하지 않는다. 공식 근거·패치·제약은 SECURITY_REVIEW.md 참고.

- 보안 패치 WAR를 8081 루프백에서 직접 시작해 실제 로그인과 주문/주문 등록/재고/원장/보고서 JSP 5개 HTTP 200 확인 후 해당 검증 프로세스를 종료했다. 8080 개발 앱과 업무 DB는 유지했다.

## 원격 최종 확인 — 커밋 605d6eb

[GitHub Actions 39539817704](https://github.com/songsunglyong/publishing-erp-mini/actions/runs/39539817704) 완료: 전체 91개/WAR 성공, Java 고유 패키지 60개·필수 라이브러리 분석 범위 검사 성공, 소스 secret 검사 단계 성공.

취약점 단계는 **실패**다. Jackson/Tomcat의 앞선 8개 발견 항목은 재검사에서 사라졌고 다음 Spring 2개만 남았다. 무시·예외 규칙으로 성공 처리하지 않았다.

- CVE-2026-47884, spring-webmvc 6.2.19, OSS 수정 7.0.9.
- CVE-2026-47890, spring-webmvc 6.2.19, OSS 수정 7.0.9.

스캐너 등급과 개발사 공식 등급/노출 조건이 다르므로 SECURITY_REVIEW.md의 근거를 함께 본다. 서버 계정과 Spring 버전 방향에 대한 사용자 답변이 필요하다. 지원서 초안은 준비됐지만 실제 제출·공개 배포는 하지 않았다.

## 최신 결과 — Boot 4 전환 완료 (2026-10-21)

사용자 승인에 따라 Boot 4.1.1·Framework 7.0.9·Security 7.1.1·MyBatis Starter 4.1.0으로 전환했다. JSP/기존 SQL·데이터·Java 21은 유지했다. Tomcat 11.0.27/Jackson 3.1.7의 최종 소스 커밋은 25f7f46이다. 초기 전환은 5667988에 있다.

- 로컬: 92개, 실패/오류/건너뜀 0, WAR 성공(28.917초). .local/boot4-final-tests.log.
- 실제 GitHub 새 복제: 25f7f46, 빈 전용 DB 마이그레이션·92개/WAR 성공(32.085초). 임시 DB/계정 정리 완료. .local/boot4-github-clone-verify.log.
- [최종 원격 CI 39544325071](https://github.com/songsunglyong/publishing-erp-mini/actions/runs/39544325071): **전체 성공**. 92개/WAR, 고유 Java 패키지 71개·필수 라이브러리 분석 범위, 설정된 HIGH/CRITICAL 취약점 gate 및 source secret 검사 단계 성공.
- 이전 Spring CVE 2개와 Jackson 항목은 최종 검사에서 보고되지 않았다. 무시 목록·예외 규칙을 추가하지 않았다. 모든 가능한 보안 결함이 없음을 보장하는 결과는 아니다.
- 실제 WAR 로그인/JSP 5개와 Chromium 품목 추가/삭제·한국어 404·업무 화면, JS 오류 0개를 확인했다. 새 캡처 06~08을 제공한다. 8080 개발 앱도 최종 패치로 재시작했다.
- 초기 CI 실패/패치 기록은 위에 이력으로 보존한다. 더 이상 Boot 3.5 유지/Boot 4 선택 답변을 기다리는 상태가 아니다.
- 서버 가입은 사용자가 더 생각하기로 했다. Docker 기동·서버 생성·결제·외부 호스팅은 수행하지 않았다.

상세 파일/학습 설명: BOOT4_MIGRATION.md. CI의 actions/checkout·setup-java v4는 Node/지원 중단 경고를 출력했으나 이번 실행은 Node 24로 성공했다. 향후 SHA 고정 버전 갱신을 별도 점검한다.
