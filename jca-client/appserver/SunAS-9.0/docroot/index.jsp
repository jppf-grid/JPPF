<%@ page language="java" %>
<%@ page import="
	java.util.*,
	javax.naming.*,
	javax.rmi.*,
	javax.resource.*,
	javax.resource.cci.*,
	org.jppf.jca.cci.*,
	org.jppf.jca.demo.*,
	org.jppf.server.protocol.*
" %>

<html>
<head>
</head>
<body>
<%
  String perform = request.getParameter("perform");
  if (perform != null)
  {
		String msg = new DemoTest("eis/JPPFConnectionFactory").testConnector();
		response.sendRedirect(request.getContextPath()+"/index.jsp?msg="+msg);
  }
	else
	{
		String msg = request.getParameter("msg");
		if (msg != null)
		{
%>
	    <p>Result : <%= msg %>
<%
		}
%>
		<p>
		<form name="jppftest" action="<%=request.getContextPath()%>/index.jsp" method="get">
			<input type="hidden" value="true" name="perform">
			<input type="submit" value="Submit">
		</form>
<%
	}
%>
</body>
</html>