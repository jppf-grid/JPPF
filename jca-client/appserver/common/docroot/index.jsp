<%@ page language="java" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="jndiName.jsp"%>
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
		  boolean blocking = request.getParameter("blocking") != null;
		  DemoTest test = new DemoTest(jndiName);
		  String msg = null;
			if (!blocking) msg = test.testConnector(jobId, duration, nbTasks);
			else msg = test.testConnectorBlocking(jobId, duration, nbTasks);
			response.sendRedirect(request.getContextPath()+"/index.jsp?msg="+msg);
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
		<h4>Submit a job with the specified number of tasks and duration for each task<br/>
		A blocking job will wait until the job has finished executing, whereas a non-blocking job will return immediately with a job submission id</h4>
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
					<td><input type="text" value="<%= (float) duration / 1000.0f %>" name="duration" maxLength="10"></td>
				</tr>
				<tr>
					<td colspan="2"><input type="checkbox" checked="true" name="blocking">Blocking job?</td>
				</tr>
				<tr><td align="center" colspan="2">&nbsp;<br/><input type="hidden" value="true" name="perform"><input type="submit" value="Submit"></td></tr>
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
	}
%>
		</div>
<%@ include file="footer.jsp"%>
