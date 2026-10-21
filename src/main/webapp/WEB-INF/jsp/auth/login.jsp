<%@ page pageEncoding="UTF-8" %>
<%@ include file="../common/header.jspf" %>
<div class="form-shell"><h1>로그인</h1>
<c:if test="${param.logout != null}"><p role="status"><spring:message code="auth.logout.done"/></p></c:if>
<c:if test="${param.passwordChanged != null}"><p role="status"><spring:message code="auth.password.changed"/></p></c:if>
<form method="post" action="/login" class="card card-body">
<input type="hidden" name="_csrf" value="<c:out value='${_csrf.token}'/>">
<label for="username" class="form-label">아이디</label><input id="username" name="username" class="form-control mb-3" maxlength="40" autocomplete="username" required>
<label for="password" class="form-label">비밀번호</label><input id="password" type="password" name="password" class="form-control mb-3" maxlength="200" autocomplete="current-password" required>
<button class="btn btn-primary">로그인</button>
</form></div>
<%@ include file="../common/footer.jspf" %>
