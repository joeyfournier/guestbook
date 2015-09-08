<?xml version="1.0" encoding="UTF-8"?>
<%@page contentType="text/html; charset=UTF-8" import="java.util.Map" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<title>Subscription Required</title>
	<meta http-equiv="Content-Type" content="application/xhtml+xml; charset=UTF-8" />
	<link rel="stylesheet" type="text/css" href="consumer-servlet.css" />
</head>
<body>
	<div>
	<%
	if (request.getParameter("message")!=null)
	{
		%>
		<div><b><%=request.getParameter("message")%></b></div>
		<%
	}
	%>
		<div>You must purchase a valid subscription to this application from <a href="http://appdirect.com">AppDirect</a></div>
		<div>If you or your company has already purchases this application, you need to be a valid user and login through <a href="http://appdirect.com">AppDirect</a>.</div>
		<div>See your company administrator to be added as a valid user.</div>
	</div>
</body>
</html>