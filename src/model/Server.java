/*
0 * Code that handles everything for the server
 */

package model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.swing.Timer;

public class Server {

    private static final int SERVER_PORT = 9001;

    static Map<String, User> allUsers;
    static Map<String, Document> allDocuments;
    static Map<String, OpenDocument> openDocuments;
    static List<ObjectOutputStream> clientOutStreams;

    /*
     * Listens for new incoming client connections, and creates a ClientHandler
     * to deal with them.
     */
    public static void main(String[] args) throws IOException {

	loadData();

	setUpSaveTimer();

	try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
	    System.out.println("Server started on port " + SERVER_PORT);
	    while (true) {
		new ClientHandler(serverSocket.accept()).start();
	    }
	}
    }

    @SuppressWarnings("unchecked")
    private static void loadData() {
	Scanner scan = new Scanner(System.in);
	System.out.println("Do you want to load the saved data?\nEnter 1 for yes or 0 for no.");
	int answer = scan.nextInt();
	scan.close();
	if (answer == 1) {
	    try {
		FileInputStream rawBytes = new FileInputStream("SaveFile");
		ObjectInputStream inFile = new ObjectInputStream(rawBytes);
		allUsers = Collections.synchronizedMap((Map<String, User>) inFile.readObject());
		allDocuments = Collections.synchronizedMap((Map<String, Document>) inFile.readObject());
		inFile.close();
	    } catch (Exception any) {
		any.printStackTrace();
	    }
	} else {
	    allUsers = Collections.synchronizedMap(new HashMap<>());
	    allDocuments = Collections.synchronizedMap(new HashMap<>());
	    hardCodeUsers();
	    hardCodeDocs();
	    saveData();
	}
	openDocuments = Collections.synchronizedMap(new HashMap<>());
	clientOutStreams = Collections.synchronizedList(new ArrayList<>());
    }

    /*
     * Hard code some Users for testing purposes.
     */
    private static void hardCodeUsers() {
	allUsers.put("Daniel", new User("Daniel", "Avetian"));
	allUsers.put("Michael", new User("Michael", "Carolin"));
	allUsers.put("Jacob", new User("Jacob", "Groh"));
	allUsers.put("Filbert", new User("Filbert", "Johnson"));
	allUsers.put("Orzy", new User("Orzy", "Hazan"));
    }

    /*
     * Hard code some Documents for testing purposes.
     */
    private static void hardCodeDocs() {
	allDocuments.put("DanielsDoc", new Document("DanielsDoc", "Daniel"));
	allUsers.get("Daniel").addOwnedDocument("DanielsDoc");

	allUsers.get("Michael").addEditableDocument("DanielsDoc");
	allUsers.get("Filbert").addEditableDocument("DanielsDoc");
	allUsers.get("Orzy").addEditableDocument("DanielsDoc");
	allUsers.get("Jacob").addEditableDocument("DanielsDoc");
	allDocuments.get("DanielsDoc").addEditor("Michael");
	allDocuments.get("DanielsDoc").addEditor("Filbert");
	allDocuments.get("DanielsDoc").addEditor("Orzy");
	allDocuments.get("DanielsDoc").addEditor("Jacob");

	allDocuments.put("MichaelsDoc", new Document("MichaelsDoc", "Michael"));
	allUsers.get("Michael").addOwnedDocument("MichaelsDoc");

	allDocuments.put("JacobsDoc", new Document("JacobsDoc", "Jacob"));
	allUsers.get("Jacob").addOwnedDocument("JacobsDoc");

	allDocuments.put("FilbertsDoc", new Document("FilbertsDoc", "Filbert"));
	allUsers.get("Filbert").addOwnedDocument("FilbertsDoc");

	allDocuments.put("OrzysDoc", new Document("OrzysDoc", "Orzy"));
	allUsers.get("Orzy").addOwnedDocument("OrzysDoc");
    }

    /*
     * Creates and starts a timer that calls the saveData method every 30
     * seconds.
     */
    private static void setUpSaveTimer() {
	Timer saveTimer = new Timer(30000, new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent arg0) {
		saveData();
	    }
	});
	saveTimer.start();
    }

    /*
     * Writes the HashMaps that contain the User and Document data to a file.
     */
    private static void saveData() {
	try {
	    FileOutputStream bytesToDisk = new FileOutputStream("SaveFile");
	    ObjectOutputStream outFile = new ObjectOutputStream(bytesToDisk);
	    outFile.writeObject(allUsers);
	    outFile.writeObject(allDocuments);
	    outFile.close();
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
    }
}

/*
 * Client handler that is created and started each time a new connection is
 * created. Handles everything for one client
 */
class ClientHandler extends Thread {

    private Socket clientSocket;
    private ObjectInputStream clientIn; // clientIn stream of the new client
    private ObjectOutputStream clientOut; // output stream of the new client
    private User currentUser; // current user logged in
    private OpenDocument currentOpenDoc; // current document being edited by the
    private boolean isRunning, removeStreams; // booleans...

    /*
     * Opens the Input and Output Streams.
     */
    ClientHandler(Socket clientSocket) {
	this.clientSocket = clientSocket;
	try {
	    clientIn = new ObjectInputStream(clientSocket.getInputStream());
	    clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
	    isRunning = true;
	    System.out.println("Accepted a new connection from " + clientSocket.getInetAddress());
	} catch (IOException e) {
	    e.printStackTrace();
	    closeConnection();
	}
    }

    /*
     * While the thread is still running, get the next ClientRequest from the
     * client, and call the respective method.
     */
    @Override
    public void run() {
	ClientRequest request;
	while (isRunning) {
	    try {
		request = (ClientRequest) clientIn.readObject();
		switch (request) {
		case LOGIN:
		    authenticateUser();
		    break;
		case CREATE_ACCOUNT:
		    createAccount();
		    break;
		case CHANGE_PASSWORD:
		    changePassword();
		    break;
		case GET_DOCS:
		    sendDocumentList();
		    break;
		case GET_EDITORS:
		    sendEditorList();
		    break;
		case GET_USERS:
		    searchUsers();
		    break;
		case CHAT_MSG:
		    updateChat();
		    break;
		case CREATE_DOC:
		    createDocument();
		    break;
		case ADD_PERMISSION:
		    addPermission();
		    break;
		case REMOVE_PERMISSION:
		    removePermission();
		    break;
		case OPEN_DOC:
		    openDocument();
		    break;
		case DOC_TEXT:
		    updateDocument();
		    break;
		case SAVE_REVISION:
		    saveRevision();
		    break;
		case UNDO:
		    undoDocument();
		    break;
		case GET_REVISIONS:
		    sendRevisionList();
		    break;
		case REVERT_DOC:
		    revertDocument();
		    break;
		case CLOSE_DOC:
		    closeDocument();
		    break;
		case DELETE_DOC:
		    deleteDocument();
		    break;
		case LOGOUT:
		    logoutAndCloseConnection();
		    break;
		default:
		    System.out.println("Catastrophic Failure.");
		    System.out.flush();
		    System.exit(1);
		    break;
		}
	    } catch (ClassNotFoundException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
		Server.clientOutStreams.remove(clientOut);
		isRunning = false;
	    }
	}
	logoutAndCloseConnection();
    }

    /*
     * Gets the username and password from the client, and creates a new User
     * account. Verifies that the username is unique.
     */
    private void createAccount() throws ClassNotFoundException, IOException {
	String username = (String) clientIn.readObject();
	String password = (String) clientIn.readObject();
	if (Server.allUsers.get(username) != null) {
	    clientOut.writeObject(ServerResponse.ACCOUNT_EXISTS);
	} else {
	    User newUser = new User(username, password);
	    Server.allUsers.put(username, newUser);
	    newUser.setLogin(true);
	    currentUser = newUser;
	    Server.clientOutStreams.add(clientOut);
	    clientOut.writeObject(ServerResponse.ACCOUNT_CREATED);
	}
    }

    /*
     * Gets the credentials from the client and checks if the match a User
     * account. Verifies that the User is not already logged in
     */
    private void authenticateUser() throws ClassNotFoundException, IOException {
	User user = null;
	String username = (String) clientIn.readObject();
	String password = (String) clientIn.readObject();

	if ((user = Server.allUsers.get(username)) == null) {
	    clientOut.writeObject(ServerResponse.INCORRECT_USERNAME);
	} else if (user.isLoggedIn()) {
	    clientOut.writeObject(ServerResponse.LOGGED_IN);
	} else if ((user.getSalt() + password).hashCode() != user.getHashPass()) {
	    clientOut.writeObject(ServerResponse.INCORRECT_PASSWORD);
	} else {
	    user.setLogin(true);
	    currentUser = user;
	    Server.clientOutStreams.add(clientOut);
	    clientOut.writeObject(ServerResponse.LOGIN_SUCCESS);
	}
    }

    /*
     * Changes the password associated with the User account.
     */
    private void changePassword() throws ClassNotFoundException, IOException {
	String username = (String) clientIn.readObject();
	String newPassword = (String) clientIn.readObject();
	User user = Server.allUsers.get(username);
	if (user == null) {
	    clientOut.writeObject(ServerResponse.INCORRECT_USERNAME);
	} else {
	    user.setPassword(newPassword);
	    clientOut.writeObject(ServerResponse.PASSWORD_CHANGED);
	}
    }

    /*
     * Sends a list of Documents owned by the User, and a list of Documents that
     * the User has permission to edit.
     */
    private void sendDocumentList() throws IOException {
	List<String> usersOwnedDocuments = new ArrayList<String>();
	for (String docName : currentUser.getOwnedDocuments()) {
	    if (Server.allDocuments.get(docName).hasNoRevisions()) {
		usersOwnedDocuments.add(docName + "  -  Last Revision: None");
	    } else {
		usersOwnedDocuments
			.add(docName + "  -  Last Revision: " + Server.allDocuments.get(docName).getLastRevisionKey());
	    }
	}
	List<String> usersEditableDocuments = new ArrayList<String>();
	for (String docName : currentUser.getEditableDocuments()) {
	    if (Server.allDocuments.get(docName).hasNoRevisions()) {
		usersEditableDocuments.add(docName + "  -  Last Revision: None");
	    } else {
		usersEditableDocuments
			.add(docName + "  -  Last Revision: " + Server.allDocuments.get(docName).getLastRevisionKey());
	    }
	}
	clientOut.writeObject(usersOwnedDocuments);
	clientOut.writeObject(usersEditableDocuments);
    }

    /*
     * Sends a list of all Users that have permission to edit the specified
     * document.
     */
    private void sendEditorList() throws ClassNotFoundException, IOException {
	String fullDocName = (String) clientIn.readObject();
	String docName = fullDocName.substring(0, fullDocName.indexOf("  -  "));
	Document document = Server.allDocuments.get(docName);
	if (document == null) {
	    clientOut.writeObject(ServerResponse.NO_DOCUMENT);
	} else {
	    clientOut.writeObject(ServerResponse.DOCUMENT_EXISTS);
	    clientOut.reset();
	    clientOut.writeObject(document.getEditors());
	}
    }

    /*
     * Gets a String from the client, and creates a list of all User names that
     * contain the specified text. Sends the list to the client.
     */
    private void searchUsers() throws IOException, ClassNotFoundException {
	String searchFor = (String) clientIn.readObject();
	Set<String> userNames = Server.allUsers.keySet();
	Iterator<String> itr = userNames.iterator();
	List<String> foundUsers = new ArrayList<String>();
	String temp;
	while (itr.hasNext()) {
	    if ((temp = itr.next()).contains(searchFor)) {
		foundUsers.add(temp);
	    }
	}
	clientOut.writeObject(foundUsers);
    }

    /*
     * Creates a new Document, with the current User as the owner. Verifies that
     * the Document name is unique.
     */
    private void createDocument() throws ClassNotFoundException, IOException {
	String docName = (String) clientIn.readObject();
	if (Server.allDocuments.get(docName) != null) {
	    clientOut.writeObject(ServerResponse.DOCUMENT_EXISTS);
	} else {
	    Document newDocument = new Document(docName, currentUser.getName());
	    Server.allDocuments.put(docName, newDocument);
	    currentUser.addOwnedDocument(docName);
	    currentOpenDoc = new OpenDocument(newDocument, clientOut, currentUser.getName());
	    Server.openDocuments.put(docName, currentOpenDoc);
	    clientOut.writeObject(ServerResponse.DOCUMENT_CREATED);
	    clientOut.writeObject(currentOpenDoc.getText());
	}
    }

    /*
     * Adds permission for a User to edit a Document.
     */
    private void addPermission() throws ClassNotFoundException, IOException {
	String username = (String) clientIn.readObject();
	String fullDocName = (String) clientIn.readObject();
	String docName = fullDocName.substring(0, fullDocName.indexOf("  -  "));
	User user = Server.allUsers.get(username);
	Document document = Server.allDocuments.get(docName);
	if (document == null) {
	    clientOut.writeObject(ServerResponse.NO_DOCUMENT);
	} else if (!currentUser.owns(docName)) {
	    clientOut.writeObject(ServerResponse.PERMISSION_DENIED);
	} else {
	    if (!user.hasPermission(docName)) {
		user.addEditableDocument(docName);
	    }
	    if (!document.isEditableBy(username)) {
		document.addEditor(username);
	    }
	    clientOut.writeObject(ServerResponse.PERMISSION_ADDED);
	}
    }

    /*
     * Removes permission for a User to edit a Document.
     */
    private void removePermission() throws ClassNotFoundException, IOException {
	String username = (String) clientIn.readObject();
	String fullDocName = (String) clientIn.readObject();
	String docName = fullDocName.substring(0, fullDocName.indexOf("  -  "));
	Document document = Server.allDocuments.get(docName);
	if (document == null) {
	    clientOut.writeObject(ServerResponse.NO_DOCUMENT);
	} else if (document.getOwner().equals(username)) {
	    clientOut.writeObject(ServerResponse.PERMISSION_DENIED);
	} else {
	    Server.allUsers.get(username).removeDocument(docName);
	    document.removeEditor(username);
	    clientOut.writeObject(ServerResponse.PERMISSION_REMOVED);
	}
    }

    /*
     * Opens a Document for editing. Checks to see whether the Document exists,
     * if the User has permission to edit it. ALso check if there is already an
     * OpenDocument for the Document to open, which means the document is
     * currently being edited by another User(s). If not it creates an
     * OpenDocument for the Document to be opened.
     */
    private void openDocument() throws ClassNotFoundException, IOException {
	String fullDocName = (String) clientIn.readObject();
	String docName = fullDocName.substring(0, fullDocName.indexOf("  -  "));
	Document openingDoc = Server.allDocuments.get(docName);
	if (openingDoc == null) {
	    clientOut.writeObject(ServerResponse.NO_DOCUMENT);
	} else if (!currentUser.hasPermission(docName)) {
	    clientOut.writeObject(ServerResponse.PERMISSION_DENIED);
	} else {
	    currentOpenDoc = Server.openDocuments.get(docName);
	    if (currentOpenDoc == null) {
		currentOpenDoc = new OpenDocument(openingDoc, clientOut, currentUser.getName());
		Server.openDocuments.put(docName, currentOpenDoc);
	    } else {
		currentOpenDoc.addEditor(clientOut, currentUser.getName());
	    }
	    clientOut.writeObject(ServerResponse.DOCUMENT_OPENED);
	    clientOut.writeObject(currentOpenDoc.getText());
	    sendUpdateToClients(ServerResponse.CURRENT_EDITORS, currentOpenDoc.getEditorNames(), false);
	}
    }

    /*
     * Reads in the new chat message from the client.
     */
    private void updateChat() throws ClassNotFoundException, IOException {
	String chatMessage = currentUser.getName() + ": " + (String) clientIn.readObject();
	sendUpdateToClients(ServerResponse.CHAT_UPDATE, chatMessage, false);
    }

    /*
     * Reads in the new Document update from the client.
     */
    private void updateDocument() throws ClassNotFoundException, IOException {
	currentOpenDoc.updateText((String) clientIn.readObject());
	sendUpdateToClients(ServerResponse.DOCUMENT_UNEDITABLE, currentUser.getName(), true);
	sendUpdateToClients(ServerResponse.DOCUMENT_UPDATE, currentOpenDoc.getText(), true);
    }

    /*
     * Saves a revision. Tells clients that the current openDocument is
     * editable.
     */
    private void saveRevision() throws IOException {
	currentOpenDoc.saveRevision(currentUser.getName());
	sendUpdateToClients(ServerResponse.DOCUMENT_EDITABLE, "", false);
    }

    /*
     * Sends a list of Revisions to the client.
     */
    private void sendRevisionList() throws IOException {
	clientOut.writeObject(ServerResponse.REVISION_LIST);
	clientOut.reset();
	clientOut.writeObject(currentOpenDoc.getRevisionList());
    }

    /*
     * Reverts the current OpenDocument to the requested revision.
     */
    private void revertDocument() throws ClassNotFoundException, IOException {
	String documentKey = (String) clientIn.readObject();
	currentOpenDoc.revert(documentKey);
	sendUpdateToClients(ServerResponse.DOCUMENT_UPDATE, currentOpenDoc.getText(), false);
    }

    /*
     * Reverts the current OpenDocument to its most recent revision.
     */
    private void undoDocument() throws IOException {
	sendUpdateToClients(ServerResponse.DOCUMENT_UPDATE, currentOpenDoc.undo(), false);
    }

    /*
     * Takes in a ServerResponse, that indicates what type of update is being
     * sent, (Document or chat), and a String containing the text of the update.
     * Sends the ServerResponse and the text to all of the clients currently in
     * the list of output streams in the OpenDocument. Keeps track of the
     * clients that have disconnected and removes them from the list of
     * OuputStreams in the current OpenDocument.
     */
    private void sendUpdateToClients(ServerResponse response, Object update, boolean skipThisClient)
	    throws IOException {
	removeStreams = false;
	Set<ObjectOutputStream> closedEditors = new HashSet<ObjectOutputStream>();
	for (ObjectOutputStream editorOutStream : currentOpenDoc.getEditorOutStreams()) {
	    if (skipThisClient && editorOutStream == clientOut) {
		if (response == ServerResponse.DOCUMENT_UNEDITABLE) {
		    clientOut.writeObject(ServerResponse.CURRENT_TYPER);
		}
		continue;
	    }
	    try {
		editorOutStream.reset();
		editorOutStream.writeObject(response);
		editorOutStream.writeObject(update);
	    } catch (IOException e) {
		removeStreams = true;
		closedEditors.add(editorOutStream);
	    }
	}
	if (removeStreams) {
	    currentOpenDoc.removeClosedEditorStreams(closedEditors);
	    removeStreams = false;
	    if (currentOpenDoc.hasNoEditors()) {
		Server.openDocuments.remove(currentOpenDoc.getDocumentName());
	    }
	}
    }

    /*
     * Removes the User from the current OpenDocument. Closes the current
     * OpenDocument if the user was the only editor.
     */
    private void closeDocument() throws IOException {
	currentOpenDoc.removeEditor(clientOut, currentUser.getName());
	clientOut.writeObject(ServerResponse.DOCUMENT_CLOSED);
	if (currentOpenDoc.hasNoEditors()) {
	    Server.openDocuments.remove(currentOpenDoc.getDocumentName());
	} else {
	    sendUpdateToClients(ServerResponse.CURRENT_EDITORS, currentOpenDoc.getEditorNames(), false);
	}
    }

    /*
     * Deletes a Document.
     */
    private void deleteDocument() throws ClassNotFoundException, IOException {
	String fullDocName = (String) clientIn.readObject();
	String docName = fullDocName.substring(0, fullDocName.indexOf("  -  "));
	Document toDelete = Server.allDocuments.get(docName);
	if (toDelete == null) {
	    clientOut.writeObject(ServerResponse.NO_DOCUMENT);
	} else if (!currentUser.owns(docName)) {
	    clientOut.writeObject(ServerResponse.PERMISSION_DENIED);
	} else if (Server.openDocuments.get(docName) != null) {
	    clientOut.writeObject(ServerResponse.DOCUMENT_OPENED);
	} else {
	    Server.allDocuments.remove(docName);
	    currentUser.removeDocument(docName);
	    clientOut.writeObject(ServerResponse.DOCUMENT_DELETED);
	}
    }

    /*
     * Logs out the current User and closes the connection.
     */
    private void logoutAndCloseConnection() {
	Server.clientOutStreams.remove(clientOut);
	currentUser.setLogin(false);
	isRunning = false;
	closeConnection();
    }

    /*
     * Closes the socket.
     */
    private void closeConnection() {
	try {
	    clientSocket.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
}
