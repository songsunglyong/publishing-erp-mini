<%@ page pageEncoding="UTF-8" %>
<%@ include file="../common/header.jspf" %>
<div class="d-flex justify-content-between align-items-center mb-4"><h1>도서 관리</h1><a class="btn btn-primary" href="/books/new">도서 등록</a></div>
<form:form method="get" modelAttribute="query" action="/books" cssClass="card card-body mb-4">
<div class="row g-3 align-items-end"><div class="col-md-7"><form:label path="q" cssClass="form-label">도서명 또는 ISBN</form:label><form:input path="q" cssClass="form-control" maxlength="100"/></div>
<div class="col-md-3"><form:label path="status" cssClass="form-label">사용 상태</form:label><form:select path="status" cssClass="form-select"><form:option value="">전체</form:option><form:option value="true">사용</form:option><form:option value="false">비활성</form:option></form:select></div><div class="col-md-2"><button class="btn btn-dark w-100">검색</button></div></div></form:form>
<div class="card"><div class="table-responsive"><table class="table mb-0"><thead><tr><th>도서명 / ISBN</th><th>저자 / 출판사</th><th>정가</th><th>상태</th><th>작업</th></tr></thead><tbody>
<c:forEach var="row" items="${result.items}"><tr><td><strong><c:out value="${row.title}"/></strong><div class="small text-secondary"><c:out value="${row.isbn}" default="ISBN 없음"/></div></td><td><c:out value="${row.author}"/><div class="small"><c:out value="${row.publisher}"/></div></td><td><fmt:formatNumber value="${row.listPrice}" pattern="#,##0"/>원</td><td><span class="${row.active ? '' : 'status-off'}">${row.active ? '사용' : '비활성'}</span></td><td><div class="actions"><a class="btn btn-sm btn-outline-primary" href="/books/${row.id}/edit">수정</a>
<c:if test="${row.active}"><form method="post" action="/books/${row.id}/deactivate" data-confirm="비활성화하시겠습니까? 데이터는 보존됩니다."><input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><button class="btn btn-sm btn-outline-secondary">비활성화</button></form></c:if></div></td></tr></c:forEach>
<c:if test="${empty result.items}"><tr><td colspan="5" class="text-center py-5">검색 결과가 없습니다.</td></tr></c:if>
</tbody></table></div></div><c:set var="section" value="books"/><%@ include file="../common/pagination.jspf" %>
<%@ include file="../common/footer.jspf" %>
