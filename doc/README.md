# 설치 및 실행

## 검증한 환경

macOS ARM64(aarch64), RAM 8GB, OpenJDK 21.0.12.1, MariaDB 11.4.13. Spring Boot 3.5.16, Spring Security 6.5.11, MyBatis Starter 3.0.5, Maven 3.9.11 / Wrapper 3.3.4를 사용한다. Docker와 전역 Maven 설치는 필요 없다. 최초 빌드는 Maven Central 네트워크 접근이 필요하다.

## 1. 설치와 복제

```sh
brew install openjdk@21 mariadb@11.4
git clone https://github.com/songsunglyong/publishing-erp-mini.git
cd publishing-erp-mini
```

비공개 저장소 접근 권한이 필요하다. 현재 추가 구현은 작업 트리에 있으며 원격에 푸시하지 않았다. 아래 복제 명령으로 최신 구현을 받으려면 해당 변경이 먼저 원격에 반영되어야 한다. 이번에는 비밀/target을 제외한 새 소스 사본과 빈 DB로 검증했다.

## 2. 프로젝트 전용 MariaDB 시작

```sh
./scripts/db-start.sh
```

Homebrew 서비스 등록 없이 `.local/data`를 사용하는 네이티브 DB를 `127.0.0.1:3307`에 시작한다. 기본 인스턴스(`/opt/homebrew/var/mysql`)를 사용하지 않는다. 스크립트는 재실행할 수 있으며 기존 데이터를 초기화하지 않는다.

- 업무 DB: `publishing_erp` / `erp_app`.
- 테스트 DB: `publishing_erp_test` / `erp_test`.
- 두 계정은 각각 해당 스키마의 권한만 갖는다. 현재 단계에서는 Flyway 마이그레이션에 필요한 DDL 권한도 포함한다.
- 임의 생성한 암호는 권한 600인 `.local/db.env`에 저장된다. `.local/` 전체는 Git에서 제외된다.
- InnoDB 버퍼 128MB, 최대 연결 15개, performance schema 비활성화로 로컬 개발 메모리를 줄인다.

다른 MariaDB 설치 경로라면 `ERP_DB_BIN=/설치경로/bin ./scripts/db-start.sh`로 실행한다. 자동 탐색 기본값은 Apple Silicon Homebrew의 `/opt/homebrew/opt/mariadb@11.4/bin`이다.

## 3. 테스트와 패키징

```sh
./scripts/verify.sh
```

이 명령은 Java 경로와 로컬 DB 환경 변수를 불러온 뒤 `./mvnw clean verify`를 실행한다. JUnit 5 테스트, 실제 MariaDB/MyBatis·트랜잭션·제약조건, 실제 HTTP/JSP 통합 검증을 포함하며 `target/publishing-erp-mini-0.1.0-SNAPSHOT.war`를 만든다. H2 대체나 Docker를 사용하지 않는다. DB 연결이 없으면 테스트는 실패한다.

직접 Maven Wrapper를 사용하려면:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
set -a
. .local/db.env
set +a
./mvnw clean verify
```

테스트는 전용 `_test` 스키마에서만 수행한다. 테스트에서 생성한 `TST_`/`TXN_` 도서·거래처와 해당 거래 이력을 정리하며 업무 DB를 테스트에 지정하면 안 된다. 테스트 DB의 Flyway 합성 시드는 유지한다. 테스트 결과: `target/surefire-reports/`.

## 4. 앱 실행

처음 한 번 `./scripts/auth-init.sh`를 실행해 `.local/auth.env`를 만든다. 파일을 로컬 편집기에서 열어 초기 관리자 아이디/비밀번호를 확인한다. 권한은 600이며 Git에서 제외된다. 기존 파일과 DB 계정은 덮어쓰지 않는다. 실행 스크립트가 이 파일의 bootstrap 환경을 읽어 계정이 전혀 없는 DB에만 관리자를 생성한다.

로그인 후 비밀번호를 바꾸면 최초 파일 값은 더 이상 로그인 비밀번호가 아니다. bootstrap 환경은 최초 설정 후 비활성화하거나 제거한다. 자세한 정책은 [SECURITY.md](SECURITY.md)를 참고한다.

```sh
# 합성 도서 3개 / 거래처 3개를 넣은 시연 모드
./scripts/run.sh -Dspring-boot.run.profiles=demo

# 또는 시드 없는 기본 local 프로필
./scripts/run.sh
```

[홈](http://127.0.0.1:8080), [도서](http://127.0.0.1:8080/books), [거래처](http://127.0.0.1:8080/partners), [주문](http://127.0.0.1:8080/orders), [재고](http://127.0.0.1:8080/inventory), [보고서](http://127.0.0.1:8080/reports/sales), [상태 확인](http://127.0.0.1:8080/health).

실행 스크립트는 Homebrew Java 21 경로를 자동 설정한다. 기존 `JAVA_HOME`이 있으면 해당 값을 사용하므로 Java 21인지 확인한다. 포트 변경은 `PORT=8081 ./scripts/run.sh`로 할 수 있다.

패키징한 WAR를 직접 실행할 수도 있다.

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
set -a
. .local/db.env
. .local/auth.env
set +a
"$JAVA_HOME/bin/java" -Xmx768m -jar target/publishing-erp-mini-0.1.0-SNAPSHOT.war --spring.profiles.active=demo
```

### JSP를 수정해도 화면이 그대로일 때

개발 중에는 `./scripts/run.sh -Dspring-boot.run.profiles=demo`로 실행한다. 이 방식은 소스 폴더의 JSP를 사용한다. local/demo 프로필은 JSP `development=true`, `modificationTestInterval=0`, `recompileOnFail=true`와 Tomcat `resource.allow-caching=false`를 명시하여 저장 후 다음 새로고침에서 변경을 검사한다. 프로필 설정을 바꾼 직후에는 앱을 한 번 재시작해야 한다. 실행 명령만 개발용으로 바꾸는 것으로는 이 설정이 보장되지 않는다.

저장 파일에 실제 변경이 있는지도 확인한다. 제목 앞에 문구를 넣을 때는 `<strong>제목: <c:out value="${row.title}"/></strong>`처럼 `<c:out`의 시작 `<`를 보존해야 한다. `<`가 없으면 태그가 실행되지 않고 글자로 표시될 수 있다.

설정 근거: [Tomcat Jasper 문서](https://tomcat.apache.org/tomcat-10.1-doc/jasper-howto.html). 실제 사용 중인 Spring Boot 3.5.16 Jsp 클래스의 기본 `development=false`와 리소스 캐시 기본 TTL 5초도 로컬 라이브러리에서 확인했다.

`java -jar ...war`로 실행 중이면 WAR 안에 묶인 이전 JSP를 사용한다. 이 경우 소스 저장만으로 반영되지 않는다. 실행한 앱을 종료하고 개발 실행으로 전환하거나, WAR를 다시 빌드한 뒤 새 WAR로 재시작한다. 같은 8080 포트에 앱 두 개를 동시에 실행할 수 없다. PHP의 원본 템플릿과 별도 배포 사본을 구분하는 상황에 비유할 수 있다.

Maven 힙은 384MB, 앱 힙은 768MB, 테스트 JVM은 512MB로 제한한다. Hikari 연결 풀은 앱 최대 5개, 테스트 최대 2개다. 테스트 JVM은 하나만 실행한다. 이 값은 전체 프로세스 RSS 상한이 아니라 Java 힙 상한이다.

## 5. 프로필·마이그레이션

- `local`: 업무 스키마만 자동 적용하며 합성 데이터를 추가하지 않는다.
- `demo`: 업무 스키마와 가상 도서·거래처 시드를 적용한다. 관리자는 별도 bootstrap으로 생성하며 공유 기본 비밀번호는 없다.
- `test`: 독립된 테스트 DB에 스키마와 합성 시드를 적용한다. 테스트 설정에서만 합성 로그인 계정을 만든다.
- `prod`: Secure 쿠키와 HTTPS 전환을 강제한다. local/demo/container와 동시에 활성화하지 않는다. 실제 인증서·프록시 배포는 아직 하지 않았다.
- `container`: Compose 내부 0.0.0.0 바인딩, 호스트 루프백 공개 HTTP 시연 전용. 구성만 작성했으며 실제 기동은 미검증이다. CONTAINERS.md 참고.

Flyway V1은 도서·거래처·재고 테이블을 만들고, demo/test 전용 V1.1은 합성 데이터를 한 번 추가한다. 이미 데이터가 있는 local DB에도 demo 시드를 추가할 수 있다. 시드는 데이터베이스에 남으며 프로필 변경만으로 지워지지 않는다. demo로 마이그레이션한 DB는 이후에도 demo 프로필로 실행해 마이그레이션 이력을 일관되게 유지한다. V2는 erp_users와 login_attempts를 추가한다. V3는 주문·품목·재고 원장·반품을 추가하고 기존 비영 재고를 OPENING 원장으로 이관한다. 이후 변경은 V4 이상으로 추가한다. 적용된 SQL은 수정하지 않는다.

환경 변수를 직접 지정하면 `.local/db.env`의 기본값보다 우선한다. 외부 DB를 사용하는 경우 `.env.example`을 참고해 업무/테스트 URL·계정·암호를 각각 export한다. `.env`는 앱이 자동으로 읽지 않는다. 외부 DB에도 utf8mb4 스키마와 별도 계정을 준비해야 한다.

## 6. 시연 절차

먼저 `/login`에서 관리자 또는 MANAGER로 로그인한다. USER는 조회만 가능하다. 홈과 공통 메뉴에 로그아웃/내 정보/비밀번호 변경을 제공하고 ADMIN에게 계정 관리를 표시한다.

1. 도서 목록에서 도서명을 검색하고 사용/비활성 필터를 변경한다.
2. 도서를 등록한다. ISBN은 생략 가능하며 입력 시 유효한 ISBN-13이 필요하다. 정가는 0 이상 정수다.
3. 도서 수정에서 정가·발행일·사용 상태를 변경한다. ISBN 중복은 한국어 오류로 표시된다.
4. 거래처에 고유한 대문자 코드를 입력하고 서점/유통사 유형을 선택해 등록한다.
5. 거래처명·코드·담당자로 검색하고 수정한다. 비활성화 후에도 데이터는 보존되고 다시 사용 상태로 바꿀 수 있다.
6. 재고 메뉴에서 등록한 도서를 입고한다. 수량·사유는 필수이며 ADMIN만 재고 조정을 할 수 있다.
7. 주문 메뉴에서 거래처와 1~20개 도서를 선택한다. 단가를 비우면 정가가 적용되며 서버가 총액을 계산한다.
8. 주문 확정 후 출고한다. 확정 시 예약하지 않고 출고 때 차감한다. 한 품목이라도 부족하면 전체 출고를 되돌린다. 작성·확정 상태만 취소할 수 있다.
9. 출고 완료 상세에서 품목별 부분 반품 수량·사유를 입력한다. 출고-기반 누적 한도를 검사하고 재고를 복원한다.
10. 원장과 보고서를 확인한다. 출고일 매출/반품일 차감을 구분한다. 전체 시연 대본은 DEMO.md 참고. 목록/원장은 10건씩, 보고서 순위는 상위 20개다.

## 7. 종료와 로그

포그라운드 앱은 실행한 터미널에서 `Ctrl+C`로 종료한 뒤 DB를 종료한다.

```sh
./scripts/db-stop.sh
```

DB 로그는 `.local/mariadb.log`, Maven 테스트 결과는 `target/surefire-reports/`다. 데이터는 종료 후에도 `.local/data`에 남는다. 별도 요청 없이 이 디렉터리를 삭제하거나 DB를 초기화하지 않는다. 로컬 검증에서 실행한 앱 프로세스를 종료할 때는 `lsof -nP -iTCP:8080 -sTCP:LISTEN`으로 PID와 앱을 확인한 뒤 그 PID에 `kill -TERM`을 보낸다.

## 문제 해결

- Java Runtime 없음: 위 `JAVA_HOME`을 설정한다. 시스템 JDK 심볼릭 링크나 관리자 권한은 필요 없다.
- 3307 포트 충돌: 기존 프로세스를 확인한다. 전용 스크립트는 3307 고정이며 임의의 다른 DB를 중지하지 않는다.
- 암호 누락/Access denied: `db-start.sh` 후 `scripts/verify.sh` 또는 `scripts/run.sh`를 사용한다. `.local/db.env`는 데이터 디렉터리와 함께 보관한다.
- Maven 다운로드 지연: 프로젝트 `.mvn/settings.xml`은 Central 전용 미러를 사용한다. 사내 미러가 필요하면 해당 설정을 환경에 맞게 조정한다.
- Flyway checksum 오류: 적용된 SQL을 임의로 변경하지 않는다. 새 마이그레이션을 추가하고 원인을 확인한다.
- 메시지 파일 변경 미반영: 원본 저장 후 개발 앱을 종료하고 `./scripts/run.sh -Dspring-boot.run.profiles=demo`로 재실행한다. Maven의 target/classes 리소스 복사와 Spring 메시지 재로딩이 필요하다. 자세한 절차는 USER_MESSAGES.md 참고.
- 빌드 성공 후 JSP 오류: JAR 대신 WAR를 실행하고 `src/main/webapp/WEB-INF/jsp`가 패키지에 포함됐는지 확인한다.

로그인·권한·CSRF는 Spring Security로 통합했다. 기본 미사용 30분·절대 8시간·계정당 한 세션을 적용하고 루프백에만 바인딩한다. 실제 개인정보·운영 데이터는 사용하지 않는다.

## 오류 안내와 문의

공통 오류 코드·한국어 문구·입력 보존·보안 기준은 [USER_MESSAGES.md](USER_MESSAGES.md)를 참고한다. 화면의 오류 코드와 문의용 추적 ID로 서버의 user_error 로그를 찾는다. 비밀번호·토큰·입력 내용은 로그나 문의에 첨부하지 않는다. 저장 장애 후에는 목록에서 반영 여부를 먼저 확인한다. 로그인/권한 오류도 같은 안내 체계를 사용한다. 실제 운영 연락 수단은 아직 준비되지 않았다.

## GitHub Actions

`.github/workflows/security.yml`에 전체 테스트·Trivy 취약점/비밀정보 검사를 연결했다. 워크플로는 actionlint 검사를 통과했지만 원격 푸시/실행 및 Trivy 결과는 아직 미검증이다. CI의 MariaDB 서비스 컨테이너는 합성 테스트용이며 로컬 Docker나 서버 배포를 수행한 것은 아니다.

## 포트폴리오·Docker 준비

[시연 대본·실제 화면](DEMO.md), [학습/단계별 결과](PHASE2_4.md), [구조·경로·매출 정의](architecture.md), [ERD](erd.md), [유지보수 재현](maintenance-case.md), [전체 검증](TEST_RESULTS.md)을 제공한다. Dockerfile/Compose는 준비했지만 실행·배포하지 않았다. 실제 기동 절차와 미검증 항목은 [CONTAINERS.md](CONTAINERS.md)에 있다.

## 로컬 백업과 복원 계획

프로젝트 DB가 실행 중일 때 로컬 OS 계정의 소켓 인증으로 백업할 수 있다. 덤프에는 해시된 인증정보와 합성 업무 데이터가 포함되므로 Git/채팅에 올리지 않는다. 다음은 후속 절차이며 실제 복원 훈련은 아직 수행하지 않았다.

```sh
# 현재 폴더가 프로젝트 루트인지 확인 후 실행
umask 077
/opt/homebrew/opt/mariadb@11.4/bin/mariadb-dump --no-defaults \
  --socket="$PWD/.local/mariadb.sock" --single-transaction --routines --triggers \
  publishing_erp > .local/publishing-erp-backup.sql
chmod 600 .local/publishing-erp-backup.sql
```

복원은 별도 빈 스키마와 전용 계정을 생성해 해당 스키마에 덤프를 입력하고 DB_URL을 바꿔 검증한다. 기존 업무 스키마를 지우고 덮어쓰지 않는다. DB 데이터와 `.local/db.env`/변경 후 관리자 암호는 함께 안전하게 보관한다.

## 현재 한계

단일 JVM/단일 회사·합성 데이터 시연이다. 세션은 앱 재시작 시 사라지며 서버 여러 대의 동시 로그인 제한은 지원하지 않는다. 결제·회계·운송장·재고 예약/배정·주문 수정·통화/세금은 범위 밖이다. 보고서 정렬은 순매출 금액 기준이며 현재 재고와 기간 매출을 구분한다. Flyway는 MariaDB 11.4에 검증 범위 경고를 출력하지만 실제 마이그레이션·통합 테스트는 통과했다. 운영 적용 전에 도구 지원 범위·취약점·백업 복원을 재검토한다.
