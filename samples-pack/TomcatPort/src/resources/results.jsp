<%@ page language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="header.jsp"%>
<br>
<h1>Results</h1>
<%
  String msg = (String) session.getAttribute("result");
  if (msg == null) msg = "No results available - please submit a new job";
  else session.removeAttribute("result");
%>
<table cellspacing="0" cellpadding="0">
	<tr><td align="center"><%= msg %></td></tr>
</table>

<%@ include file="footer.jsp"%>
