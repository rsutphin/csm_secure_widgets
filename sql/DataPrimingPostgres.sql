--
--  The following entry is for your application. 
--  Replace <<application_context_name>> with your application name.
-- 


INSERT INTO CSM_APPLICATION(
APPLICATION_NAME, APPLICATION_DESCRIPTION, DECLARATIVE_FLAG, ACTIVE_FLAG,UPDATE_DATE)
VALUES ('secure_widgets','Application Description','0','0',current_date)
;

INSERT INTO CSM_PROTECTION_ELEMENT(
PROTECTION_ELEMENT_NAME, PROTECTION_ELEMENT_DESCRIPTION, OBJECT_ID, APPLICATION_ID,UPDATE_DATE)
VALUES ( 'secure_widgets','secure_widgets Admin Application Protection Element','secure_widgets',1,current_date)
;

INSERT INTO CSM_PRIVILEGE(PRIVILEGE_NAME, PRIVILEGE_DESCRIPTION,UPDATE_DATE)
VALUES('CREATE','This privilege grants permission to a user to create an entity. This entity can be an object, a database entry, or a resource such as a network connection', current_date)
;
	
INSERT INTO CSM_PRIVILEGE(PRIVILEGE_NAME, PRIVILEGE_DESCRIPTION,UPDATE_DATE)
VALUES('ACCESS','This privilege allows a user to access a particular resource.  Examples of resources include a network or database connection, socket, module of the application, or even the application itself', current_date)
;
	
INSERT INTO CSM_PRIVILEGE(PRIVILEGE_NAME, PRIVILEGE_DESCRIPTION,UPDATE_DATE)
VALUES('READ','This privilege permits the user to read data from a file, URL, database, an object, etc. This can be used at an entity level signifying that the user is allowed to read data about a particular entry', current_date)
;
	
INSERT INTO CSM_PRIVILEGE(PRIVILEGE_NAME, PRIVILEGE_DESCRIPTION,UPDATE_DATE)
VALUES('WRITE','This privilege allows a user to write data to a file, URL, database, an object, etc. This can be used at an entity level signifying that the user is allowed to write data about a particular entity', current_date)
;
	
INSERT INTO CSM_PRIVILEGE(PRIVILEGE_NAME, PRIVILEGE_DESCRIPTION,UPDATE_DATE)
VALUES('UPDATE','This privilege grants permission at an entity level and signifies that the user is allowed to update data for a particular entity. Entities may include an object, object attribute, database row etc', current_date)
;
	
INSERT INTO CSM_PRIVILEGE(PRIVILEGE_NAME, PRIVILEGE_DESCRIPTION,UPDATE_DATE)
VALUES('DELETE','This privilege permits a user to delete a logical entity. This entity can be an object, a database entry, a resource such as a network connection, etc', current_date)
;
	
INSERT INTO CSM_PRIVILEGE(PRIVILEGE_NAME, PRIVILEGE_DESCRIPTION,UPDATE_DATE)
VALUES('EXECUTE','This privilege allows a user to execute a particular resource. The resource can be a method, function, behavior of the application, URL, button etc', current_date)
;

COMMIT
;
