<%@ page language="java" %>
<%@ page import="org.jppf.jca.demo.*" %>
<html>
	<head>
		<title>Java Parallel Processing Framework Home Page</title>
		<meta name="description" content="An open-source, Java-based, framework for parallel computing.">
		<meta name="keywords" content="JPPF, Java, Parallel Computing, Distributed Computing, Grid Computing, Cluster, Grid">
		<meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
		<link rel="stylesheet" type="text/css" href="./jppf.css" title="Style">
	</head>
	<body>
		<table align="center" width="80%" cellspacing="0" cellpadding="5"
			class="table_" style="background: url('images/grid.gif'); background-repeat: repeat; background-attachment: fixed">
			<tr><td height="5"></td></tr>
			<tr>
				<td width="50%" align="center">
					<img src="images/logo.gif" border="0" alt="Java Parallel Processing Framework"/>
				</td>
				<td width="50%" align="center" valign="center">
					<h3>Java Parallel Processing Framework</h3>
					<h3>Resource Adapter Demonstration</h3>
				</td>
			</tr>
			<tr><td height="5"></td></tr>
		</table>
<%
	  String perform = request.getParameter("perform");
	  if (perform != null)
	  {
			String msg = new DemoTest(jndiName).testConnector();
			response.sendRedirect(request.getContextPath()+"/index.jsp?msg="+msg);
	  }
		else
		{
%>
		<table align="center" width="80%" cellspacing="0" cellpadding="5" class="table_">
			<tr><td height="5"></td></tr>
			<tr><td width="50%" align="center">
				<h3>Click on this button to submit a task to JPPF&nbsp;</h3>
			</td></tr>
	
			<tr><td width="50%" align="center">
				<form name="jppftest" action="<%=request.getContextPath()%>/index.jsp" method="get">
					<input type="hidden" value="true" name="perform">
					<input type="submit" value="Submit">
				</form>
			</td></tr>
<%
			String msg = request.getParameter("msg");
			if (msg != null)
			{
%>
			<tr><td width="50%" align="center">
		    <h3>Result : <%= msg %></h3>
			</td></tr>
<%
			}
		}
%>
		</table>
	</body>
</html>
	