# 출판 ERP Mini

Java 21 · Spring Boot · MyBatis XML · JSP/JSTL · jQuery · MariaDB로 만든 한국어 출판 업무 시연 프로젝트입니다. Phase 0과 Phase 1(도서·거래처 관리)을 구현했습니다. 합성 데이터만 사용합니다.

## Mac ARM64 빠른 시작

```sh
brew install openjdk@21 mariadb@11.4
./scripts/db-start.sh
./scripts/verify.sh
./scripts/run.sh -Dspring-boot.run.profiles=demo
```

[로컬 화면](http://127.0.0.1:8080)에서 도서·거래처를 등록·수정·검색·비활성화할 수 있습니다. `demo`는 도서와 거래처 각 3개의 가상 데이터를 추가합니다. 빈 업무 DB로 시작하려면 `./scripts/run.sh`를 사용하세요.

실제 MariaDB·HTTP 테스트 **29개 통과**, 실행 WAR의 JSP 화면 확인 완료. 테스트 DB는 업무 DB와 분리됩니다. 로그인·권한, 주문·출고·반품·보고서는 후속 단계입니다. 앱은 `127.0.0.1`에만 바인딩됩니다.

- [설치·실행·종료·문제 해결](doc/README.md)
- [프로젝트 상태](doc/PROJECT.md) · [설계 결정](doc/DECISIONS.md)
- [검증 결과와 단계별 변경 파일](doc/TEST_RESULTS.md)
- [구조](doc/architecture.md) · [현재 ERD](doc/erd.md)
- [할 일](doc/TODO.md) · [변경 기록](doc/CHANGELOG.md)

Docker 배포·서버 호스팅은 구성하지 않았습니다.
