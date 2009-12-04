<%@ page language="java" %>
<html>
	<head>
		<title>JPPF Tomcar Integration Demo</title>
		<meta name="description" content="An open-source, Java-based, framework for parallel computing.">
		<meta name="keywords" content="JPPF, Java, Parallel Computing, Distributed Computing, Grid Computing, Cluster, Grid">
		<meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
		<link rel="stylesheet" type="text/css" href="./jppf.css" title="Style">
	</head>
	<body>
		<div align="center">
		<table width="80%" cellspacing="0" cellpadding="0"
			style="background: url('images/header_new.jpg'); background-repeat: no-repeat; background-attachment: scroll; background-position: left top">
			<tr><td>
				<table width="100%" cellspacing="0" cellpadding="5">
					<tr>
						<td width="70%" align="right" valign="center">
							<h1>JPPF</h1>
						</td>
						<td width="30%" align="center">
							<img src="images/logo1.gif" border="0" alt="JPPF"/>
						</td>
					</tr>
				</table>
			</td></tr>
			<tr><td>
				<table border="0" cellspacing="5" cellpadding="0" style="position: relative; top: 20px">
					<tr>
						<td>
							<table border="0" cellspacing="0" cellpadding="0" width="100%">
								<tr>
									<td width="10" height="10" style="background-image: url(images/buttons/nw.gif); background-repeat: no-repeat; background-position: 100% 100%"/>
									<td width="10" height="10" colspan="${span}" style="background-image: url(images/buttons/filler.gif); background-repeat: repeat-x; background-position: 0% 0%"/>
									<td width="10" height="10" style="background-image: url(images/buttons/ne.gif); background-repeat: no-repeat; background-position: 0% 100%"/>
								</tr>
								<tr>
									<td width="10" style="background-image: url(images/buttons/filler.gif); background-repeat: repeat-y; background-position: 0% 0%"/>
									<td colspan="${span}" style="background-image: url(images/buttons/filler.gif); background-repeat: repeat; background-position: 0% 0%">
										&nbsp;<a href="index.jsp">Submit</a>&nbsp;
									</td>
									<td width="10" style="background-image: url(images/buttons/right.gif); background-repeat: repeat-y; background-position: 0% 0%"/>
								</tr>
								<tr>
									<td width="10" height="10" style="background-image: url(images/buttons/sw.gif); background-repeat: no-repeat; background-position: 100% 0%"/>
									<td width="10" height="10" colspan="${span}" style="background-image: url(images/buttons/bottom.gif); background-repeat: repeat-x; background-position: 0% 0%"/>
									<td width="10" height="10" style="background-image: url(images/buttons/se.gif); background-repeat: no-repeat; background-position: 0% 0%"/>
								</tr>
							</table>
						</td>
						<td>
							<table border="0" cellspacing="0" cellpadding="0" width="100%">
								<tr>
									<td width="10" height="10" style="background-image: url(images/buttons/nw.gif); background-repeat: no-repeat; background-position: 100% 100%"/>
									<td width="10" height="10" colspan="${span}" style="background-image: url(images/buttons/filler.gif); background-repeat: repeat-x; background-position: 0% 0%"/>
									<td width="10" height="10" style="background-image: url(images/buttons/ne.gif); background-repeat: no-repeat; background-position: 0% 100%"/>
								</tr>
								<tr>
									<td width="10" style="background-image: url(images/buttons/filler.gif); background-repeat: repeat-y; background-position: 0% 0%"/>
									<td colspan="${span}" style="background-image: url(images/buttons/filler.gif); background-repeat: repeat; background-position: 0% 0%">
											&nbsp;<a href="results.jsp">Results</a>&nbsp;
									</td>
									<td width="10" style="background-image: url(images/buttons/right.gif); background-repeat: repeat-y; background-position: 0% 0%"/>
								</tr>
								<tr>
									<td width="10" height="10" style="background-image: url(images/buttons/sw.gif); background-repeat: no-repeat; background-position: 100% 0%"/>
									<td width="10" height="10" colspan="${span}" style="background-image: url(images/buttons/bottom.gif); background-repeat: repeat-x; background-position: 0% 0%"/>
									<td width="10" height="10" style="background-image: url(images/buttons/se.gif); background-repeat: no-repeat; background-position: 0% 0%"/>
								</tr>
							</table>
						</td>
						<td>
							<table border="0" cellspacing="0" cellpadding="0" width="100%">
								<tr>
									<td width="10" height="10" style="background-image: url(images/buttons/nw.gif); background-repeat: no-repeat; background-position: 100% 100%"/>
									<td width="10" height="10" colspan="${span}" style="background-image: url(images/buttons/filler.gif); background-repeat: repeat-x; background-position: 0% 0%"/>
									<td width="10" height="10" style="background-image: url(images/buttons/ne.gif); background-repeat: no-repeat; background-position: 0% 100%"/>
								</tr>
								<tr>
									<td width="10" style="background-image: url(images/buttons/filler.gif); background-repeat: repeat-y; background-position: 0% 0%"/>
									<td colspan="${span}" style="background-image: url(images/buttons/filler.gif); background-repeat: repeat; background-position: 0% 0%">
											&nbsp;<a href="about.jsp">About</a>&nbsp;
									</td>
									<td width="10" style="background-image: url(images/buttons/right.gif); background-repeat: repeat-y; background-position: 0% 0%"/>
								</tr>
								<tr>
									<td width="10" height="10" style="background-image: url(images/buttons/sw.gif); background-repeat: no-repeat; background-position: 100% 0%"/>
									<td width="10" height="10" colspan="${span}" style="background-image: url(images/buttons/bottom.gif); background-repeat: repeat-x; background-position: 0% 0%"/>
									<td width="10" height="10" style="background-image: url(images/buttons/se.gif); background-repeat: no-repeat; background-position: 0% 0%"/>
								</tr>
							</table>
						</td>
					</tr>
				</table>
			</td></tr>
		</table>
	