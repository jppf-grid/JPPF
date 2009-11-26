<%@ page language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="org.jppf.jca.demo.*" %>
<%@ include file="jndiName.jsp"%>
<%@ include file="header.jsp"%>
		<div align="center">
		<h1>Results</h1>
		<table width="500" cellspacing="0" cellpadding="5">
			<tr><td height="5"></td></tr>
			<tr><td align="center">
				<h4>To refresh the list, please click on the &quot;Results&quot; button again</h4>
				<h4>Clicking on one of the &quot;Submission ID&quot; links, this will also remove the submission from the queue</h4>
			</td></tr>
		</table>

		<table cellspacing="0" cellpadding="5" border="1">
			<tr>
				<th colspan="2">Submissions Queue</th>
			</tr>
			<tr>
				<th>Submission ID</th>
				<th>Status</th>
			</tr>
<%
			DemoTest demo = new DemoTest(jndiName);
			Map map = demo.getStatusMap();
			if (map.isEmpty())
			{
%>
			<tr>
				<td align="center" colspan="2">The submission queue is empty</a></td>
			</tr>
<%
			}
			else
			{
				Iterator it = map.keySet().iterator();
				while (it.hasNext())
				{
					String id = (String) it.next();
					String status = (String) map.get(id);
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
				<td align="center"><%= status %></td>
			</tr>
<%
				}
			}
%>
		</table>
		</div>
<%@ include file="footer.jsp"%>
