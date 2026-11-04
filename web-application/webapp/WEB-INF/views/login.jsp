<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>SCASP Login</title></head>
<body>
<h1>SCASP Login</h1>
<c:if test="${not empty error}"><p role="alert"><c:out value="${error}"/></p></c:if>
<form method="post" action="login">
  <label>User <input name="user" required></label>
  <label>Password <input name="password" type="password" required></label>
  <button>Login</button>
</form>
<p>Demo: student1 / demo, staff1 / demo</p>
</body></html>
