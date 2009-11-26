<%@ page language="java" %>
<%@ page import="org.jppf.gigaspaces.test.*" %>
<%@ include file="header.jsp"%>
<%
	  String perform = request.getParameter("perform");
	  if (perform != null)
	  {
	  	String msg = TestFromJSP.testGS();
			response.sendRedirect(request.getContextPath()+"/index.jsp?msg="+msg);
	  }
		else
		{
%>
		<h1>Submit a job</h1>
		<table width="600" cellspacing="0" cellpadding="5">
			<tr><td height="5"></td></tr>
			<tr><td>
				<h3>Click on the button to submit a job to JPPF</h3>
				<h4>This will submit a job to the JPPF processing unit</h4>
			</td></tr>
	
			<tr><td>
				<form name="jppftest" action="<%=request.getContextPath()%>/index.jsp" method="post">
					<input type="hidden" value="true" name="perform">
					<input type="submit" value="Submit">
				</form>
			</td></tr>
<%
			String msg = request.getParameter("msg");
			if (msg != null)
			{
%>
			<tr><td>
		    <h3>Result : <%= msg %></h3>
			</td></tr>
<%
			}
		}
%>
		</table>
<%@ include file="footer.jsp"%>
