<%@ page language="java" %>
<%@ page import="sample.dist.tasklength.*" %>
<%@ page import="org.jppf.client.*" %>
<%@ page import="org.jppf.server.protocol.*" %>
<%@ page import="org.jppf.utils.*" %>
<%@ page import="java.util.*" %>
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
		  List list = new ArrayList();
		  list.add(new LongTask(1000*duration));
		  JPPFClient jppfClient = (JPPFClient) session.getAttribute("jppfClient");
		  if (jppfClient == null)
		  {
		  	jppfClient = new JPPFClient();
		  	session.setAttribute("jppfClient", jppfClient);
		  }
		  List results = jppfClient.submit(list, null);
		  String msg = (String) ((JPPFTask) results.get(0)).getResult();
			response.sendRedirect(request.getContextPath()+"/results.jsp?msg="+msg);
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
<br>
			<h1 align="center">Submit a task</h1>
			<table align="center" cellspacing="0" cellpadding="0">
				<tr><td height="5"></td></tr>
				<tr><td width="50%" align="center">
					<h3>Click on the button to submit a task to JPPF</h3>
					<h4>This will submit a task that will be executed for the specified duration</h4>
				</td></tr>

				<tr><td width="50%" align="center">
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
				<tr><td width="50%" align="center">
					<h3>Result : <%= msg %></h3>
				</td></tr>
<%
				}
			}
%>
			</table>
<%@ include file="footer.jsp"%>
