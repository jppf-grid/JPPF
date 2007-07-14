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
  	InitialContext ctx = new InitialContext();
		Object objref = ctx.lookup("eis/JPPFConnectionFactory");
		ConnectionFactory cf = (ConnectionFactory) PortableRemoteObject.narrow(objref, ConnectionFactory.class);
		JPPFConnection test = (JPPFConnection) cf.getConnection();
		JPPFTask task = new DemoTask();
		List list = new ArrayList();
		list.add(task);
		list = test.submit(list, null);
		String msg = null;
		task = (JPPFTask) list.get(0);
		if (task.getException() != null) msg = task.getException().getMessage();
		else msg = (String) task.getResult();
		test.close();
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