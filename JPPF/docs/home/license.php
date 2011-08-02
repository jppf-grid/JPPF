<?php $currentPage="License" ?>
<html>
		<head>
		<title>JPPF License
</title>
		<meta name="description" content="An open-source, Java-based, framework for parallel computing.">
		<meta name="keywords" content="JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, gloud, open source">
		<meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
		<link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
		<link rel="stylesheet" type="text/css" href="/jppf.css" title="Style">
	</head>
	<body>
		<div align="center">
		<div class="gwrapper" align="center">
					<?php
		if (!isset($currentPage))
		{
			$currentPage = $_REQUEST["page"];
			if (($currentPage == NULL) || ($currentPage == ""))
			{
				$currentPage = "Home";
			}
		}
		if ($currentPage != "Forums")
		{
		?>
		<div style="background-color: #E2E4F0; margin: 0px;height: 10px"><img src="/images/frame_top.gif"/></div>
		<?php
		}
		?>
		<!--<div class="jppfheader" style="border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">-->
			<table width="100%" cellspacing="0" cellpadding="0" border="0" class="jppfheader" style="border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">
				<tr style="height: 80px">
					<td width="20"></td>
					<td width="400" align="left" valign="center"><a href="/"><img src="/images/logo2.gif" border="0" alt="JPPF"/></a></td>
					<td align="right">
						<table border="0" cellspacing="0" cellpadding="0" style="height: 30px; background-color:transparent;">
							<tr>
								<td style="background: transparent url('/images/buttons/<?php if ($currentPage == "Home") echo "btn_start.gif"; else echo "btn_active_start.gif"; ?>') repeat-x scroll left bottom; width: 9px"></td>
								<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "Home")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/index.php" class="headerMenuItem2">Home</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/index.php" class="headerMenuItem">Home</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "About")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/about.php" class="headerMenuItem2">About</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/about.php" class="headerMenuItem">About</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "Download")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/downloads.php" class="headerMenuItem2">Download</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/downloads.php" class="headerMenuItem">Download</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "Documentation")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/wiki" class="headerMenuItem2">Documentation</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/wiki" class="headerMenuItem">Documentation</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "Forums")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/forums" class="headerMenuItem2">Forums</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/forums" class="headerMenuItem">Forums</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
								<td style="background: transparent url('/images/buttons/<?php if ($currentPage == "Forums") echo "btn_end.gif"; else echo "btn_active_end.gif"; ?>') repeat-x scroll right bottom; width: 9px"></td>
							</tr>
						</table>
					</td>
					<td width="20"></td>
				</tr>
			</table>
		<!--</div>-->
					<table border="0" cellspacing="0" cellpadding="5" width="100%px" style="border: 1px solid #6D78B6; border-top: 8px solid #6D78B6;">
			<tr>
				<td style="background-color: #FFFFFF">
				<div class="sidebar">
																				<?php
											$itemClass = "";
											if ($currentPage == "Home") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/" class="<?php echo $itemClass; ?>">&raquo; Home</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "About") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/about.php" class="<?php echo $itemClass; ?>">&raquo; About</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Download") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/downloads.php" class="<?php echo $itemClass; ?>">&raquo; Download</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Features") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/features.php" class="<?php echo $itemClass; ?>">&raquo; Features</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Documentation") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/wiki" class="<?php echo $itemClass; ?>">&raquo; Documentation</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Patches") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/patches.php" class="<?php echo $itemClass; ?>">&raquo; Patches</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Javadoc") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/api-2.0" class="<?php echo $itemClass; ?>">&raquo; Javadoc</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Samples") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/samples-pack/index.php" class="<?php echo $itemClass; ?>">&raquo; Samples</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "License") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/license.php" class="<?php echo $itemClass; ?>">&raquo; License</a><br>
											</div>
				<hr style="background-color: #6D78B6"/>
															<?php
											$itemClass = "";
											if ($currentPage == "Press") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/release_notes.php?version=2.4" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Quotes") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/quotes.php" class="<?php echo $itemClass; ?>">&raquo; Quotes</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Screenshots") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/screenshots.php" class="<?php echo $itemClass; ?>">&raquo; Screenshots</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "News") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/news.php" class="<?php echo $itemClass; ?>">&raquo; News</a><br>
											</div>
				<hr style="background-color: #6D78B6"/>
															<?php
											$itemClass = "";
											if ($currentPage == "Contacts") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/contacts.php" class="<?php echo $itemClass; ?>">&raquo; Contacts</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Services") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/services.php" class="<?php echo $itemClass; ?>">&raquo; Services</a><br>
											</div>
				<br/>
				</div>
				<div class="jppf_content">
<div align="center">
	<h1>JPPF is released under the Apache 2.0 license</h1>
	<p>Apache License<br>
	Version 2.0, January 2004<br>
	<a href="http://www.apache.org/licenses/">http://www.apache.org/licenses/</a>
</div>
<div align="justify">
	<p>TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION
	<p><b>1. Definitions.</b>
		<p>"License" shall mean the terms and conditions for use, reproduction, 
		and distribution as defined by Sections 1 through 9 of this document.
		<p>"Licensor" shall mean the copyright owner or entity authorized by
		the copyright owner that is granting the License.
		<p>"Legal Entity" shall mean the union of the acting entity and all
		other entities that control, are controlled by, or are under common
		control with that entity. For the purposes of this definition,
		"control" means (i) the power, direct or indirect, to cause the
		direction or management of such entity, whether by contract or
		otherwise, or (ii) ownership of fifty percent (50%) or more of the
		outstanding shares, or (iii) beneficial ownership of such entity.
		<p>"You" (or "Your") shall mean an individual or Legal Entity
		exercising permissions granted by this License.
		<p>"Source" form shall mean the preferred form for making modifications,
		including but not limited to software source code, documentation
		source, and configuration files.
		<p>"Object" form shall mean any form resulting from mechanical
		transformation or translation of a Source form, including but
		not limited to compiled object code, generated documentation,
		and conversions to other media types.
		<p>"Work" shall mean the work of authorship, whether in Source or
		Object form, made available under the License, as indicated by a
		copyright notice that is included in or attached to the work
		(an example is provided in the Appendix below).
		<p>"Derivative Works" shall mean any work, whether in Source or Object
		form, that is based on (or derived from) the Work and for which the
		editorial revisions, annotations, elaborations, or other modifications
		represent, as a whole, an original work of authorship. For the purposes
		of this License, Derivative Works shall not include works that remain
		separable from, or merely link (or bind by name) to the interfaces of,
		the Work and Derivative Works thereof.
		<p>"Contribution" shall mean any work of authorship, including
		the original version of the Work and any modifications or additions
		to that Work or Derivative Works thereof, that is intentionally
		submitted to Licensor for inclusion in the Work by the copyright owner
		or by an individual or Legal Entity authorized to submit on behalf of
		the copyright owner. For the purposes of this definition, "submitted"
		means any form of electronic, verbal, or written communication sent
		to the Licensor or its representatives, including but not limited to
		communication on electronic mailing lists, source code control systems,
		and issue tracking systems that are managed by, or on behalf of, the
		Licensor for the purpose of discussing and improving the Work, but
		excluding communication that is conspicuously marked or otherwise
		designated in writing by the copyright owner as "Not a Contribution."
		<p>"Contributor" shall mean Licensor and any individual or Legal Entity
		on behalf of whom a Contribution has been received by Licensor and
		subsequently incorporated within the Work.
	<p><b>2. Grant of Copyright License.</b> Subject to the terms and conditions of
		this License, each Contributor hereby grants to You a perpetual,
		worldwide, non-exclusive, no-charge, royalty-free, irrevocable
		copyright license to reproduce, prepare Derivative Works of,
		publicly display, publicly perform, sublicense, and distribute the
		Work and such Derivative Works in Source or Object form.
	<p><b>3. Grant of Patent License.</b> Subject to the terms and conditions of
		this License, each Contributor hereby grants to You a perpetual,
		worldwide, non-exclusive, no-charge, royalty-free, irrevocable
		(except as stated in this section) patent license to make, have made,
		use, offer to sell, sell, import, and otherwise transfer the Work,
		where such license applies only to those patent claims licensable
		by such Contributor that are necessarily infringed by their
		Contribution(s) alone or by combination of their Contribution(s)
		with the Work to which such Contribution(s) was submitted. If You
		institute patent litigation against any entity (including a
		cross-claim or counterclaim in a lawsuit) alleging that the Work
		or a Contribution incorporated within the Work constitutes direct
		or contributory patent infringement, then any patent licenses
		granted to You under this License for that Work shall terminate
		as of the date such litigation is filed.
	<p><b>4. Redistribution.</b> You may reproduce and distribute copies of the
		Work or Derivative Works thereof in any medium, with or without
		modifications, and in Source or Object form, provided that You
		meet the following conditions:
		<p>(a) You must give any other recipients of the Work or
				Derivative Works a copy of this License; and
		<p>(b) You must cause any modified files to carry prominent notices
				stating that You changed the files; and
		<p>(c) You must retain, in the Source form of any Derivative Works
				that You distribute, all copyright, patent, trademark, and
				attribution notices from the Source form of the Work,
				excluding those notices that do not pertain to any part of
				the Derivative Works; and
		<p>(d) If the Work includes a "NOTICE" text file as part of its
				distribution, then any Derivative Works that You distribute must
				include a readable copy of the attribution notices contained
				within such NOTICE file, excluding those notices that do not
				pertain to any part of the Derivative Works, in at least one
				of the following places: within a NOTICE text file distributed
				as part of the Derivative Works; within the Source form or
				documentation, if provided along with the Derivative Works; or,
				within a display generated by the Derivative Works, if and
				wherever such third-party notices normally appear. The contents
				of the NOTICE file are for informational purposes only and
				do not modify the License. You may add Your own attribution
				notices within Derivative Works that You distribute, alongside
				or as an addendum to the NOTICE text from the Work, provided
				that such additional attribution notices cannot be construed
				as modifying the License.
		<p>You may add Your own copyright statement to Your modifications and
		may provide additional or different license terms and conditions
		for use, reproduction, or distribution of Your modifications, or
		for any such Derivative Works as a whole, provided Your use,
		reproduction, and distribution of the Work otherwise complies with
		the conditions stated in this License.
	<p><b>5. Submission of Contributions.</b> Unless You explicitly state otherwise,
		any Contribution intentionally submitted for inclusion in the Work
		by You to the Licensor shall be under the terms and conditions of
		this License, without any additional terms or conditions.
		Notwithstanding the above, nothing herein shall supersede or modify
		the terms of any separate license agreement you may have executed
		with Licensor regarding such Contributions.
	<p><b>6. Trademarks.</b> This License does not grant permission to use the trade
		names, trademarks, service marks, or product names of the Licensor,
		except as required for reasonable and customary use in describing the
		origin of the Work and reproducing the content of the NOTICE file.
	<p><b>7. Disclaimer of Warranty.</b> Unless required by applicable law or
		agreed to in writing, Licensor provides the Work (and each
		Contributor provides its Contributions) on an "AS IS" BASIS,
		WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
		implied, including, without limitation, any warranties or conditions
		of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
		PARTICULAR PURPOSE. You are solely responsible for determining the
		appropriateness of using or redistributing the Work and assume any
		risks associated with Your exercise of permissions under this License.
	<p><b>8. Limitation of Liability.</b> In no event and under no legal theory,
		whether in tort (including negligence), contract, or otherwise,
		unless required by applicable law (such as deliberate and grossly
		negligent acts) or agreed to in writing, shall any Contributor be
		liable to You for damages, including any direct, indirect, special,
		incidental, or consequential damages of any character arising as a
		result of this License or out of the use or inability to use the
		Work (including but not limited to damages for loss of goodwill,
		work stoppage, computer failure or malfunction, or any and all
		other commercial damages or losses), even if such Contributor
		has been advised of the possibility of such damages.
	<p><b>9. Accepting Warranty or Additional Liability.</b> While redistributing
		the Work or Derivative Works thereof, You may choose to offer,
		and charge a fee for, acceptance of support, warranty, indemnity,
		or other liability obligations and/or rights consistent with this
		License. However, in accepting such obligations, You may act only
		on Your own behalf and on Your sole responsibility, not on behalf
		of any other Contributor, and only if You agree to indemnify,
		defend, and hold each Contributor harmless for any liability
		incurred by, or claims asserted against, such Contributor by reason
		of your accepting any such warranty or additional liability.
	<p>END OF TERMS AND CONDITIONS
	<p>APPENDIX: How to apply the Apache License to your work.
		<p>To apply the Apache License to your work, attach the following
		boilerplate notice, with the fields enclosed by brackets "[]"
		replaced with your own identifying information. (Don't include
		the brackets!)  The text should be enclosed in the appropriate
		comment syntax for the file format. We also recommend that a
		file or class name and description of purpose be included on the
		same "printed page" as the copyright notice for easier
		identification within third-party archives.
	<p>Copyright [yyyy] [name of copyright owner]
	<p>Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
		 <p>http://www.apache.org/licenses/LICENSE-2.0
	<p>Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
</div>
<br>
				</div>
									</td>
				</tr>
			</table>
				<!--<div align="center" style="width: 100%; border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">-->
		<table border="0" cellspacing="0" cellpadding="0" width="100%" class="jppffooter">
			<tr><td colspan="*" style="height: 10px"></td></tr>
			<tr>
				<td align="center" style="font-size: 9pt; color: #6D78B6">
					<a href="http://sourceforge.net/donate/index.php?group_id=135654"><img src="http://images.sourceforge.net/images/project-support.jpg" width="88" height="32" border="0" alt="Support This Project" /></a>
				</td>
				<td align="center" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2011 JPPF.org</td>
				<td align="right">
					<a href="http://www.parallel-matters.com"><img src="/images/pm_logo_tiny.jpg" border="0" alt="Powered by Parallel Matters" /></a>&nbsp;
					<a href="http://sourceforge.net/projects/jppf-project">
						<img src="http://sflogo.sourceforge.net/sflogo.php?group_id=135654&type=10" width="80" height="15" border="0"
							alt="Get JPPF at SourceForge.net. Fast, secure and Free Open Source software downloads"/>
					</a>
				</td>
				<td style="width: 10px"></td>
			</tr>
			<tr><td colspan="*" style="height: 10px"></td></tr>
		</table>
	<!--</div>-->
	<div style="background-color: #E2E4F0; width: 100%;"><img src="/images/frame_bottom.gif" border="0"/></div>
		</div>
		</div>
	</body>
</html>
