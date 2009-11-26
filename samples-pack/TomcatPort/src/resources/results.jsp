<%@ page language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="header.jsp"%>
<br>
<h1>Results</h1>
<%
  String msg = request.getParameter("msg");
%>
<table cellspacing="0" cellpadding="0">
	<tr><td align="center"><h3>Results: <%= msg %></h3></td></tr>
</table>

<%@ include file="footer.jsp"%>
