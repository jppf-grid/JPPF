<%@ page language="java" %>
<%@ page import="org.jppf.gigaspaces.test.*" %>
<%@ include file="header.jsp"%>
<%
	  long duration = 500L;
	  int nbTasks = 10;
	  String jobId = "Demo job";
	  String perform = request.getParameter("perform");
	  if (perform != null)
	  {
		  String text = request.getParameter("duration");
		  try
		  {
		  	duration = (long) (1000L * Float.parseFloat(text));
		  	session.setAttribute("duration", text);
		  }
		  catch(NumberFormatException ignored) {}
		  text = request.getParameter("nbTasks");
		  try
		  {
		  	nbTasks = Integer.parseInt(text);
		  	session.setAttribute("nbTasks", text);
		  }
		  catch(NumberFormatException ignored) {}
		  text = request.getParameter("jobId");
		  if ((text != null) && !"".equals(text.trim())) jobId = text;
		  session.setAttribute("jobId", text);
			String msg = TestFromJSP.testGS(jobId, nbTasks, duration);
			//response.sendRedirect(request.getContextPath()+"/index.jsp?msg="+msg);
%>
			<form name="jppftestBack" action="<%=request.getContextPath()%>/index.jsp" method="post">
				<table border="0" cellspacing="0" cellpadding="5">
					<tr><td><%=msg%></td></tr>
					<tr><td align="center"><input type="submit" value="Submit a new job"></td></tr>
				</table>
			</form>
<%
	  }
		else
		{
			String text = (String) session.getAttribute("duration");
			if (text != null)
			{
			  try
			  {
			  	duration = (long) (1000L * Float.parseFloat(text));
			  }
			  catch(NumberFormatException ignored) {}
			}
			text = (String) session.getAttribute("nbTasks");
			if (text != null)
			{
			  try
			  {
		  		nbTasks = Integer.parseInt(text);
			  }
			  catch(NumberFormatException ignored) {}
			}
			text = (String) session.getAttribute("jobId");
			if (text != null) jobId = text;
%>
			<div align="center">
			<h1>Submit a job</h1>
			<br/>
			<h4>Submit a job with the specified number of tasks and duration for each task</h4>
			<form name="jppftest" action="<%=request.getContextPath()%>/index.jsp" method="post">
				<table width="450" cellspacing="0" cellpadding="5">
					<tr>
						<td>Job name:</td>
						<td><input type="text" value="<%= jobId %>" name="jobId" maxLength="30"></td>
					</tr>
					<tr>
						<td>Number of tasks in the job:</td>
						<td><input type="text" value="<%= nbTasks %>" name="nbTasks" maxLength="5"></td>
					</tr>
					<tr>
						<td>Duration of each task in seconds:</td>
						<td><input type="text" value="<%= (float) duration / 1000f %>" name="duration" maxLength="10"></td>
					</tr>
					<tr><td align="center" colspan="*"><input type="hidden" value="true" name="perform"><input type="submit" value="Submit"></td></tr>
				</table>
			</form>
<%
			String msg = request.getParameter("msg");
			if (msg != null)
			{
%>
				<h3>Result : <%= msg %></h3>
<%
			}
%>
			</div>
<%
		}
%>
<%@ include file="footer.jsp"%>
