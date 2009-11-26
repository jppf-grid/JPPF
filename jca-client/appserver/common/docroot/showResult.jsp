<%@ page language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="jndiName.jsp"%>
<%@ include file="header.jsp"%>
<%
  String id = request.getParameter("id");
  DemoTest demo = new DemoTest(jndiName);
  String msg = demo.getMessage(id);
	String title = "Result for submission ID \""+id+"\"";
%>
	<h1 align="center"><%= title %></h1>
	<table align="center" cellspacing="0" cellpadding="5">
		<tr><td height="5"></td></tr>
		<tr><td width="50%" align="center">
			<h3><%= msg %></h3>
		</td></tr>
	</table>
<%@ include file="footer.jsp"%>
