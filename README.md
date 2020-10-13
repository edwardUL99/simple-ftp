# simple-ftp
A simple Java FTP-Client program

A side project to practise my Java skills and to help improve my GUI skills.
The goal is to implement a GUI drag and drop FTP application
This may progress slowly

# Configuration properties
-Dsimpleftp.passwordEncryptFile=<filename> allows you to write on one line a string of characters which will be used as a "seed" for password encryption. Note that if this file changes, old passwords in files will not be able to be decrypted. So may be best to have the same constant file added to the classpath in front of the jar so the program will find the file before the default encryption file inside in the jar. i.e password.encrypt:<jar-name>:$CLASSPATH, password.encrypt is inside the jar but since password.encrypt is before the jar it will be found first 
