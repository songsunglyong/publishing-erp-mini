<%@ page pageEncoding="UTF-8" %>
<%@ include file="../common/header.jspf" %>
<h1>계정 관리</h1><p>ADMIN은 계정 관리, MANAGER는 도서·거래처 조회와 변경, USER는 조회를 할 수 있습니다. 직원 정보는 본인과 ADMIN만 조회합니다.</p>
<table class="table"><thead><tr><th>이름</th><th>아이디</th><th>권한</th><th>상태</th><th>작업</th></tr></thead><tbody>
<c:forEach var="account" items="${accounts}"><tr><td><c:out value="${account.displayName}"/></td><td><c:out value="${account.username}"/></td><td><c:out value="${account.role}"/></td><td>${account.enabled ? '사용' : '차단'}</td><td>
<c:if test="${account.enabled and account.id != currentUser.id}"><form method="post" action="/admin/users/${account.id}/block" data-confirm="계정을 차단하시겠습니까? 해당 계정의 모든 로그인 세션이 종료됩니다."><input type="hidden" name="_csrf" value="<c:out value='${_csrf.token}'/>"><button class="btn btn-outline-danger btn-sm">차단</button></form></c:if></td></tr></c:forEach>
</tbody></table><h2>계정 등록</h2><p>아이디는 영문 소문자로 시작하는 소문자·숫자·밑줄 3~40자입니다. 비밀번호는 12자 이상, UTF-8 기준 72바이트 이내로 입력하세요.</p>
<form method="post" action="/admin/users" class="card card-body">
<input type="hidden" name="_csrf" value="<c:out value='${_csrf.token}'/>">
<label for="accountUsername">아이디</label><input id="accountUsername" name="username" value="<c:out value='${inputUsername}'/>" class="form-control mb-2" pattern="[a-z][a-z0-9_]{2,39}" maxlength="40" autocomplete="off" required>
<label for="displayName">이름</label><input id="displayName" name="displayName" value="<c:out value='${inputDisplayName}'/>" class="form-control mb-2" maxlength="100" required>
<label for="role">권한</label><select id="role" name="role" class="form-select mb-2"><option value="USER">일반 사용자</option><option value="MANAGER">업무 관리자</option><option value="ADMIN">시스템 관리자</option></select>
<label for="accountPassword">초기 비밀번호</label><input id="accountPassword" type="password" name="password" class="form-control mb-2" minlength="12" maxlength="72" autocomplete="new-password" required>
<button class="btn btn-primary">계정 등록</button></form>
<%@ include file="../common/footer.jspf" %>
