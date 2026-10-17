<%@ page pageEncoding="UTF-8" %>
<%@ include file="../common/header.jspf" %>
<div class="form-shell"><h1 class="mb-4">도서 ${empty entityId ? '등록' : '수정'}</h1>
<c:set var="saveAction" value="/books"/><c:if test="${not empty entityId}"><c:set var="saveAction" value="/books/${entityId}"/></c:if>
<form:form modelAttribute="form" method="post" action="${saveAction}" cssClass="card card-body" htmlEscape="true">
<input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
<form:errors path="*" cssClass="alert alert-danger" element="div"/>
<div class="row g-3"><div class="col-md-6"><form:label path="isbn" cssClass="form-label">ISBN-13 (선택)</form:label><form:input path="isbn" cssClass="form-control" maxlength="13"/><form:errors path="isbn" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="title" cssClass="form-label">도서명</form:label><form:input path="title" cssClass="form-control" maxlength="200"/><form:errors path="title" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="author" cssClass="form-label">저자</form:label><form:input path="author" cssClass="form-control" maxlength="100"/><form:errors path="author" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="publisher" cssClass="form-label">출판사</form:label><form:input path="publisher" cssClass="form-control" maxlength="100"/><form:errors path="publisher" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="publicationDate" cssClass="form-label">발행일</form:label><form:input path="publicationDate" cssClass="form-control" type="date" min="1000-01-01" max="9999-12-31"/><form:errors path="publicationDate" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="listPrice" cssClass="form-label">정가 (원)</form:label><form:input path="listPrice" cssClass="form-control" type="number" min="0" max="999999999999" step="1"/><form:errors path="listPrice" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="active" cssClass="form-label">사용 상태</form:label><form:select path="active" cssClass="form-select"><form:option value="true">사용</form:option><form:option value="false">비활성</form:option></form:select><form:errors path="active" cssClass="field-error"/></div>
</div>
<div class="actions mt-4"><button class="btn btn-primary" type="submit">저장</button><a class="btn btn-outline-secondary" href="/books">목록으로</a></div>
</form:form></div>
<%@ include file="../common/footer.jspf" %>
