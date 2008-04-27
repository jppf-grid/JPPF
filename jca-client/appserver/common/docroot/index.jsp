<%@ page language="java" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="jndiName.jsp"%>
<%@ include file="header.jsp"%>
<%
	  int duration = 5;
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
			String msg = new DemoTest(jndiName).testConnector(duration);
			response.sendRedirect(request.getContextPath()+"/index.jsp?msg="+msg);
	  }
		else
		{
			String text = (String) session.getAttribute("duration");
			if (text != null)
			{
			  try
			  {
			  	duration = Integer.parseInt(text);
			  }
			  catch(NumberFormatException ignored)
			  {
			  }
			}
%>
<%
			String title = "Submit a task";
%>
<%@ include file="block_header.jsp"%>
		<table align="center" cellspacing="0" cellpadding="5">
			<tr><td height="5"></td></tr>
			<tr><td align="center">
				<h3>Click on the button to submit a task to JPPF</h3>
				<h4>This will submit a task that will be executed for the specified duration</h4>
			</td></tr>
	
			<tr><td align="center">
				<form name="jppftest" action="<%=request.getContextPath()%>/index.jsp" method="post">
					<input type="hidden" value="true" name="perform">
					Duration in seconds: <input type="text" value="<%= duration %>" name="duration" maxLength="3">&nbsp;&nbsp;
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
