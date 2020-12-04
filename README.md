# simple-ftp
A simple Java FTP-Client program

A side project to practise my Java skills and to help improve my GUI skills.
The goal is to implement a GUI drag and drop FTP application
This may progress slowly.

## Features 
The application is to have a small collection of features for working with local and remote FTP Server.
The features listed here are features that will be implemented as the initial working version. They are **must haves**.
Features that are nice to have, see below.

- Browse files on both the local file system and the remote FTP System
    - This is done using a panel to list the files in the current directory on that File System
    - Works much like a shell terminal, you operate within it in a directory, this is the panel's current directory
- Open simple text files in a very basic editor
    - Opening certain files causes the program to crash. Therefore, the editor is extremely experimental.
    If the program crashes upon opening a file, open a bug report outlining the file extension and type (don't have to include contents).
    This will be a type that needs to be added to the list of files that cannot be opened.
- Allow saving of these edited files
- Absolute and relative paths can be specified in any dialog requesting a path. The PathResolver interface provides this resolving of paths, where each implementation can define how the path is resolved. In most cases, this means to convert it to an absolute, canonicalized path (i.e. no . or .. characters and all symbolic links expanded to target path)
- Creation of new files/directories on both local and remote server
- Deleting files and empty directories
- Viewing of basic file properties such as modification time, size, permissions etc.
- Applying a mask to the list of current files to filter them using wildcards like * and ?
- Navigating by using the file panels or entering a specific path using the Go To button
- Support for symbolic links (**limited support, see the entry on Symbolic Links here**)
- Support for showing/hiding hidden files (if supported on local file system, remote file system, filename just needs to start with a dot character)
- Renaming files (this can also be used to move files)
- Copying/moving/deleting files (non-recursive and recursive)
- Saving/Deleting sessions (the server that was logged in, last working directory etc.)
- Login panel to the server 
- Drag and drop copying of files and folders as well as dialog options to do the same

*This list may change as development proceeds, these features are for release 1.0*

## Pre-requisites
You need the following to run the application:
- For runtime (no development):
    - Java JRE 1.8.0 minimum
- For development:
    - Maven 3.6.3 if you don't want to use wrapper
    - Java JDK 1.8.0 minimum (all other dependencies resolved automatically by Maven)

## How to build
1. In a directory where you want to download the project, call git clone using the clone url (or download the zip and extract 
to a specified directory)
2. Using system-installed maven, call mvn or the maven wrapper for your OS
3. Called mvn (or ./mvnw) clean install
4. The built jar will be in the target folder

## How to install

*Note, I hope to generate a build script in the future to randomly generate the password.encrypt file on installation and abstract these operations to the end-user*

1. You need the password.encrypt file on the classpath containing a 20 character key for encrypting passwords.
2. It is best to have this file inside in the Jar, or else run export (set on windows) CLASSPATH=<folder containing password.encrypt (not the file name)>:$CLASSPATH(%CLASSPATH% on windows)
3. You can specify a system property to provide a different name or specific path to the file (see Configuration properties)
4. Create a text file called password.encrypt.
5. Edit the file and add in a string of 20 random characters.
6. In the same directory where the jar is located, move the password.encrypt file to it
7. Run the command jar -uf <name of simpleftp jar> password.encrypt.
Example is jar -uf simple-ftp-1.0-SNAPSHOT.jar password.encrypt. (1.0 may be a different version number)
8. Run jar -tvf <name of simpleftp jar> and verify that a file called password.encrypt is in the root of the jar
9. Delete all copies of password.encrypt that are not inside in the JAR (if not having it inside the jar, ensure there is only one copy in the location on the classpath or specified by the property)

#### jar command not found
If the jar command could not be found, you have a JRE installed instead of JDK, you can use the zip command (as a JAR file is just a ZIP).

For adding the file, replace the jar -uf command with the zip -u command with the same order of arguments.
To list the files, use unzip -l <jar name> command and see if the file is included.
All other steps still stand (e.g. password.encrypt must be in the same directory etc.)

## How to run
The project doesn't have a main class to run at the moment committed to the repository.
At present, it is just a local testing main program which cannot be committed as it would expose passwords.

Check back here closer/after release 1.0 for run-instructions

## Configuration properties
If the file is not dded to the jar, you need to add the file's location to the CLASSPATH.
For example, if a folder called resources contains the password.encrypt file, you want to add the absolute path to the resources folder to the classpath.
Don't add the file in the path on the classpath. If the path it /path/to/resources/password.encrypt, you will want to add /path/to/resources to the CLASSPATH.

If the name of the file is different to password.encrypt, the name of the file needs to be specified by the -Dsimpleftp.passwordEncryptFile=<filename> property.

You can also specify the full path to the file here, e.g. -Dsimpleftp.passwordEncryptFile=/path/to/resources/password.encrypt

e.g. classpath
You have a directory called simpleftp/resources and inside in the resources you have the password.encrypt file. Add the following to classpath CLASSPATH=jar-name:simpleftp/resources

If it cannot find the file in any of these, it attempts to find it in the directory returned by the system property "user.dir".

**WARNING:** If this file changes, any existing encrypted passwords in files will not work anymore.

You can enable debugging information by running with the property -Dsimpleftp.debug

## Feature Ideas for the future
These are the "nice-to-haves" as described above.

The current system uses a ftp package for dealing with connection to the FTP Server. The structure of this is, at the moment,
too rigid to make the project extensible to include other file transfer protocols like SFTP.

The idea for a future implementation is to make a connection package, which provides interfaces for connection to servers with different protocols.
This would provide a Connection interface, which all different connection protocols having an implementing connection class.

The current structure/architecture would have to be greatly refactored to move it away from being strictly FTP.
This is especially in the filesystem.RemoteFile class as that only supports an FTP file. RemoteFile could, in the future,
either be made an interface or an abstract class, with factory methods producing the appropriate RemoteFile for a given connection implementation.
This would involve a lot of work but would make the system more flexible and extensible. If, even the refactoring could be done without adding other protocols, but have the correct interfaces,
it would be a great help for extending the project. The goal for this refactoring is to not change the UI code too much. It should only change enough to accommodate different connection types, in certain scenarios.
In most scenarios, the common Connection interface should keep the UI functionality the same.
See the GitHub issue https://github.com/edwardUL99/simple-ftp/issues/83 for discussion about this issue, and the analysis that would need to be done.

However, for version 1.0 and possibly a lot of different versions, the sole focus is FTP as that was the initial goal of this project and it should be completed as an FTP application.
Maybe, keep this idea for version 2.0 or even version 3.0 if you ever get there! (But with university/work, it's unlikely, I'd prefer to have a working FTP application before this is even a remote possibility)

## Note on Symbolic Links
Symbolic links are *experimental* as not every operating system supports them and depends on the FTP Server if they are supported or not.
So, because of this support issue among different systems, it's hard to have a consistent experience with symbolic links.

Any bugs/issues that are found however, should be reported so that they can be worked on and see if a solution can be worked on them.

In the future, it may be a possibility to implement some form of abstract symbolic link for systems that don't support links. These links would only exist within the application and would have to be stored somewhere. Maybe in a different XML file than the Session xml file for saving sessions (or in the session file, as you may want different links on a different server).
However, for now, we can only support symbolic links if they are supported on the relevant system/server.

#### Symbolic link supported features
The following features for symbolic links supported are:
- Following a symbolic link (i.e. following it as if it is an actual directory/file in the current directory)
- Following a link directly to its target instead of following it symbolically
- On a local panel, you can create a new symbolic link.
- On a remote panel, you can click create symbolic link but it just gives directions how to
- Navigating to symbolic links from entering the path in the go to dialog (for directories, gives the option to go to the path (follow), or go to the target) 
- The size of the symbolic link displayed is the size of the actual file it points to. This is contrary to a linux symbolic link for directories, but I believe showing the actual size is better.
