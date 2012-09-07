<%@ page language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="jndiName.jsp"%>
<%@ include file="header.jsp"%>
		<div align="center">
		<h1>Results</h1>
		<table width="600" cellspacing="0" cellpadding="5">
			<tr><td height="5"></td></tr>
			<tr><td align="center">
				<h4>To refresh the list, please click on the &quot;Results&quot; button again</h4>
				<h4>Clicking on one of the &quot;Submission ID&quot; links, this will also remove the submission from the queue</h4>
			</td></tr>
		</table>

		<table cellspacing="0" cellpadding="5" border="1">
			<tr>
				<th colspan="3">Submissions Queue</th>
			</tr>
			<tr>
        <th>Submission ID</th>
        <th>Job Name</th>
				<th>Status</th>
			</tr>
<%
			J2EEDemo demo = new J2EEDemo(jndiName);
      Map map = JPPFHelper.getStatusMap();
			if (map.isEmpty())
			{
%>
			<tr>
				<td align="center" colspan="3">The submission queue is empty</a></td>
			</tr>
<%
			}
			else
			{
				Iterator<String> it = map.keySet().iterator();
				while (it.hasNext())
				{
					String id = it.next();
          String status = JPPFHelper.getStatus(id);
          String jobName = JPPFHelper.getJobName(id);
          String fontWeight = ("COMPLETE".equals(status) || "FAILED".equals(status)) ? "bold" : "normal";
          String color = "COMPLETE".equals(status) ? "green" : ("FAILED".equals(status) ? "red" : "black");
%>
			<tr>
<%
					if ("COMPLETE".equals(status) || "FAILED".equals(status))
					{
%>
				<td><a href="showResult.jsp?id=<%= id %>"><%= id %></a></td>
<%
					}
					else
					{
%>
				<td><%= id %></td>
<%
					}
%>
        <td align="center"><%= jobName %></td>
        <td align="center" style="font-weight: <%= fontWeight %>; color: <%= color %>"><%= status %></td>
			</tr>
<%
				}
			}
%>
		</table>
		</div>
<%@ include file="footer.jsp"%>
