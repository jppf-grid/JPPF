<%@ page language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="jndiName.jsp"%>
<%@ include file="header.jsp"%>
<%
  String id = request.getParameter("id");
  J2EEDemo demo = new J2EEDemo(jndiName);
  String msg = demo.getMessage(id);
	String title = "Result for submission ID \""+id+"\"";
%>
	<h1 align="center"><%= title %></h1>
	<div align="center">
	<table cellspacing="0" cellpadding="5">
		<tr><td height="5"></td></tr>
		<tr><td width="50%"><%= msg %></td></tr>
	</table>
	</div>
<%@ include file="footer.jsp"%>
