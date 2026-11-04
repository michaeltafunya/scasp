<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Courses</title></head>
<body>
<%@ include file="nav.jspf" %>
<h1>Courses</h1>
<c:if test="${not empty sessionScope.msg}"><p role="status"><c:out value="${sessionScope.msg}"/></p><c:remove var="msg" scope="session"/></c:if>
<form method="get"><input name="q" placeholder="Search" value="<c:out value='${param.q}'/>"><button>Search</button></form>
<c:choose>
<c:when test="${empty courses}"><p>No courses found.</p></c:when>
<c:otherwise>
<table border="1"><tr><th>Code</th><th>Title</th><th>Capacity</th><th>Registered</th><c:if test="${sessionScope.role == 'STUDENT'}"><th>Action</th></c:if></tr>
<c:forEach var="c" items="${courses}">
<tr><td><c:out value="${c.code}"/></td><td><c:out value="${c.title}"/></td><td>${c.capacity}</td><td>${report[c.code] == null ? 0 : report[c.code]}</td>
<c:if test="${sessionScope.role == 'STUDENT'}"><td>
<form method="post" action="courses"><input type="hidden" name="code" value="<c:out value='${c.code}'/>">
<button name="action" value="register">Register</button> <button name="action" value="withdraw">Withdraw</button></form></td></c:if></tr>
</c:forEach></table>
</c:otherwise></c:choose>
</body></html>
