FRCClockSystem
==============
_A UUID based quick clock-in and clock-out system that shows a member's photo._
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
Generated images are stored in the "./data/mugs" folder.  These images are already resized to the optimal size and have a nametag attached.  Un-processed images are stored in the "./data/mugs_large" folder, and can be processed running the "create_mugs" script, or the class "com.skunk.clock.CreateMugs".

Information
--------
* **Types**  
There are three user types, Student, Mentor, and Coach.  
A _Student_ is a user that will show up on the larger left side of the window, seperated into sections by their first specified group.  
A _Mentor_ is a user that will appear on the right side of the window.  
A _Coach_ is identical to a mentor with the addition that when clocking out he or she will also clock out all remaining users with one hour of clock time, and save the clock time database for that day.
* **Groups**  
Groups are one of Systems, Programming, Build, Marketing, Electrical, Mentor, Design, Saftey, or Lead.  They are responsible for determining the grouping of students in the student panel.  Some users may have more than one group, (e.g. a programming lead may be in both the programming and lead groups), and should have their group stored first by their primary group, then any other groups seperated by spaces in the database.  (e.g. Said programmer groups value in the database would be "Programming Lead")  
