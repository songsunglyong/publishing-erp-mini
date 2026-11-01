# Java 21 보안 패치 적용 — 2026-10-31

## 적용 범위

사용자가 로컬 Java 패치를 승인했다. Eclipse Temurin **21.0.13+9 LTS / macOS aarch64**를 프로젝트 전용으로 설치했다. 기존 Homebrew 21.0.12.1은 삭제하거나 인증서를 직접 수정하지 않았다. macOS·전역 셸·다른 프로젝트·IDE·Docker·서버의 Java를 일괄 전환한 작업은 아니다. 업무 코드·Spring/MyBatis 버전·DB 권한/스키마는 변경하지 않았다.

설치 위치:

```text
.local/jdks/temurin-21.0.13+9/Contents/Home
```

`.local/`는 Git 제외다. 다운로드 약 200MB(199,754,598 bytes), 설치 파일은 이 Mac에서 `du -sh` 기준 약 335MB다. 설치 후 압축파일은 제거했다. 기존 Java와 새 Java가 함께 존재한다.

## 배포 출처와 무결성

- [공급자 보안 안내](https://adoptium.net/de/news/2026/10/temurin-october-2026-emudhra): 21.0.13+9를 대응 빌드로 안내한다. Temurin은 Mozilla CA 정책을 사용하며 G1이 남는 것과 관련 G3/AAA의 제거를 구분한다.
- [공식 릴리스](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.13%2B9)의 `OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.13_9.tar.gz`를 사용했다. 최초 Adoptium assets 조회 경로의 404 이후 같은 공급자의 공식 GitHub 배포로 전환했다.
- 공식 sha256.txt와 실제 압축파일 SHA256 일치: `bf036b35b071bfb2f54f2c0af8ee8d88afa95ee6eb3dce02c0f94cfc2524d04a`.
- 설치 메타데이터는 `.local/jdks/temurin-21.0.13+9.json`에 보관한다. 다운로드한 실행 파일이나 비밀 파일을 Git에 추가하지 않는다.

다른 Mac에서 재현하려면 공식 릴리스의 **JDK / macOS / aarch64** 압축파일과 해당 체크섬을 내려받아 검증하고, 압축을 풀어 JDK bundle을 `.local/jdks/temurin-21.0.13+9`에 둔다. `Contents/Home/bin/java`가 실제로 존재하는지 확인한다. 이 폴더는 복제 시 자동 설치되지 않는다. 다른 OS/아키텍처는 대응 패키지를 받아 명시적 JAVA_HOME으로 지정하고 따로 검증한다.

## 파일 역할과 선택 흐름

- `scripts/env.sh`: 호출자가 명시한 JAVA_HOME → 설치된 프로젝트 Temurin → 기존 Homebrew fallback 순서다. 선택한 JDK의 bin을 현재 스크립트 PATH 앞에 둬 Maven과 Java 하위 프로세스가 일치하도록 했다.
- `scripts/java-info.sh`: 같은 선택 규칙으로 java -version만 보여 준다. DB 암호 등 환경 전체를 출력하지 않는다.
- `scripts/verify.sh` / `scripts/run.sh`: 기존처럼 env.sh를 읽기 때문에 새 프로젝트 Java로 Maven과 앱을 실행한다.

PHP 비교: 프로젝트 실행 스크립트에서 사용할 PHP 실행 파일 경로를 정하는 것과 같다. PHP CLI와 PHP-FPM이 서로 다른 버전을 쓸 수 있듯, 터미널의 일반 java 명령·IDE·이미 실행 중인 앱은 별도로 확인해야 한다. Java 실행 파일이 변경돼도 Controller → Service → MyBatis → MariaDB → JSP의 업무 요청 흐름은 같다.

실행:

```sh
./scripts/java-info.sh
./scripts/db-start.sh
./scripts/verify.sh
./scripts/run.sh -Dspring-boot.run.profiles=demo
```

명시적 JAVA_HOME이 이전 JDK를 가리키면 그 값을 존중한다. 이 경우 프로젝트 기본 선택을 확인하려면 `env -u JAVA_HOME ./scripts/java-info.sh`를 사용한다. IDE의 프로젝트 SDK/Maven Runner도 위 Contents/Home을 선택해야 한다. 실행 중인 앱은 파일 설치만으로 Java가 바뀌지 않으며 정상 종료 후 새 스크립트로 시작해야 한다. 이번에는 기존 앱이 실행 중이지 않았고 테스트용 서버만 실행했다.

직접 Maven/실행 WAR를 실행하는 경우 현재 셸에만 다음처럼 지정한다.

```sh
export JAVA_HOME="$PWD/.local/jdks/temurin-21.0.13+9/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

설치되지 않은 복제 사본은 fallback JDK를 사용할 수 있으므로 java-info.sh로 **전체 빌드와 공급자**를 확인한 뒤 실행한다. 긴급 복귀를 위해 기존 JDK를 남겼지만, 기존 신뢰 저장소 문제도 남아 있으므로 검증 실패 시 구버전을 안전한 정상 상태라고 안내하지 않는다.

## 인증서 수정 전후 검증

[공급자가 안내한 공개 테스트 자료](https://emudhra.we-have-a-problem.com/)의 chains.zip을 받아 읽기 전용 Java TrustManager로 확인했다. 문제 체인을 임의 시스템/Java 저장소에 설치하지 않았고 인증서 검증을 끄지 않았다. 묶음에는 일반 14개와 키 rollover 변형 2개가 있다.

| 항목 | 기존 Homebrew 21.0.12.1 | 새 Temurin 21.0.13+9 |
|---|---|---|
| 관련 G3·AAA 루트 | 기본 신뢰 목록에 존재 | 기본 신뢰 목록에서 제거 |
| 공개 문제 체인 일반 14개 | 모두 허용 | 모두 거부 |
| rollover 변형 2개 | 모두 허용 | 모두 거부 |
| 정상 Maven Central HTTPS | 이번 비교에서 별도 확인 안 함 | HTTP 200 |
| 정상 GitHub API HTTPS | 이번 비교에서 별도 확인 안 함 | HTTP 200 |

이 검사는 알려진 체인의 인증서 경로 신뢰 비교다. 실사용 트래픽의 MITM 공격을 시도한 것이 아니고 서명 JAR의 모든 공격 경로를 검증한 것도 아니다. 정상 HTTPS는 기본 인증서/호스트 검증을 유지한 Java HttpClient로 확인했고 개인 인증정보를 보내지 않았다. 외부 연동을 추후 추가하면 해당 대상도 검증한다.

검증용 Java 코드의 첫 실행에서는 EC 키에 대해 TLS authType을 EC로 넘겨 기존 JDK까지 모두 거부하는 잘못된 비교가 나왔다. 이를 EC→ECDHE_ECDSA/RSA→ECDHE_RSA로 보정하고 **기존 허용 16 / 새 거부 16**의 최종 결과를 확인했다. 잘못된 첫 결과를 패치 성공 증거로 사용하지 않는다.

증거: `.local/java-patch-20261031-before.log`, `after.log`, `cert-summary.json`, `.local/JavaTrustCheck.java`. 공개 체인 묶음의 출처/SHA256은 cert-summary.json에 기록했다. 도구와 묶음은 Git 제외 로컬 증거다. 기존 보안 보고서의 A01은 그 당시 확인 결과이며 이번 결과로 프로젝트 실행 Java의 조치를 갱신한다.

## 회귀 검증과 한계

최종 `./scripts/verify.sh`는 **93개, 실패/오류/스킵 0, WAR BUILD SUCCESS(39.213초)**. TEST_RESULTS.md와 .local/java-patch-20261031-tests-final.log에 기록했다. 첫 새 JDK 실행은 Mockito가 임시 파일을 만들 때 `No space left on device`가 발생해 오류 MVC 테스트 19개가 실패했다. JVM 교체 호환성 오류로 단정하지 않고 원인을 확인했다. 앞선 점검에서 받은 재생성 가능한 Trivy DB 캐시만 제거해 약 1.5GB를 회수한 뒤 재실행해 통과했다. 보안 검사 JSON·보고서·사용자 파일·DB 데이터는 삭제하지 않았다.

구문 검사와 실제 Java 선택 확인: 기본은 Temurin 21.0.13+9, 명시적 이전 JAVA_HOME은 Homebrew 21.0.12.1을 선택함을 확인했다. 설치 JDK는 Git ignore 적용을 확인했다. 전역 Java·운영 서버·Docker 실행 검증과 DB 최소 권한 분리는 이번 범위 밖이다.

## 학습 과제 1개

`./scripts/java-info.sh`를 실행하고 이 문서 맨 아래에 본인이 확인한 배포판·전체 빌드를 한 줄 기록한다. 출력된 버전만 적고 환경 전체나 암호는 붙이지 않는다. 이 과제는 Codex가 대신 수행하지 않는다.

## 이해도 질문 3개

1. 일반 터미널의 java와 프로젝트 실행 스크립트의 Java가 서로 다를 수 있는 이유는 무엇인가?
2. 문제 인증서 체인을 거부하는 검사 외에 정상 HTTPS도 확인한 이유는 무엇인가?
3. 이 Mac에 새 JDK를 설치하면 운영 서버와 Docker 안의 Java도 자동으로 바뀌는가?
