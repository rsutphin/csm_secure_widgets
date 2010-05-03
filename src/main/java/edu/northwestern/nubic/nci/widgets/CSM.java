package edu.northwestern.nubic.nci.widgets;

import gov.nih.nci.security.AuthorizationManager;
import gov.nih.nci.security.dao.AuthorizationDAO;
import edu.northwestern.nubic.nci.widgets.csmext.FasterAuthorizationDaoImpl;
import gov.nih.nci.security.exceptions.CSConfigurationException;
import gov.nih.nci.security.provisioning.AuthorizationManagerImpl;
import gov.nih.nci.security.system.ApplicationSessionFactory;
import org.hibernate.SessionFactory;

import java.util.HashMap;

/**
 * @author Rhett Sutphin
 */
public class CSM {
    public static final String APPLICATION_NAME = "secure_widgets";
    private static AuthorizationDAO dao;
    private static AuthorizationManager manager;

    public synchronized static AuthorizationManager getAuthorizationManager() {
        if (manager == null) {
            try {
                // CSM requires that the SF be loaded before the AuthorizationManager is created.  Really.
                AuthorizationDAO dao = getAuthorizationDao();
                AuthorizationManagerImpl newMgr = new AuthorizationManagerImpl(APPLICATION_NAME);
                newMgr.setAuthorizationDAO(dao);
                manager = newMgr;
            } catch (CSConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        return manager;
    }

    public synchronized static AuthorizationDAO getAuthorizationDao() {
        if (dao == null) {
            try {
                SessionFactory sf = ApplicationSessionFactory.getSessionFactory(APPLICATION_NAME, csmConnectionProperties());
                dao = new FasterAuthorizationDaoImpl(sf, APPLICATION_NAME);
            } catch (CSConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        return dao;
    }

    private static HashMap<String, String> csmConnectionProperties() {
        HashMap<String, String> connectionProperties = new HashMap<String, String>();
        connectionProperties.put("hibernate.connection.url", "jdbc:postgresql:csm_for_secure_widgets");
        connectionProperties.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        connectionProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        connectionProperties.put("hibernate.connection.username", "rsutphin");
        connectionProperties.put("hibernate.connection.password", "");
        return connectionProperties;
    }

    // static class
    private CSM() { }
}
