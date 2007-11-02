<%@ page language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="header.jsp"%>
<%@ include file="menu.jsp"%>
<%
  String id = request.getParameter("id");
  DemoTest demo = new DemoTest(jndiName);
  String msg = demo.getMessage(id);
%>
	<table align="center" width="80%" cellspacing="0" cellpadding="5" class="table_">
		<tr><td height="5"></td></tr>
		<tr><td width="50%" align="center">
			<h3>Results for submission ID &quot;<%= id %>&quot; :</h3> 
			<h3><%= msg %></h3>
		</td></tr>
	</table>
<%@ include file="footer.jsp"%>
