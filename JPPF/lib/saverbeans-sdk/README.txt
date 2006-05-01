-------------------------------------------------------------------------------
SaverBeans SDK License:
-------------------------------------------------------------------------------
Copyright (c) 2004-2005 Sun Microsystems, Inc. All rights reserved. Use is
subject to license terms.

This program is free software; you can redistribute it and/or modify
it under the terms of the Lesser GNU General Public License as
published by the Free Software Foundation; either version 2 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA
-------------------------------------------------------------------------------

-------------------------------------------------------------------------------
JOGL License:
-------------------------------------------------------------------------------
Distributed with this software is a copy of JOGL, a library that provides
Java bindings for OpenGL.  Usage of JOGL is covered under the following
license terms:

JOGL Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistribution of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.
- Redistribution in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

Neither the name of Sun Microsystems, Inc. or the names of
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

This software is provided "AS IS," without a warranty of any kind
ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, 
INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

You acknowledge that this software is not designed or intended for use
in the design, construction, operation or maintenance of any nuclear
facility.

Sun gratefully acknowledges that this software was originally authored
and developed by Kenneth Bradley Russell and Christopher John Kline.
-------------------------------------------------------------------------------


SaverBeans SDK README
---------------------

The SaverBeans SDK is a Java screensaver development kit, enabling developers
to create cross-platform screensavers.  The developer writes a set of Java
classes along with an XML description of the screensaver settings, and uses
the tools in this development kit to produce a screensaver for any 
Java-supported OS.  The resulting screensavers behave just like a native
screensaver (i.e. with preview capabilities and control over settings).


Screensaver SDK Components
--------------------------

The SaverBeans SDK contains the following components:

    * SaverBeans Ant Task JAR - A JAR required by screensaver
      projects, used to produce native screensavers.
    * SaverBeans API JAR - These are redistributable files to be
      included with your generated screensavers.
    * SaverBeans API Javadocs - Javadocs for the SaverBeans APIs.
    * Screensaver Startup Kit - Sample project including a build file, 
      skeleton screensaver and configuration file.
    * Screensaver JOGL Startup Kit - Sample project for jogl (OpenGL) 
      screensavers including a build file, skeleton screensaver and 
      configuration file.


SaverBeans Screensaver Pack
---------------------------

The SaverBeans SDK does not itself contain any screensavers.  It is
only a development kit for producing screensavers.

A collection of open source Java screensavers contributed by the
Java community is available in the SaverBeans Screensaver Pack, which
is hosted in the screensavers project on java.net:

    http://screensavers.dev.java.net/


Design and Implementation Details
---------------------------------

A native layer has been developed for each target operating system so 
that the screensaver plugs in naturally into the existing OS screensaver
framework.
   
For Linux and Solaris, the xscreensaver framework is used.  For Windows,
a native .scr is produced.  Support for other operating systems is
possible and contributions are welcome.  The screensavers themselves 
are written in Java, depending only on Java APIs, and are completely
cross-platform.

An XEmbeddedFrame (for JDK 1.5) is attached to the native window and
the native code ensures the embedded frame is always the right size
and location.  A ScreensaverContext is populated with a reference to
the component, which is passed to the screensaver.  The screensaver 
writes directly to the embedded frame.  To the user, it appears as 
though the code is writing directly to the native window.

Java screensavers extend SimpleScreensaver and implement init() and
paint() methods.  The paint method is called automatically on a
regular basis to render the next frame of the animation.  Future
screensavers may extend an alternate base class that will allow the
screensaver to be able to control its own timing.  This has not yet
been implemented.  Again, contributions are most welcome.

To specify which options a screensaver accepts, an xscreensaver-compatible
.xml file is created with the settings.  When the screensaver is to be
built, a code generator reads the .xml file and produces supplementary 
code to allow a native wrapper to be produced.  For xscreensaver platforms,
the supplementary code consists of simple configuration information so
that screenhack can validate commandline parameters.  For Windows, code
is automatically generated for a dialog box that enables settings to 
be tweaked.


Building the SDK
----------------
To build the SDK from source, your system must meet the following
requirements:

    * JDK 5.0 or greater, available from http://java.sun.com/
    * Ant 1.5.1 or greater, available from http://ant.apache.org/

To build the SDK:

    1. Copy build.properties.sample to build.properties.
    2. Edit build.properties and follow the instructions in that file
       to configure for your environment.
    3. Run 'ant dist' to build the SDK.

To rebuild the Win32 Native Layer:

    To rebuild the Win32 native layer (optional), you must have a
    licensed copy of Visual C++ 5.0 with Service Pack 3 installed.
    (Without SP3 you'll get a strange error message with the linker).
    Execute VCVARS32.bat in the command prompt you use to build.  You may
    need to tweak some environment settings to get everything building.

    You can get Serivce Pack 3 here:
      http://msdn.microsoft.com/vstudio/downloads/updates/sp/vs97/default.aspx
    Make sure to get the FULL pack.

    The binary is checked into the CVS repository so unless you change
    any files in the src/ant-native/win32 directory, you will not need this.
    If you do need to rebuild it, the relevant ant target is ant-native-win32.

Building the Screensavers
-------------------------
To build screensavers developed using this kit, see the requirements
outlined in the Screensaver Startup Kit.


SaverBeans Ant Task JAR
-----------------------
The SaverBeans Ant Task JAR contains an ant task used by
screensaver projects to generate the supporting code for your
target platform.

The ForEachScreensaver ant task reads in a screensaver
configuration file and automatically generates supporting
code for your target platform.

For an example of how this task is used, see the startup kit.

To define the <foreachscreensaver> ant task, use the taskdef ant task
as follows:

    <taskdef name="foreachscreensaver" 
        classname="org.jdesktop.jdic.screensaver.autogen.ForEachScreensaver"
        classpath="${saverbeans-ant.jar.path}" />

Where ${saverbeans-ant.jar.path} is the full path to the generated
saverbeans-ant.jar.

Once the task has been defined, add a target for each desired platform, and
invoke the <foreachscreensaver> in each target as follows:

    <foreachscreensaver confDir="${src.conf}"
                        outDir="${build}/gensrc"
                        buildDir="${build}/linux"
                        os="unix">
      <!-- Build steps for Unix -->
    </foreachscreensaver>

Where:

    confDir is the directory containing a set of screensaver XML 
        configuration files, one for each screensaver.
    outDir is a directory that the foreachscreensaver task can use to 
        generate temporary intermediate files.
    buildDir is a directory that final .class files should be 
        generated to
    os is the target operating system.  Current possible values are:
        unix - Use this value for Unix operating systems such as 
               Solaris and Linux.
        win32 - Use this value for Windows operating systems such
               as Windows 98 or XP.

In the body of the <foreachscreensaver> task, place the steps needed
to build the screensaver for that particular OS.  The body is repeated
once for each screensaver configuration file in confDir.

The Screensaver startup kit contains a build.xml with the necessary
steps and configuration to build on both Linux and Windows.


SaverBeans API JAR
------------------
This component of the SaverBeans SDK is a redistributable JAR
that is distributed with your screensaver.  A single copy of this
JAR must be installed on the user's system in order for them to be
able to run the screensaver.


SaverBeans API Javadocs
-----------------------
This component contains the generated javadocs for the SaverBeans
API.  These javadocs are used as a reference by screensaver
developers.


Screensaver Startup Kit and JOGL (OpenGL) Startup Kit
-----------------------------------------------------
This component is a skeleton screensaver project startup kit,
containing a build.xml, simple screensaver and a configuration file.
It allows screensaver developers to get a quick start and provides a 
consistent look and feel between screensaver projects developed using 
the SaverBeans SDK.

The jogl version includes all the code you need to get started writing
JOGL screensavers in Java.

To create your own screensaver, simply unpack the startup kit
to a new directory and start tweaking!

Use the ant 'debug' target during development to preview your screensaver
in a ScreensaverFrame.  When done, use the ant 'dist' target to produce
your final screensaver bundles for each supported platform.

When you're done, be sure to visit the http://screensavers.dev.java.net/
project to share your work with the community!


Support
-------
Support for SaverBeans is provided by the Java community on the
JDIC and screensavers projects on java.net.  There are various
forums and mailing lists available.

    * http://jdic.dev.java.net/
    * http://screensavers.dev.java.net/


Revision History
----------------
Version 0.2 - June 22, 2005
    * Added JOGL (OpenGL) Screensaver Startup Kit and corresponding APIs
      This allows developers to write OpenGL 3D screensavers in Java!
    * Added installer generation for Windows screensavers (generates a
      Nullsoft installer script).
    * Added a destroy() method to the ScreensaverBase API so that
      screensavers can release resources before shutting down.
    * Screensaver now exits on Windows when cursor moves.
    * Added isFullscreen() API on context object.
    * Mouse cursor is no longer hidden from preview in Windows.
    * Multi-monitor support.
    * Fixed bug in settings dialog when values are out of bounds.
    * Fixed slow-downs on Linux for some screensavers that use alpha.
    * Upped minimum JDK requirement to 5.0.

Version 0.1.1 - June 10, 2004
    * Fixed screensavers.dev.java.net issue #1:
      Fatal Exception if *.scr and *.jar not in system32 directory
    * Fixed jdic.dev.java.net issue #20:
      ScreensaverFrame for developers
      - Added ScreensaverFrame and ant debug target to make it easier to
        debug screensavers.
    * Fixed jdic.dev.java.net issue #21:
      Screensaver exceptions not being caught in init() method
    * Improved Unix Makefile to give error message when dirs don't exist
    * Building on Windows now produces correct newlines for unix distribution

Version 0.1 - June 1, 2004
    * Initial open-source release of SaverBeans SDK

