# Claude Code 프로젝트 진입점

@AGENTS.md

공통 지침은 위 파일을 가져와 사용하며 이 파일에 복제하지 않는다.

분야별 지침은 `.claude/rules/`의 Markdown 파일에서 관리한다. 모든 규칙을 전역 적용하며 `paths` 조건을 두지 않는다. 자동으로 로딩되는 규칙 파일을 이 파일에서 다시 `@`로 가져오지 않는다.

가져오기·규칙 로딩 방식은 [Claude Code 공식 문서](https://code.claude.com/docs/en/memory)를 따른다. 실제 세션에서 읽혔는지 확인하기 전에는 로딩 검증을 완료했다고 기록하지 않는다.
