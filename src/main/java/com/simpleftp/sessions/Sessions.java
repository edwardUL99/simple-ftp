/*
 *  Copyright (C) 2020  Edward Lynch-Milner
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.simpleftp.sessions;

import com.simpleftp.filesystem.LocalFile;
import com.simpleftp.ftp.connection.FTPConnectionDetails;
import com.simpleftp.ftp.connection.Server;
import com.simpleftp.sessions.exceptions.SessionInitialisationException;
import com.simpleftp.sessions.exceptions.SessionLoadException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class provides the integration between the session classes so we can use sessions with 1 class.
 * This class also abstracts the constraint that we can only have 1 active session per login instance. It hides
 * all the details ensuring this constraint is maintained.
 *
 * It also abstracts the management/location of the session file on the disk.
 *
 * This is the main "driver" for interacting with sessions in client classes outside the sessions package.
 * To start the sessions service, a call to Sessions#initialise and a check after with Sessions#isInitialised enables the sessions service.
 *
 * This restricts the use of the sessions package outside the package to the use of a single session at a time as only one instance of a login can save and read a session at that same time.
 * This single session instance can be accessed with getCurrentSession. The current session must be initialised with the specified setCurrentSession methods, which will do a check to see if details matches an existing session and use that,
 * or else create a new one and return that.
 *
 * Changes aren't persisted until you call the save method on the current session.
 *
 * This class provides the main interface between the sessions file both in memory and on disk.
 *
 * This class is not thread-safe. The results of multiple threads accessing and saving the current session at the same time are undefined.
 */
@Log4j2
public final class Sessions {
    /**
     * The path to the session file on disk
     */
    private static String sessionFilePath;

    /**
     * The session loader for loading in our session file
     */
    private static SessionLoader sessionLoader;

    /**
     * The session saver used for saving session files
     */
    private static SessionSaver sessionSaver;

    /**
     * The read in session file from memory
     */
    private static SessionFile sessionFile;

    /**
     * The current session being used.
     */
    private static Session currentSession;

    /**
     * Keeps track of the initialisation of session saving.
     */
    @Getter
    private static boolean initialised = false;

    /**
     * A boolean to keep track of an existing file found. This returns false, if a new file has to be created
     */
    private static boolean existingFileFound;

    /**
     * A boolean to indicate if an existing file was loaded in successfully.
     */
    private static boolean existingFileLoadedIn;

    /**
     * The exception that prevented an existing file to be loaded
     */
    private static SessionLoadException fileLoadException;

    /**
     * Path separator for constructing paths
     */
    private static final String PATH_SEPARATOR = System.getProperty("file.separator");

    /**
     * Don't allow initialisation
     */
    private Sessions() {}

    /**
     * Initialises the variables required
     */
    private static void initialiseVariables() {
        existingFileLoadedIn = existingFileFound = false;
        fileLoadException = null;
        sessionFile = null;
        sessionSaver = null;
        sessionLoader = null;
        sessionFilePath = null;
        currentSession = null;
        initialised = false;
    }

    /**
     * Intiialises the session file path
     */
    private static void initialiseSessionFilePath() {
        String sessionFileDirectory = System.getProperty("user.home") + PATH_SEPARATOR + ".simple-ftp";
        LocalFile file = new LocalFile(sessionFileDirectory);

        if (file.isFile()) {
            throw new SessionInitialisationException("The storage location for the session file: " + sessionFileDirectory + " already exists as a file. Can't initialise session saving");
        }

        if (!file.isDirectory()) {
            if (!file.mkdir()) {
                throw new SessionInitialisationException("Failed to create the storage location for the session file: " + sessionFileDirectory);
            }
        }

        sessionFilePath = sessionFileDirectory + PATH_SEPARATOR + "sessions.xml";
    }

    /**
     * Initialises the session file and the savers and loaders for it.
     * This is the final initialisation method and this sets the value for the flag initialised as that flag surrounds the fact that session file is initialised and so are the session saver and loader
     * @param deleteExisting true to delete the existing session file if it failed to load
     */
    private static void initialiseSessionFile(boolean deleteExisting) {
        LocalFile file = new LocalFile(sessionFilePath);

        if (file.isDirectory()) {
            throw new SessionInitialisationException("The session file " + sessionFilePath + " already exists as a directory");
        }

        sessionLoader = new SessionLoader();
        sessionSaver = new SessionSaver();

        if (file.exists()) {
            if (deleteExisting) {
                if (!file.delete())
                    throw new SessionInitialisationException("Could not delete existing session file");

                existingFileFound = false; // we are not using the existing one
                sessionFile = new SessionFile(sessionFilePath);
            } else {
                existingFileFound = true;

                try {
                    sessionLoader.initialiseLoader(sessionFilePath);
                    sessionFile = sessionLoader.loadFile();
                    existingFileLoadedIn = true;
                } catch (SessionLoadException exception) {
                    existingFileLoadedIn = initialised = false;
                    fileLoadException = new SessionLoadException("Failed to load in existing session file. The data could be corrupted/invalid", exception);
                    return;
                }
            }
        } else {
            existingFileFound = false;
            sessionFile = new SessionFile(sessionFilePath);
        }

        initialised = true;
    }

    /**
     * Initialises the Sessions class for saving of sessions.
     * @param deleteExisting set this to true to delete any existing session file and start with a new session file. This could be useful if you are getting an exception trying to initialise when a file already exists
     */
    public static void initialise(boolean deleteExisting) {
        initialiseVariables();
        initialiseSessionFilePath();
        initialiseSessionFile(deleteExisting);
    }

    /**
     * Checks if the Sessions framework is initialised, and throws a SessionInitialisationException if not
     */
    private static  void checkInitialisation() {
        if (!initialised) {
            throw new SessionInitialisationException("The Session saving functionality is not initialised");
        }
    }

    /**
     * Returns true if the sessions file represents an existing session file
     * @return true if sessions file represents an existing one, false if not
     */
    public static boolean existingFileFound() {
        return existingFileFound;
    }

    /**
     * Returns true if an existing file was loaded in successfully.
     * @return true if existing file was loaded in successfully, false if not. If this returns false but existingFileFound() returns true, check the exception to find out why it failed with
     * getExistingLoadException and then try to re-initialise with a value of true for deleteExisting
     */
    public static boolean existingFileLoadedSuccessfully() {
        return existingFileLoadedIn;
    }

    /**
     * If an existing file was found but failed to be loaded, use this method to access the exception
     * @return the causing exception if exists, false if not
     */
    public static SessionLoadException getExistingLoadException() {
        return fileLoadException;
    }

    /**
     * Retrieves the SessionSaver object being used to save the file. Package private to enforce saving of a Session through the Session#save() method
     * @return the SessionSaver being used to save the sessions
     */
    static SessionSaver getSessionSaver() {
        checkInitialisation();
        return sessionSaver;
    }

    /**
     * You can allow direct access to the sessions file because outside of the sessions package, the methods that can modify its state are hidden
     * @return the session file being saved to in memory
     */
    public static SessionFile getSessionFile() {
        checkInitialisation();
        return sessionFile;
    }

    /**
     * Gets the current session that is being used
     * @return the current session
     */
    public static Session getCurrentSession() {
        checkInitialisation();
        return currentSession;
    }

    /**
     * Sets the current session to be used. If this session is not in the SessionFile, an IllegalArgumentException is thrown
     * This can be used if you have a session already loaded in and you want to load it rather than entering details and creating a new one.
     * If a session equals a session found in the session file, but have different addresses, it means that you didn't pass in a session that was listed from the session file.
     * It is undefined what happens if this occurs because other objects inside the session may have different addresses and may not save correctly,
     * so an IllegalArgumentException will be thrown. To create a new session that is not on file yet, you need to call the method which takes server details and allow that method to create the session and save it on file
     * @param currentSession the session to use
     */
    public static void setCurrentSession(Session currentSession) {
        checkInitialisation();

        AtomicBoolean refsNonMatch = new AtomicBoolean(false); // equal sessions found but the references don't match
        boolean contains = sessionFile.getSessions()
                .stream()
                .filter(session -> session.equals(currentSession))
                .findFirst()
                .map(session -> {
                    refsNonMatch.set(session != currentSession);
                    return true;
                })
                .orElse(false);

        if (refsNonMatch.get())
            throw new IllegalArgumentException("The provided session is equal to one in the Session file, but it does not refer to the same object. This session should be retrieved from the getAllSessions returned set");

        if (!contains) {
            throw new IllegalArgumentException("The provided session does not exist in the Session File");
        }

        Sessions.currentSession = currentSession;
    }

    /**
     * Attempts to find a session with the same server details and connection details (excluding password) and returns it if found.
     * If not found, it returns null.
     * @param serverDetails the server details to match
     * @param connectionDetails the connection details to match
     * @return the found session, or null if not found
     */
    private static Session matchSession(Server serverDetails, FTPConnectionDetails connectionDetails) {
        return sessionFile.getSessions()
                .stream()
                .filter(session -> session.getServerDetails().equals(serverDetails)
                        && session.getFtpConnectionDetails().equals(connectionDetails))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the last session ID used
     * @return the last session ID used
     */
    private static int getLastId() {
        int sessionId = 0;
        Set<Session> sessions = sessionFile.getSessions();

        if (!sessions.isEmpty()) {
            Session lastSession = null; // should be the sorted one
            for (Session session : sessions)
                lastSession = session;

            if (lastSession != null)
                sessionId = lastSession.getSessionId();
        }

        return sessionId;
    }

    /**
     * Creates a new session with the provided details
     * @param serverDetails the server details to save the file with
     * @param connectionDetails the connection details
     * @return the created session
     */
    private static Session createNewSession(Server serverDetails, FTPConnectionDetails connectionDetails) {
        int sessionId = getLastId();
        return new Session(++sessionId, serverDetails, connectionDetails, new Session.LastSession());
    }

    /**
     * This is the operation to get a Session object representing/matching the provided details.
     * It attempts to match an existing session with this one based on server host, user, port and matching connection details. If a match is found,
     * it returns that session. If a match could not be found, the match is returned (with password updated to the one in server details if provided).
     *
     * This method can be used to store entered details to be saved as a new session. If a session is chosen from an existing list, the setCurrentSession method should be used
     * instead.
     *
     * This method does not save the created session to file. You would need to call save on the returned session
     *
     * @param serverDetails to construct/match the session with
     * @param connectionDetails the details to construct/match the session with
     * @return the constructed session if doesn't exist on file, or the matched session
     */
    public static Session getSession(Server serverDetails, FTPConnectionDetails connectionDetails) {
        checkInitialisation();
        Session session = matchSession(serverDetails, connectionDetails);

        if (session == null) {
            session = createNewSession(serverDetails, connectionDetails);
            sessionFile.addSession(session);
        } else {
            session.getServerDetails().setPassword(serverDetails.getPassword()); // set the password in case they are different
        }

        return session;
    }

    /**
     * Returns all the saved sessions
     * @return Set of all sessions
     */
    public static Set<Session> getAllSessions() {
        checkInitialisation();
        return sessionFile.getSessions();
    }

    /**
     * Returns a set of sessions that match the given predicate
     * @param predicate the predicate to match
     * @return a set of sessions matching the predicate
     */
    public static Set<Session> getAllSessions(Predicate<Session> predicate) {
        checkInitialisation();
        return sessionFile.getSessions()
                .stream()
                .filter(predicate)
                .collect(Collectors.toSet());
    }

    /**
     * Deletes the session specified by session ID and if found, returns the deleted session.
     * While you may already have a list of sessions to remove, it is safest to match by ID and remove that session retrieved as session IDs are unique.
     * Note that calling save on the returned session will throw an UnsupportedOperationException as it is no longer a part of a SessionFile to save
     * @param sessionId the id of the session to remove
     * @return the removed session, null if no session matches the session ID
     */
    public static Session deleteSession(int sessionId) throws Exception {
        checkInitialisation();
        Optional<Session> sessionToRemove = getAllSessions().stream()
                .filter(session -> session.getSessionId() == sessionId)
                .findFirst();

        AtomicReference<Exception> caughtException = new AtomicReference<>();

        Session removedSession = sessionToRemove.map(session -> {
            try {
                return deleteSession(session) ? session:null;
            } catch (Exception ex) {
                caughtException.set(ex);
                return null;
            }
        }).orElse(null);

        Exception exceptionThrown = caughtException.get();
        if (exceptionThrown != null)
            throw exceptionThrown;

        return removedSession;
    }

    /**
     * Removes the session in the file equals to the provided session
     * @param session the session to remove
     * @return true if removed successfully
     * @throws Exception if deletion throws one
     */
    public static boolean deleteSession(Session session) throws Exception {
        checkInitialisation();
        Set<Session> sessions = getAllSessions();
        if (!sessions.contains(session)) {
            throw new IllegalArgumentException("The provided session has already been deleted as it is not in the session file");
        } else {
            sessionFile.removeSession(session);
            boolean removed = !sessions.contains(session);
            if (removed) {
                sessionSaver.initialiseSaver(sessionFile);
                sessionSaver.writeSessionFile();
            }

            return removed;
        }
    }
}