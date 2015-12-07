package model;

public enum ServerResponse {
   ACCOUNT_CREATED, ACCOUNT_EXISTS, INCORRECT_USERNAME, INCORRECT_PASSWORD,
   LOGIN_SUCCESS, LOGGED_IN,  PASSWORD_CHANGED, LOGOUT_SUCCESS,
   DOCUMENT_CREATED, DOCUMENT_EXISTS, NO_DOCUMENT, DOCUMENT_OPENED, 
   DOCUMENT_CLOSED, DOCUMENT_DELETED, PERMISSION_ADDED, PERMISSION_DENIED, 
   PERMISSION_REMOVED, DOCUMENT_UPDATE, CHAT_UPDATE, REVISION_LIST,
   DOCUMENT_REVERTED, DOCUMENT_EDITABLE, DOCUMENT_UNEDITABLE;
}
