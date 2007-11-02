<%@ page language="java" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="header.jsp"%>
<%@ include file="menu.jsp"%>
<%
	  String perform = request.getParameter("perform");
	  if (perform != null)
	  {
		  String text = request.getParameter("duration");
		  int duration = 15;
		  try
		  {
		  	duration = Integer.parseInt(text);
		  }
		  catch(NumberFormatException ignored)
		  {
		  }
			String msg = new DemoTest(jndiName).testConnector(duration);
			response.sendRedirect(request.getContextPath()+"/index.jsp?msg="+msg);
	  }
		else
		{
%>
		<table align="center" width="80%" cellspacing="0" cellpadding="5" class="table_">
			<tr><td height="5"></td></tr>
			<tr><td width="50%" align="center">
				<h3>Click on the button to submit a task to JPPF</h3>
				<h4>This will submit a task that will be executed for the specified duration</h4>
			</td></tr>
	
			<tr><td width="50%" align="center">
				<form name="jppftest" action="<%=request.getContextPath()%>/index.jsp" method="post">
					<input type="hidden" value="true" name="perform">
					Duration in seconds: <input type="text" value="15" name="duration" maxLength="3">&nbsp;&nbsp;
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
<%@ include file="footer.jsp"%>
