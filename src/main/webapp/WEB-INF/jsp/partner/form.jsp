<%@ page pageEncoding="UTF-8" %>
<%@ include file="../common/header.jspf" %>
<div class="form-shell"><h1 class="mb-4">거래처 ${empty entityId ? '등록' : '수정'}</h1>
<c:set var="saveAction" value="/partners"/><c:if test="${not empty entityId}"><c:set var="saveAction" value="/partners/${entityId}"/></c:if>
<form:form modelAttribute="form" method="post" action="${saveAction}" cssClass="card card-body" htmlEscape="true">
<input type="hidden" name="_csrf" value="<c:out value='${_csrf.token}'/>">
<form:errors path="*" cssClass="alert alert-danger" element="div"/>
<div class="row g-3"><div class="col-md-6"><form:label path="code" cssClass="form-label">거래처 코드</form:label><form:input path="code" cssClass="form-control"
            maxlength="40" placeholder="STORE_01"/><form:errors path="code" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="name" cssClass="form-label">거래처명</form:label><form:input path="name" cssClass="form-control" maxlength="150"/><form:errors path="name" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="type" cssClass="form-label">유형</form:label><form:select path="type" cssClass="form-select"><form:option value="">선택하세요</form:option><form:option value="BOOKSTORE">서점</form:option><form:option value="DISTRIBUTOR">유통사</form:option></form:select><form:errors path="type" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="contactPerson" cssClass="form-label">담당자</form:label><form:input path="contactPerson" cssClass="form-control" maxlength="100"/><form:errors path="contactPerson" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="phone" cssClass="form-label">전화번호</form:label><form:input path="phone" cssClass="form-control" maxlength="30"/><form:errors path="phone" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="email" cssClass="form-label">이메일</form:label><form:input path="email" cssClass="form-control" maxlength="254"/><form:errors path="email" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="address" cssClass="form-label">주소</form:label><form:input path="address" cssClass="form-control" maxlength="300"/><form:errors path="address" cssClass="field-error"/></div>
<div class="col-md-6"><form:label path="active" cssClass="form-label">사용 상태</form:label><form:select path="active" cssClass="form-select"><form:option value="true">사용</form:option><form:option value="false">비활성</form:option></form:select><form:errors path="active" cssClass="field-error"/></div>
</div>
<div class="actions mt-4"><button class="btn btn-primary" type="submit">저장</button><a class="btn btn-outline-secondary" href="/partners">목록으로</a></div>
</form:form></div>
<%@ include file="../common/footer.jspf" %>
