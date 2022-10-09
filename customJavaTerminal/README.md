Custom terminal built in Java.
Can be ran using the included gradle file. First, run "gradle build" and 
then "java -jar Terminal.jar" from the build/libs folder.

Commands include:
ptime - Current time spent in child processes
list - More comprehensive version of ls
mdir - Make a directory
rdir - Remove an empty directory
cd X - cd to a different directory where X = .. or a folder
exit - exits the program
Other commands will be fed to the computer to process internally.
Piping is supported.