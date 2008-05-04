<%@ page language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="header.jsp"%>
<br>
<%
	String title = "Results";
%>
<%@ include file="block_header.jsp"%>
<%
  String msg = request.getParameter("msg");
%>
	<table align="center" cellspacing="0" cellpadding="5">
		<tr><td height="5"></td></tr>
		<tr><td width="50%" align="center">
			<h3>Results: <%= msg %></h3>
		</td></tr>
	</table>

<%@ include file="block_footer.jsp"%>
<%@ include file="footer.jsp"%>
