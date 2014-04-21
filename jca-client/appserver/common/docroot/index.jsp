<%@ page language="java" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="jndiName.jsp"%>
<%@ include file="header.jsp"%>
<%
	  long duration = 500L;
	  int nbJobs = 1;
	  int nbTasks = 10;
	  String jobId = "Demo job";
	  String perform = request.getParameter("perform");
	  boolean blocking = true;
	  if (perform != null)
	  {
		  String text = request.getParameter("duration");
		  try {
		  	duration = (long) (1000L * Float.parseFloat(text));
		  	session.setAttribute("duration", text);
		  } catch(NumberFormatException ignored) {}
		  text = request.getParameter("nbJobs");
		  try {
		  	nbJobs = Integer.parseInt(text);
		  	session.setAttribute("nbJobs", text);
		  } catch(NumberFormatException ignored) {}
		  text = request.getParameter("nbTasks");
		  try {
		  	nbTasks = Integer.parseInt(text);
		  	session.setAttribute("nbTasks", text);
		  } catch(NumberFormatException ignored) {}
		  text = request.getParameter("jobId");
		  if ((text != null) && !"".equals(text.trim())) jobId = text;
		  session.setAttribute("jobId", text);
		  blocking = request.getParameter("blocking") != null;
		  session.setAttribute("blocking", "" + blocking);
		  J2EEDemo demo = new J2EEDemo(jndiName);
		  String msg = null;
			msg = demo.testMultipleJobs(nbJobs, jobId, duration, nbTasks, blocking);
			if ((msg != null) && msg.startsWith("Error:")) response.sendRedirect(request.getContextPath() + "/index.jsp?msg="+msg);
			else response.sendRedirect(request.getContextPath() + "/results.jsp");
	  }
		else
		{
			String text = (String) session.getAttribute("duration");
			if (text != null) {
			  try {
			  	duration = (long) (1000L * Float.parseFloat(text));
			  } catch(NumberFormatException ignored) {}
			}
			text = (String) session.getAttribute("nbJobs");
			if (text != null) {
			  try {
		  		nbJobs = Integer.parseInt(text);
			  } catch(NumberFormatException ignored) {}
			}
			text = (String) session.getAttribute("nbTasks");
			if (text != null) {
			  try {
		  		nbTasks = Integer.parseInt(text);
			  } catch(NumberFormatException ignored) {}
			}
			text = (String) session.getAttribute("jobId");
			if (text != null) jobId = text;
			text = (String) session.getAttribute("blocking");
			if (text != null) blocking = Boolean.valueOf(text);
%>
		<div align="center">
		<h1>Submit a job</h1>
		<br/>
		<h4>Submit the specified number of jobs, each with the specified number of tasks, task duration and blocking indicator</h4>
		<form name="jppftest" action="<%=request.getContextPath()%>/index.jsp" method="post">
			<table width="450" cellspacing="0" cellpadding="5">
				<tr>
					<td>Number of jobs:</td>
					<td><input type="text" value="<%= nbJobs %>" name="nbJobs" maxLength="5"></td>
				</tr>
        <tr><td colspan="2" style="font-size: 90%"><i>This is the prefix for the job names, they will be suffixed with &quot;1, 2 ...&quot;</i></td></tr>
				<tr>
					<td>Job name:</td>
					<td><input type="text" value="<%= jobId %>" name="jobId" maxLength="30"></td>
				</tr>
				<tr>
					<td>Number of tasks in each job:</td>
					<td><input type="text" value="<%= nbTasks %>" name="nbTasks" maxLength="5"></td>
				</tr>
				<tr>
					<td>Duration of each task in seconds:</td>
					<td><input type="text" value="<%= (float) duration / 1000.0f %>" name="duration" maxLength="10"></td>
				</tr>
        <tr><td colspan="2" style="font-size: 90%"><i>For non-blocking jobs, you will be redirected to the results page immediately, otherwise you will wait until the jobs are completed</i></td></tr>
				<tr>
					<td colspan="2"><input type="checkbox" checked="<%= blocking %>" name="blocking">Blocking job?</td>
				</tr>
				<tr><td align="center" colspan="2">&nbsp;<br/><input type="hidden" value="true" name="perform"><input type="submit" value="Submit"></td></tr>
			</table>
		</form>
<%
		String msg = request.getParameter("msg");
		if (msg != null)
		{
%>
		<h4><font color="red"><%= msg %></font></h4>
<%
		}
	}
%>
		</div>
<%@ include file="footer.jsp"%>
