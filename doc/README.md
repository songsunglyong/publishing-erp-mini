# 설치 및 실행

## 현재 상태

현재는 프로젝트 운영 문서만 있으며 설치할 애플리케이션이나 실행 서버가 없다. 기술 스택과 런타임은 미정이다. 애플리케이션 초기 구성 후 검증된 설치·실행·빌드·테스트 명령을 이 문서에 추가한다.

## 준비 도구

- Git: 소스와 문서 버전 관리.
- GitHub 계정 및 저장소 접근 권한: 비공개 저장소 복제와 동기화.
- GitHub CLI(`gh`): 선택 사항. 저장소 생성과 인증 확인에 사용한다.

## 로컬 작업

저장소 루트에서 다음 순서로 문서를 읽는다.

1. `doc/AGENTS.md`
2. `doc/PROJECT.md`
3. `doc/TODO.md`
4. `doc/DECISIONS.md`

작업 상태와 변경 내용을 확인한다.

```sh
git status
git diff
```

작업 완료 후 `doc/TODO.md`와 `doc/CHANGELOG.md`를 갱신한다.

## GitHub 인증 및 연결 확인

```sh
gh auth status
git remote -v
git branch -vv
```

인증이 없거나 만료되었다면 로컬 터미널에서 `gh auth login --hostname github.com`으로 로그인한다. 토큰과 비밀번호를 문서나 커밋에 기록하지 않는다.

## 검증

현재 단계에서는 문서 누락, 내부 링크, Git 변경 내역과 공백 오류를 확인한다.

```sh
git diff --check
```

애플리케이션 테스트는 소스 코드와 테스트 설정이 추가된 뒤 실행한다.
