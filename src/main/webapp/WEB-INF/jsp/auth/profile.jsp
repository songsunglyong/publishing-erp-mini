<%@ page pageEncoding="UTF-8" %>
<%@ include file="../common/header.jspf" %>
<h1>직원 정보</h1><dl><dt>이름</dt><dd><c:out value="${profile.displayName}"/></dd><dt>아이디</dt><dd><c:out value="${profile.username}"/></dd><dt>권한</dt><dd><c:out value="${profile.role}"/></dd></dl>
<%@ include file="../common/footer.jspf" %>
