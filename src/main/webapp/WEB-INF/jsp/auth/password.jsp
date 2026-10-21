<%@ page pageEncoding="UTF-8" %>
<%@ include file="../common/header.jspf" %>
<div class="form-shell"><h1>비밀번호 변경</h1>
<p>새 비밀번호는 12자 이상, UTF-8 기준 72바이트 이내로 입력하세요. 변경 후 모든 기기에서 로그아웃됩니다.</p>
<form method="post" action="/account/password" class="card card-body">
<input type="hidden" name="_csrf" value="<c:out value='${_csrf.token}'/>">
<label for="currentPassword" class="form-label">현재 비밀번호</label><input class="form-control mb-3" id="currentPassword" name="currentPassword" type="password" autocomplete="current-password" maxlength="200" required>
<label for="newPassword" class="form-label">새 비밀번호</label><input class="form-control mb-3" id="newPassword" name="newPassword" type="password" autocomplete="new-password" minlength="12" maxlength="72" required>
<button class="btn btn-primary">변경</button></form></div>
<%@ include file="../common/footer.jspf" %>
