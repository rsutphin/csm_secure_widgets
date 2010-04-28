package edu.northwestern.nubic.nci.widgets;

import edu.northwestern.nubic.nci.widgets.domain.Widget;
import gov.nih.nci.security.authorization.domainobjects.Privilege;
import gov.nih.nci.security.authorization.domainobjects.ProtectionElement;
import gov.nih.nci.security.authorization.domainobjects.ProtectionElementPrivilegeContext;
import gov.nih.nci.security.authorization.domainobjects.ProtectionGroup;
import gov.nih.nci.security.authorization.domainobjects.Role;
import gov.nih.nci.security.authorization.domainobjects.User;
import gov.nih.nci.security.dao.ProtectionElementSearchCriteria;
import gov.nih.nci.security.dao.ProtectionGroupSearchCriteria;
import gov.nih.nci.security.exceptions.CSObjectNotFoundException;
import gov.nih.nci.security.exceptions.CSTransactionException;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Rhett Sutphin
 */
@SuppressWarnings({ "unchecked", "RawUseOfParameterizedType" })
public class Main {
    private void associate(String username, String roleName, String... widgetCodes) throws CSTransactionException {
        User user = CSM.getAuthorizationManager().getUser(username);
        user.setProtectionGroupRoleContexts(new HashSet());
        Role role = getRole(roleName);

        for (String code : widgetCodes) {
            ProtectionElement widgetProtection = getOrCreateWidgetPE(code);
            ProtectionGroup group = getOrCreateSingleElementPG(widgetProtection);
            CSM.getAuthorizationManager().addUserRoleToProtectionGroup(
                user.getUserId().toString(), new String[] { role.getId().toString() }, group.getProtectionGroupId().toString());
        }

        System.out.println("Associated " + username + " with " + Arrays.asList(widgetCodes) + " in role " + roleName);
    }

    private ProtectionElement getOrCreateWidgetPE(String widgetCode) throws CSTransactionException {
        ProtectionElement example = new ProtectionElement();
        example.setProtectionElementType(Widget.class.getName());
        example.setValue(widgetCode);
        example.setObjectId(Widget.class.getName() + '.' + widgetCode);

        List matches = CSM.getAuthorizationManager().getObjects(new ProtectionElementSearchCriteria(example));
        if (matches.isEmpty()) {
            CSM.getAuthorizationManager().createProtectionElement(example);
            assert example.getProtectionElementId() != null;
            return example;
        } else {
            return (ProtectionElement) matches.get(0);
        }
    }

    private ProtectionGroup getOrCreateSingleElementPG(ProtectionElement pe) throws CSTransactionException {
        ProtectionGroup group = new ProtectionGroup();
        group.setProtectionGroupName("Widget " + pe.getValue()); // search only works by name

        List matches = CSM.getAuthorizationManager().getObjects(new ProtectionGroupSearchCriteria(group));
        if (matches.isEmpty()) {
            CSM.getAuthorizationManager().createProtectionGroup(group);
            CSM.getAuthorizationManager().assignProtectionElement(group.getProtectionGroupName(), pe.getObjectId());
            return group;
        } else {
            return (ProtectionGroup) matches.get(0);
        }
    }

    private Role getRole(String roleName) {
        try {
            return CSM.getAuthorizationDao().getRole(roleName);
        } catch (CSObjectNotFoundException cso) {
            System.out.println("No role " + roleName);
            return null;
        }
    }

    public Collection<Widget> getVisibleWidgets(String username) throws CSObjectNotFoundException {
       return getVisibleWidgets(username, null);
    }

    private Collection<Widget> getWidgets(Collection<String> widgetCodes) {
        Session s = Persistence.getSession();
        try {
            return s.createQuery("from Widget w where w.code in (:codes)").
                setParameterList("codes", new ArrayList<String>(widgetCodes)).list();
        } finally {
            if (s.isOpen()) s.close();
        }
    }

    public Collection<Widget> getVisibleWidgets(String username, String roleName) throws CSObjectNotFoundException {
        long startTime = System.currentTimeMillis();
        try {
            User jo = CSM.getAuthorizationManager().getUser(username);
            Set<String> widgetCodes = new LinkedHashSet<String>();
            Set<ProtectionElementPrivilegeContext> contexts = CSM.getAuthorizationManager().
                getProtectionElementPrivilegeContextForUser(jo.getUserId().toString());
            for (ProtectionElementPrivilegeContext context : contexts) {
                boolean include;
                if (roleName != null) {
                    include = false;
                    Set<Privilege> privs = context.getPrivileges();
                    for (Privilege p : privs) {
                        include |= p.getName().equals(roleName);
                    }
                } else {
                    include = true;
                }

                if (include) {
                    widgetCodes.add(context.getProtectionElement().getValue());
                }
            }
            return getWidgets(widgetCodes);
        } finally {
            System.err.println("Query took " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    public static void main(final String[] args) throws Exception {
        new Setup().go();

        Main m = new Main();
        m.associate("jo", "LX", "ALX", "BLX", "VLX", "SELX");
        m.associate("jo", "SE", "SEE", "SEA", "SEAL", "SELX");

        System.out.println("Jo can see these widgets " + m.getVisibleWidgets("jo"));
        System.out.println("  As LX, it's these: " + m.getVisibleWidgets("jo", "LX"));
    }
}
