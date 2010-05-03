package edu.northwestern.nubic.nci.widgets.csmext;

import gov.nih.nci.logging.api.logger.hibernate.HibernateSessionFactoryHelper;
import gov.nih.nci.security.authorization.domainobjects.Privilege;
import gov.nih.nci.security.authorization.domainobjects.ProtectionElement;
import gov.nih.nci.security.authorization.domainobjects.ProtectionElementPrivilegeContext;
import gov.nih.nci.security.exceptions.CSConfigurationException;
import gov.nih.nci.security.exceptions.CSObjectNotFoundException;
import gov.nih.nci.security.dao.AuthorizationDAOImpl;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Rhett Sutphin
 */
// TODO: if we use this in production, it needs to be cleaned up (a lot) and tested
@SuppressWarnings({ "RawUseOfParameterizedType" })
public class FasterAuthorizationDaoImpl extends AuthorizationDAOImpl {
    private SessionFactory sf;

    public FasterAuthorizationDaoImpl(SessionFactory sf, String applicationContextName) throws CSConfigurationException {
        super(sf, applicationContextName);
        this.sf = sf; // not exposed by superclass, so we have to capture it here
    }

    /**
     * This is a copy of the superclass method, modified to be faster for the specific requirements of CCTS.
     * Changes:
     * <ul>
     *   <li>It does fewer separate database queries to load PEs and Privs.  10% speedup.</li>
     *   <li>It uses a simpler query to load the PE-to-Priv mapping.
     *       Specifically, it drops support for hierarchical protection groups.
     *       This gives approximately a 3000% speedup (2500ms to 80ms).</li>
     * </ul>
     */
    @Override
    public Set getProtectionElementPrivilegeContextForUser(String userId) throws CSObjectNotFoundException {
        Set<ProtectionElementPrivilegeContext> protectionElementPrivilegeContextSet =
            new LinkedHashSet<ProtectionElementPrivilegeContext>();
        Session s = null;

        try {
            s = HibernateSessionFactoryHelper.getAuditSession(getSessionFactory());
            Map<Long, Set<Long>> peToPriv = getProtectionElementIdToPrivilegeIdsForUser(s, userId);

            Collection<ProtectionElement> allProtectionElements = getProtectionElementsByIds(s, peToPriv.keySet());
            Map<Long, Privilege> privById = new LinkedHashMap<Long, Privilege>();
            {
                Set<Long> allPrivIds = new LinkedHashSet<Long>();
                for (Set<Long> privIds : peToPriv.values()) {
                    allPrivIds.addAll(privIds);
                }
                Collection<Privilege> allPrivileges = getPrivilegesByIds(s, allPrivIds);
                for (Privilege priv : allPrivileges) privById.put(priv.getId(), priv);
            }

            {
                long startTime = System.currentTimeMillis();
                for (ProtectionElement protectionElement : allProtectionElements) {
                    ProtectionElementPrivilegeContext context = new ProtectionElementPrivilegeContext();
                    context.setProtectionElement(protectionElement);
                    Set<Privilege> privs = new LinkedHashSet<Privilege>();
                    for (Long privId : peToPriv.get(protectionElement.getProtectionElementId())) {
                        privs.add(privById.get(privId));
                    }
                    context.setPrivileges(privs);
                    protectionElementPrivilegeContextSet.add(context);
                }
                System.err.println("- mapping " + protectionElementPrivilegeContextSet.size() + " contexts took " + (System.currentTimeMillis() - startTime) + "ms");
            }
        } finally {
            if (s != null) s.close();
        }
        return protectionElementPrivilegeContextSet;
    }

    private Map<Long, Set<Long>> getProtectionElementIdToPrivilegeIdsForUser(Session s, String userId) {
        long startTime = System.currentTimeMillis();
        Map<Long, Set<Long>> peToPriv = new LinkedHashMap<Long, Set<Long>>();

        Connection connection;
        PreparedStatement ps;
        ResultSet rs;

        try {
            connection = s.connection();
            ps = SQLQueries.getQueryforUserPEPrivilegeMap(
                userId, this.getApplication().getApplicationId().intValue(), connection);
            rs = ps.executeQuery();

            while (rs.next()) {
                Long peId = rs.getLong(1);
                Long privId = rs.getLong(2);
                if (!peToPriv.containsKey(peId)) {
                    peToPriv.put(peId, new LinkedHashSet<Long>());
                }
                peToPriv.get(peId).add(privId);
            }
        } catch (SQLException e) {
            // TODO: make specific
            throw new RuntimeException(e);
        }
        // don't close the connection because it is borrowed from hibernate

        System.err.println("- getProtectionElementIdToPrivilegeIdsForUser took " + (System.currentTimeMillis() - startTime) + " ms for " + peToPriv.size() + " PEs");
        return peToPriv;
    }

    @SuppressWarnings({ "unchecked" })
    private Collection<Privilege> getPrivilegesByIds(Session s, Collection<Long> privilegeIds) {
        return getAllObjectsByIds(s, "Privilege", "id", privilegeIds);
    }

    @SuppressWarnings({ "unchecked" })
    private Collection<ProtectionElement> getProtectionElementsByIds(Session s, Collection<Long> protectionElementIds) {
        return getAllObjectsByIds(s, "ProtectionElement", "protectionElementId", protectionElementIds);
    }

    // TODO: will need to split up IN list if we use this in production
    private Collection getAllObjectsByIds(Session s, String className, String idName, Collection<Long> ids) {
        long startTime = System.currentTimeMillis();
        try {
            return s.createQuery("from " + className + " where " + idName + " in (:ids)").
                setParameterList("ids", ids).
                list();
        } finally {
            System.err.println("- loading " + className + "s for " + ids.size() + " ids took " +
                (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    protected SessionFactory getSessionFactory() {
        return sf;
    }
}
