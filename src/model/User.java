package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class User implements Serializable {

    private static final long serialVersionUID = 930908671628905940L;
    private String username;
    private int salt;
    private int hashPass;
    private boolean loggedIn;
    private List<String> ownedDocuments;
    private List<String> editableDocuments;

    User(String newUser, String pass) {
	this.username = newUser;
	setPassword(pass);
	ownedDocuments = new ArrayList<String>();
	editableDocuments = new ArrayList<String>();
    }

    /*
     * Sets the users password. Creates a random 8 digit number for a password
     * salt, concatenates it with the password, and saves the hash code.
     */
    void setPassword(String pass) {
	Random rng = new Random();
	salt = rng.nextInt(90000000);
	hashPass = (salt + pass).hashCode();
    }

    String getName() {
	return username;
    }

    int getSalt() {
	return salt;
    }

    int getHashPass() {
	return hashPass;
    }

    void setLogin(boolean status) {
	loggedIn = status;
    }

    boolean isLoggedIn() {
	return loggedIn;
    }

    boolean owns(String documentName) {
	return ownedDocuments.contains(documentName);
    }

    boolean hasPermission(String documentName) {
	return ownedDocuments.contains(documentName) || editableDocuments.contains(documentName);
    }

    void addOwnedDocument(String documentName) {
	ownedDocuments.add(documentName);
	editableDocuments.add(documentName);
    }

    void addEditableDocument(String documentName) {
	editableDocuments.add(documentName);
    }

    void removeDocument(String documentName) {
	ownedDocuments.remove(documentName);
	editableDocuments.remove(documentName);
    }

    List<String> getOwnedDocuments() {
	return ownedDocuments;
    }

    List<String> getEditableDocuments() {
	return editableDocuments;
    }
}
