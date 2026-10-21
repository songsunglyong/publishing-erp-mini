<%@ page pageEncoding="UTF-8" %><%@ include file="../common/header.jspf" %>
<h1>${adjustment ? '재고 조정' : '재고 입고'}</h1><p>${adjustment ? '현재 재고에서 더하거나 뺄 수량을 입력하세요. 조정 사유는 필수입니다.' : '양수 수량과 입고 사유를 입력하세요.'}</p>
<c:set var="saveAction" value="${adjustment ? '/inventory/adjustments' : '/inventory/receipts'}"/>
<form:form modelAttribute="form" method="post" action="${saveAction}" htmlEscape="true" cssClass="card card-body"><input type="hidden" name="_csrf" value="<c:out value='${_csrf.token}'/>"><form:hidden path="requestKey"/><form:errors path="*" cssClass="alert alert-danger" element="div"/>
<form:label path="bookId">도서</form:label><form:select path="bookId" cssClass="form-select mb-3"><form:option value="">선택하세요</form:option><c:forEach var="book" items="${books}"><form:option value="${book.id}"><c:out value="${book.title}"/></form:option></c:forEach></form:select>
<form:label path="quantity">처리 수량</form:label><form:input path="quantity" type="number" min="${adjustment ? -1000000 : 1}" max="1000000" cssClass="form-control mb-3"/>
<form:label path="note">처리 사유</form:label><form:textarea path="note" maxlength="500" cssClass="form-control mb-3"/><button class="btn btn-primary">${adjustment ? '조정 저장' : '입고 저장'}</button></form:form>
<%@ include file="../common/footer.jspf" %>
