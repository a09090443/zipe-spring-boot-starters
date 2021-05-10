<%--
  Created by IntelliJ IDEA.
  User: B04575
  Date: 2021/4/22
  Time: 上午 09:05
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<html>
<head>
    <script src="<c:url value="/static/js/hello.js" />"></script>
    <title>Jsp</title>
</head>
<body>
<h1>Jsp Say Hello!!</h1>
</body>
<script>
    (function () {
        hello();
    })();

</script>
</html>
