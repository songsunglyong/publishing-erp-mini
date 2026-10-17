<%@ page pageEncoding="UTF-8" %>
<%@ include file="../common/header.jspf" %>
<div class="d-flex justify-content-between align-items-center mb-4"><h1>거래처 관리</h1><a class="btn btn-primary" href="/partners/new">거래처 등록</a></div>
<form:form method="get" modelAttribute="query" action="/partners" cssClass="card card-body mb-4">
<div class="row g-3 align-items-end"><div class="col-md-7"><form:label path="q" cssClass="form-label">거래처명·코드·담당자</form:label><form:input path="q" cssClass="form-control" maxlength="100"/></div>
<div class="col-md-3"><form:label path="status" cssClass="form-label">사용 상태</form:label><form:select path="status" cssClass="form-select"><form:option value="">전체</form:option><form:option value="true">사용</form:option><form:option value="false">비활성</form:option></form:select></div><div class="col-md-2"><button class="btn btn-dark w-100">검색</button></div></div></form:form>
<div class="card"><div class="table-responsive"><table class="table mb-0"><thead><tr><th>거래처명 / 코드</th><th>유형</th><th>담당자 / 연락처</th><th>상태</th><th>작업</th></tr></thead><tbody>
<c:forEach var="row" items="${result.items}"><tr><td><strong><c:out value="${row.name}"/></strong><div class="small text-secondary"><c:out value="${row.code}"/></div></td><td>${row.type == 'BOOKSTORE' ? '서점' : '유통사'}</td><td><c:out value="${row.contactPerson}"/><div class="small"><c:out value="${row.phone}"/></div></td><td><span class="${row.active ? '' : 'status-off'}">${row.active ? '사용' : '비활성'}</span></td><td><div class="actions"><a class="btn btn-sm btn-outline-primary" href="/partners/${row.id}/edit">수정</a>
<c:if test="${row.active}"><form method="post" action="/partners/${row.id}/deactivate" data-confirm="비활성화하시겠습니까? 데이터는 보존됩니다."><input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"><button class="btn btn-sm btn-outline-secondary">비활성화</button></form></c:if></div></td></tr></c:forEach>
<c:if test="${empty result.items}"><tr><td colspan="5" class="text-center py-5">검색 결과가 없습니다.</td></tr></c:if>
</tbody></table></div></div><c:set var="section" value="partners"/><%@ include file="../common/pagination.jspf" %>
<%@ include file="../common/footer.jspf" %>
