@echo off

rem this script runs the built-in JPPF screen saver in standalone mode (the node is not started)
rem this is provided as a convenience for testing (or other) purposes

cd %~dp0
rem to avoid the DOS command-prompt in windowed mode, use "start javaw ..."
rem do not use 'start' if configured as a real Windows screensaver, using a utility such as
rem Run Saver (http://www.donationcoder.com/Software/Skrommel/index.html#RunSaver) or
rem Screen Launcher (http://www.bartdart.com/), as the screensaver will not be terminated when the computer resumes activity    
javaw -cp config;lib/* -Xmx64m -Dlog4j.configuration=log4j-node.properties -Djppf.config=jppf-node.properties -Djava.util.logging.config.file=config/logging-node.properties org.jppf.node.screensaver.ScreenSaverMain
