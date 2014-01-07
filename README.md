FRCClockSystem
==============
_A UUID based quick clock-in and clock-out system that shows a member's photo, originally designed for FRC Team 1983, [Skunkworks Robotics](http://ahsrobotics.us/)._
Database Setup
--------
1. The Database Folder  
The database folder is located at "./data", or the data folder inside your current directory.
2. The Members Database  
The members database is located at "./data/members.csv", and takes the form of comma seperated values.  Each line is formatted as follows:
`UUID,Type,Name,Group(s),Image,BadgeID` where...  
`UUID` is the integer-value identifier for that person.  
`Type` is the user type. (See Types)  
`Name` is the name of that person.  
`Group(s)` is a space seperated list of that member's groups. (See Groups)  
`Image` is the name of that students image file.  
`BadgeID` is that student's alternate, or badge ID.
3. Image Storage  
Generated images are stored in the "./data/mugs" folder.  These images are already resized to the optimal size and have a nametag attached.  Un-processed images are stored in the "./data/mugs_large" folder, and can be processed running the command "java -cp classpath com.skunk.clock.CreateMugs" from a shell inside "./"

Information
--------
* **Types**  
There are three user types, Student, Mentor, and Coach.  
A _Student_ is a user that will show up on the larger left side of the window, seperated into sections by their first specified group.  
A _Mentor_ is a user that will appear on the right side of the window.  
A _Coach_ is identical to a mentor with the addition that when clocking out he or she will also clock out all remaining users with one hour of clock time, and save the clock time database for that day.
An _Admin_ will not record any time in the database, but has the ability to clock in other members a certain number of hours in the past.
* **Groups**  
Groups are one of Systems, Outreach, Data, Programming, Build, Marketing, Electrical, Mentor, Design, Saftey, SubLead, or Lead.  They are responsible for determining the grouping of students in the student panel.  Some users may have more than one group, (e.g. a programming lead may be in both the programming and lead groups), and should have their group stored first by their primary group, then any other groups seperated by spaces in the database.  (e.g. Said programmer groups value in the database would be "Programming Lead")  
* **Web Interface**
The web interface is a way to visualize the information saved by the clock system as a general set.  This can then be released to the team as a whole as a way of keeping up to date on their current number of hours logged.
* **Autonomous Operation**
The clock system is designed to work with very little maintenance.  Because of this the system will automatically clock all remaining users out, giving them all one hour, and save the database if another day rolls around.  This system theoretically allows the system to simply be left on.
* **Special Number Combinations**
If these codes are entered into the system as if someone was clocking in they will preform a certain set of predefined actions.
  * 999999 forces a complete refresh of the displayed users.  This will not change the database in any way, it will simply fix any graphical glitches and force the system to show users that for some reason got messed up.
