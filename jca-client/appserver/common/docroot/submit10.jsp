<%@ page language="java" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="jndiName.jsp"%>
<%@ include file="header.jsp"%>
<%
	int duration = 2;
  String perform = request.getParameter("perform");
  if (perform != null)
  {
	  String text = request.getParameter("duration");
	  try
	  {
	  	duration = Integer.parseInt(text);
	  	session.setAttribute("duration", text);
	  }
	  catch(NumberFormatException ignored)
	  {
	  }
		new DemoTest(jndiName).testConnector2();
		response.sendRedirect(request.getContextPath()+"/submit10.jsp?msg=Submitted");
  }
	else
	{
%>
<%
		String title = "Submit a task";
%>
<%@ include file="block_header.jsp"%>
	<table width="600" align="center" cellspacing="0" cellpadding="5">
		<tr><td height="5"></td></tr>
		<tr><td align="center">
			<h3>Click on the button to submit a task to JPPF</h3>
			<h4>This will submit a task that will be executed for the specified duration</h4>
		</td></tr>

		<tr><td align="center">
			<form name="jppftest" action="<%=request.getContextPath()%>/submit10.jsp" method="post">
				<input type="hidden" value="true" name="perform">
				<input type="submit" value="Submit">
			</form>
		</td></tr>
<%
		String msg = request.getParameter("msg");
		if (msg != null)
		{
%>
		<tr><td align="center">
	    <h3>Result : <%= msg %></h3>
		</td></tr>
<%
		}
	}
%>
	</table>
<%@ include file="block_footer.jsp"%>
<%@ include file="footer.jsp"%>
