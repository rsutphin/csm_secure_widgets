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
import org.hibernate.Query;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
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
        return getWidgets(widgetCodes, Integer.MAX_VALUE);
    }

    private Collection<Widget> getWidgets(Collection<String> widgetCodes, int clauseSize) {
        long startTime = System.currentTimeMillis();
        Session s = Persistence.getSession();
        try {
            List<Widget> result =
                createSplitInQuery(s, "from Widget w where ", "w.code", clauseSize, widgetCodes).list();
            System.err.println(
                "getWidgets() took " + (System.currentTimeMillis() - startTime) + "ms for " + result.size() + " items");
            return result;
        } finally {
            if (s.isOpen()) s.close();
        }
    }

    private Query createSplitInQuery(Session s, String baseQuery, String inParam, int splitAmount, Collection<String> parameterValues) {
        List<String> values = new ArrayList<String>(parameterValues);
        List<List<String>> split = new LinkedList<List<String>>();
        int splitCt = 0;
        while (splitCt < values.size()) {
            int upper = Math.min(values.size(), splitCt + splitAmount);
            List<String> subValues = values.subList(splitCt, upper);
            split.add(subValues);
            splitCt += splitAmount;
        }

        StringBuilder qs = new StringBuilder(baseQuery).append('(');
        for (int i = 0; i < split.size(); i++) {
            qs.append(inParam).append(" in (:c").append(i).append(')');
            if (i < split.size() - 1) {
                qs.append(" OR ");
            }
        }
        qs.append(')');

        Query q = s.createQuery(qs.toString());
        for (ListIterator<List<String>> lit = split.listIterator(); lit.hasNext();) {
            List<String> block = lit.next();
            int i = lit.previousIndex();
            q.setParameterList("c" + i, block);
        }
        return q;
    }

    public Collection<Widget> getVisibleWidgets(String username, String roleName) throws CSObjectNotFoundException {
        long startTime = System.currentTimeMillis();
        try {
            User jo = CSM.getAuthorizationManager().getUser(username);
            Set<String> widgetCodes = new LinkedHashSet<String>();
            Set<ProtectionElementPrivilegeContext> contexts;
            {
                long csmStartTime = System.currentTimeMillis();
                contexts = CSM.getAuthorizationManager().
                    getProtectionElementPrivilegeContextForUser(jo.getUserId().toString());
                System.err.println("getProtectionElementPrivilegeContextForUser() took " + (System.currentTimeMillis() - csmStartTime) + "ms for " + contexts.size() + " contexts");
            }
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
            System.err.println("getVisibleWidgets took " + (System.currentTimeMillis() - startTime) + "ms total");
        }
    }

    public void runAssociatedWidgetsTest() throws Exception {
        runAssociatedWidgetsTest(26 * 26);
    }

    public void runAssociatedWidgetsTest(int lxCt) throws Exception {
        associate("jo", "Reader", manyCodes(lxCt).toArray(new String[lxCt]));
        associate("jo", "Editor", "SEE", "SEA", "SEAL", "SELX");

        Collection<Widget> all = getVisibleWidgets("jo");
        System.out.println("Jo can see these widgets " + all);
        System.out.println("  (" + all.size() + " total)");
        System.out.println("  As Editor, it's these: " + getVisibleWidgets("jo", "Editor"));
    }

    public Collection<Widget> runGetLotsOfWidgetsTest(int count, int clauseSize) {
        return getWidgets(manyCodes(count), clauseSize);
    }

    private Collection<String> manyCodes(int count) {
        Collection<String> manyCodes = new ArrayList<String>();
        for (char i = 'A'; i <= 'Z' && manyCodes.size() < count ; i++) {
            for (char j = 'A'; j <= 'Z' && manyCodes.size() < count ; j++) {
                for (char k = 'A'; k <= 'Z' && manyCodes.size() < count ; k++) {
                    manyCodes.add("" + i + j + k);
                }
            }
        }
        return manyCodes;
    }

    public static void main(final String[] args) throws Exception {
        new Setup().go();

        Main m = new Main();
        m.runAssociatedWidgetsTest(200);
    }
}
