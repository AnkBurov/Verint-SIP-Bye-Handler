# Verint SIP Bye Handler #
* Successfully solves the problem between Cisco UCM and recording server when CUCM recording calls stuck in preserved mode until SIP Trunk reboot. Details of the bug are here: https://tools.cisco.com/bugsearch/bug/CSCuv29131
The application itself works by parsing log files for call-id strings and closing found calls by sending SIP Bye requests. More information how exactly this application works is in detailed comments in source code files. 
* Version 1.0.

## Requirements ##
* Java Runtime Environment 1.8.
* Network access to Verint cluster or any other recorder with similar SIP BYE problem.
* Can work on any OS with JVM (Windows, Linux, Mac OS, toaster, etc).

## Installation ##
* Install latest JRE from http://java.com. If you install this application on a server which already has any applications using Java, then you should instal JRE to custom directory, not in default, to avoid any possible issues with backward compatibility.
* Download latest release of Verint SIP Bye Handler and extract zip archive in needed place.
* Configure settings in etc/config.properties. You should at least specify SIP number of the recorder, risLogFolderLocation and callTerminationTimeout. See detailed comments about each property in the etc/config.properties.
* Setup new rule in Task Scheduler (Windows) or Cron (*nix) with trigger every 5 minutes (or more) and executing (%path_to_JRE_folder%/bin/javaw -Dlog4j.configurationFile=etc/log4j2.xml -jar verintbyehandler.jar and working directory (Start in) as path to folder with extracted Verint SIP Bye Handler.
* * In case of installing on Linux - just write simple script: cd <Verint SIP Bye Handler path>	and mentioned above java command, and execute it in Cron.
* Run created task and check /etc/application.log for any errors or exceptions.

## Release notes ##
* 1.00 Release.