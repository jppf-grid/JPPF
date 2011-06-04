<%@ page language="java" %>
<%@ page import="org.jppf.example.tomcat.*" %>
<%@ include file="header.jsp"%>
<%
	  long duration = 500L;
	  int nbTasks = 10;
	  String jobId = "Demo job";

		String text = request.getParameter("duration");
		if (text != null)
		{
			try
			{
				duration = (long) (1000L * Float.parseFloat(text));
				session.setAttribute("duration", text);
			}
			catch(NumberFormatException ignored) {}
		}
		text = request.getParameter("nbTasks");
		if (text != null)
		{
			try
			{
				nbTasks = Integer.parseInt(text);
				session.setAttribute("nbTasks", text);
			}
			catch(NumberFormatException ignored) {}
		}
		text = request.getParameter("jobId");
		if ((text != null) && !"".equals(text.trim())) jobId = text;

	  String perform = request.getParameter("perform");
	  if (perform != null)
	  {
		  session.setAttribute("jobId", text);
			String msg = new Demo().submitJob(jobId, nbTasks, duration);
	  	session.setAttribute("result", msg);
			response.sendRedirect(request.getContextPath()+"/results.jsp");
	  }
		else
		{
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
			</div>
<%
		}
%>
<%@ include file="footer.jsp"%>
