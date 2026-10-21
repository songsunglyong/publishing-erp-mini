# 출판 ERP Mini

Java 21 · Spring Boot · MyBatis XML · JSP/JSTL · jQuery · MariaDB로 만든 한국어 출판 업무 ERP 학습·포트폴리오입니다. 도서·거래처, 다품목 주문·출고·부분 반품, 재고 원장·매출 보고서와 세션 기반 인증을 구현했습니다. 합성 데이터만 사용합니다.

## Mac ARM64 / RAM 8GB 빠른 시작

```sh
brew install openjdk@21 mariadb@11.4
./scripts/db-start.sh
./scripts/auth-init.sh
./scripts/verify.sh
./scripts/run.sh -Dspring-boot.run.profiles=demo
```

[로그인](http://127.0.0.1:8080/login) 정보는 Git 제외·권한 600인 `.local/auth.env`를 로컬 편집기에서 확인하세요. 공유 기본 비밀번호는 없습니다. 비밀번호를 변경한 뒤에는 변경한 값을 사용합니다. `demo`는 가상 도서·거래처 각 3개를 추가하며, 주문 시연은 [대본](doc/DEMO.md)을 따릅니다.

전체 자동 테스트 **91개 통과**, 실행 WAR 빌드 성공. 현재 소스를 비밀/target 없이 복사한 별도 폴더와 빈 테스트 DB에서도 91개를 통과했습니다. 실제 Chromium에서 2품목 입고→주문→출고→부분 반품→보고서를 확인했습니다. 네이티브 앱은 127.0.0.1에만 바인딩합니다.

## 단계별 상태

| 단계 | 상태 |
| --- | --- |
| Phase 0·1 | 실행 기반, 도서·거래처 관리 완료 |
| Phase 2 | 주문·재고·출고·취소·부분 반품, 원자성·중복·동시성 검증 완료 |
| Phase 3 | 매출 지표·보고서, 인증·세션·권한·CSRF 검증 완료 |
| Phase 4 | 문서·실제 화면·시연·유지보수 사례·Docker 구성 준비 완료. **원격 fresh clone·Compose 기동 gate 미완료** |

Docker 실행·배포·서버 호스팅은 진행하지 않았습니다. 저장소는 비공개입니다. GitHub 업로드·원격 CI·배포의 최신 상태는 [릴리스 기록](doc/RELEASE.md)을 참고하세요.

## 주요 문서

- [설치·실행·종료·문제 해결·백업 절차](doc/README.md)
- [실제 검증 결과·변경 파일 목록](doc/TEST_RESULTS.md) · [기능별 학습·PHP 비교](doc/PHASE2_4.md)
- [아키텍처·경로·매출 지표](doc/architecture.md) · [ERD](doc/erd.md)
- [로그인·세션·권한](doc/SECURITY.md) · [사용자 메시지 규칙](doc/USER_MESSAGES.md)
- [실패 테스트→수정→성공 유지보수 사례](doc/maintenance-case.md)
- [Docker 준비와 남은 검증](doc/CONTAINERS.md)
- [프로젝트](doc/PROJECT.md) · [설계 결정](doc/DECISIONS.md) · [할 일](doc/TODO.md) · [변경 기록](doc/CHANGELOG.md)

## 실제 화면

주문 당시 단가를 저장하며 출고 완료 후 품목별 반품을 처리합니다. 보고서는 출고 매출과 반품 차감을 구분합니다.

![출고·부분 반품](doc/screenshots/03-order-return.png)
![매출 보고서](doc/screenshots/05-sales-report.png)
