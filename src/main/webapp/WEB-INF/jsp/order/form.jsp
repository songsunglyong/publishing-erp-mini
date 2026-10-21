<%@ page pageEncoding="UTF-8" %><%@ include file="../common/header.jspf" %>
<h1>주문 등록</h1><p>최대 20개 품목입니다. 단가를 비워 두면 현재 정가를 저장하며, 총액은 서버에서 계산합니다.</p>
<form:form method="post" action="/orders" modelAttribute="form" htmlEscape="true" id="order-form">
<input type="hidden" name="_csrf" value="<c:out value='${_csrf.token}'/>"><form:hidden path="requestKey"/>
<form:errors path="*" element="div" cssClass="alert alert-danger"/>
<form:label path="partnerId">거래처</form:label><form:select path="partnerId" cssClass="form-select mb-3"><form:option value="">선택하세요</form:option><c:forEach var="partner" items="${partners}"><form:option value="${partner.id}"><c:out value="${partner.name}"/> (<c:out value="${partner.code}"/>)</form:option></c:forEach></form:select>
<table class="table"><thead><tr><th>도서</th><th>수량</th><th>단가 (원, 비우면 정가)</th><th>작업</th></tr></thead><tbody id="order-lines">
<c:forEach var="line" items="${form.items}" varStatus="loop"><tr>
<td><form:select path="items[${loop.index}].bookId" cssClass="form-select"><form:option value="">선택하세요</form:option><c:forEach var="book" items="${books}"><form:option value="${book.id}"><c:out value="${book.title}"/> / <c:out value="${book.listPrice}"/>원</form:option></c:forEach></form:select></td>
<td><form:input path="items[${loop.index}].quantity" type="number" min="1" max="1000000" cssClass="form-control"/></td><td><form:input path="items[${loop.index}].unitPrice" type="number" min="0" max="999999999999" cssClass="form-control"/></td><td><button type="button" class="btn btn-outline-secondary remove-line">삭제</button></td>
</tr></c:forEach></tbody></table>
<button type="button" id="add-order-line" class="btn btn-outline-secondary">품목 추가</button><div class="my-3"><form:label path="notes">메모</form:label><form:textarea path="notes" maxlength="500" cssClass="form-control"/></div>
<button class="btn btn-primary">작성 저장</button><a href="/orders" class="btn btn-outline-secondary">목록으로</a>
</form:form><script src="/webjars/jquery/3.7.1/jquery.min.js"></script><script src="/js/order-form.js"></script>
<%@ include file="../common/footer.jspf" %>
