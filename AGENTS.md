# 작업 규칙 진입점

이 저장소의 모든 작업은 먼저 [doc/AGENTS.md](doc/AGENTS.md)를 읽고 따른다.
작업 전 `doc/PROJECT.md`, `doc/TODO.md`, `doc/DECISIONS.md`를 순서대로 읽어 현재 상태를 파악한다.
작업 완료 시 `doc/TODO.md`와 `doc/CHANGELOG.md`를 실제 결과에 맞게 갱신한다.

필수 학습 모드: 작은 기능 하나씩 PHP와 비교해 설명하고, 수정 과제 1개와 이해도 질문 3개를 제공한다. 개발자가 이해했다고 확인하기 전에는 다음 기능·단계로 진행하지 않는다. 현재 학습 상태는 `doc/LEARNING.md`에서 확인한다.

사용자 메시지·오류 처리 작업은 [doc/USER_MESSAGES.md](doc/USER_MESSAGES.md)의 에러 코드·한국어 안내·입력 보존·추적 ID·비노출 규칙도 따른다.

로그인·세션·권한 작업은 [doc/SECURITY.md](doc/SECURITY.md)의 역할 정책·단일 서버 범위·세션 폐기·CI 검증 기준을 따른다.
