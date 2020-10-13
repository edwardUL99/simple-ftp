# simple-ftp
A simple Java FTP-Client program

A side project to practise my Java skills and to help improve my GUI skills.
The goal is to implement a GUI drag and drop FTP application
This may progress slowly

# Configuration properties
-Dsimpleftp.passwordEncryptFile=<filename> allows you to write on one line a string of characters which will be used as a "seed" for password encryption. Note that if this file changes, old passwords in files will not be able to be decrypted. The password.encrypt file will need to be provided by either adding the directory containing the file (file name not in path) to the classpath or with the system property.

e.g. classpath
You have a directory called simpleftp/resources and inside in the resources you have the password.encrypt file. Add the following to classpath CLASSPATH=jar-name:simpleftp/resources

Or you could add the file to the final built jar by running the following command jar uf jar-file password.encrypt
(the password.encrypt needs to be in the current directory where you are running the command from

If it cannot find the file in any of these, it attempts to find it in the directory returned by the system property "user.dir"
