# 설치 및 실행

## 검증한 환경

macOS ARM64(aarch64), RAM 8GB, OpenJDK 21.0.12.1, MariaDB 11.4.13. Spring Boot 3.5.16, MyBatis Starter 3.0.5, Maven 3.9.11 / Wrapper 3.3.4를 사용한다. Docker와 전역 Maven 설치는 필요 없다. 최초 빌드는 Maven Central 네트워크 접근이 필요하다.

## 1. 설치와 복제

```sh
brew install openjdk@21 mariadb@11.4
git clone https://github.com/songsunglyong/publishing-erp-mini.git
cd publishing-erp-mini
```

비공개 저장소 접근 권한이 필요하다. 현재 Phase 0·1 구현은 로컬 커밋이며 아직 원격에 푸시하지 않았다. 다른 컴퓨터에서 복제하려면 해당 커밋이 원격에 반영되어야 한다.

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

테스트는 전용 `_test` 스키마에서만 수행한다. 테스트에서 생성한 `TST_` 도서·거래처를 정리하며 업무 DB를 테스트에 지정하면 안 된다. 테스트 DB의 Flyway 합성 시드는 유지한다. 테스트 결과: `target/surefire-reports/`.

## 4. 앱 실행

```sh
# 합성 도서 3개 / 거래처 3개를 넣은 시연 모드
./scripts/run.sh -Dspring-boot.run.profiles=demo

# 또는 시드 없는 기본 local 프로필
./scripts/run.sh
```

[홈](http://127.0.0.1:8080), [도서](http://127.0.0.1:8080/books), [거래처](http://127.0.0.1:8080/partners), [상태 확인](http://127.0.0.1:8080/health).

실행 스크립트는 Homebrew Java 21 경로를 자동 설정한다. 기존 `JAVA_HOME`이 있으면 해당 값을 사용하므로 Java 21인지 확인한다. 포트 변경은 `PORT=8081 ./scripts/run.sh`로 할 수 있다.

패키징한 WAR를 직접 실행할 수도 있다.

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
set -a
. .local/db.env
set +a
"$JAVA_HOME/bin/java" -Xmx768m -jar target/publishing-erp-mini-0.1.0-SNAPSHOT.war --spring.profiles.active=demo
```

Maven 힙은 384MB, 앱 힙은 768MB, 테스트 JVM은 512MB로 제한한다. Hikari 연결 풀은 앱 최대 5개, 테스트 최대 2개다. 테스트 JVM은 하나만 실행한다. 이 값은 전체 프로세스 RSS 상한이 아니라 Java 힙 상한이다.

## 5. 프로필·마이그레이션

- `local`: 업무 스키마만 자동 적용하며 합성 데이터를 추가하지 않는다.
- `demo`: 업무 스키마와 가상 도서·거래처 시드를 자동 적용한다. 로그인 계정은 아직 없다.
- `test`: 독립된 테스트 DB에 스키마와 합성 시드를 적용한다.

Flyway V1은 도서·거래처·재고 테이블을 만들고, demo/test 전용 V1.1은 합성 데이터를 한 번 추가한다. 이미 데이터가 있는 local DB에도 demo 시드를 추가할 수 있다. 시드는 데이터베이스에 남으며 프로필 변경만으로 지워지지 않는다. demo로 마이그레이션한 DB는 이후에도 demo 프로필로 실행해 마이그레이션 이력을 일관되게 유지한다. 향후 업무 마이그레이션은 V2 이상으로 추가한다. 적용된 SQL은 수정하지 않는다.

환경 변수를 직접 지정하면 `.local/db.env`의 기본값보다 우선한다. 외부 DB를 사용하는 경우 `.env.example`을 참고해 업무/테스트 URL·계정·암호를 각각 export한다. `.env`는 앱이 자동으로 읽지 않는다. 외부 DB에도 utf8mb4 스키마와 별도 계정을 준비해야 한다.

## 6. 시연 절차

1. 도서 목록에서 도서명을 검색하고 사용/비활성 필터를 변경한다.
2. 도서를 등록한다. ISBN은 생략 가능하며 입력 시 유효한 ISBN-13이 필요하다. 정가는 0 이상 정수다.
3. 도서 수정에서 정가·발행일·사용 상태를 변경한다. ISBN 중복은 한국어 오류로 표시된다.
4. 거래처에 고유한 대문자 코드를 입력하고 서점/유통사 유형을 선택해 등록한다.
5. 거래처명·코드·담당자로 검색하고 수정한다. 비활성화 후에도 데이터는 보존되고 다시 사용 상태로 바꿀 수 있다.
6. 목록은 한 페이지 10건이며 최신 등록 순이다. 주문·입출고 UI는 Phase 2 범위다.

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
- 빌드 성공 후 JSP 오류: JAR 대신 WAR를 실행하고 `src/main/webapp/WEB-INF/jsp`가 패키지에 포함됐는지 확인한다.

로그인·권한과 전체 Spring Security 연동은 Phase 3이다. 현재는 폼 CSRF 토큰과 HTML 이스케이프를 적용하고 루프백에만 바인딩한다. 실제 개인정보·운영 데이터는 사용하지 않는다.
