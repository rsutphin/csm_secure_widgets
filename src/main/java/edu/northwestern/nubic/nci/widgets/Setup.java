package edu.northwestern.nubic.nci.widgets;

import edu.northwestern.nubic.nci.widgets.domain.Widget;
import gov.nih.nci.security.authorization.domainobjects.Role;
import gov.nih.nci.security.authorization.domainobjects.User;
import gov.nih.nci.security.authorization.domainobjects.Privilege;
import gov.nih.nci.security.exceptions.CSObjectNotFoundException;
import gov.nih.nci.security.exceptions.CSTransactionException;
import org.hibernate.Session;

/**
 * @author Rhett Sutphin
 */
public class Setup {
    private String lastCode = null;

    private void buildWidgets(int count) {
        Session s = null;
        try {
            s = Persistence.getSession();
            long currentCount = (Long) s.createQuery("select count(*) from Widget").uniqueResult();
            if (currentCount == count) {
                System.out.println("There are already " + count + " widgets in the application store");
                return; 
            }
            s.createQuery("delete from Widget").executeUpdate();
            System.out.println("Inserting " + count + " widgets into application store");
            for (int i = 0; i < count; i++) {
                Widget w = new Widget(nextCode());
                s.save(w);
                if ((i + 1) % 1024 == 0) System.out.print(".");
                if ((i + 1) % (32 * 1024) == 0) {
                    System.out.println(" Saving up to " + (i + 1));
                    s.flush();
                    s.close();
                    s = Persistence.getSession();
                }
            }
            System.out.println(" Finishing");
            s.flush();
            s.close();
        } finally {
            if (s != null && s.isOpen()) s.close();
        }
    }

    private String nextCode() {
        String next;
        if (lastCode == null) {
            next = "A";
        } else {
            char[] chars = lastCode.toCharArray();
            boolean wrap = true;
            int i = chars.length - 1;
            while (wrap && i >= 0) {
                if (chars[i] == 'Z') {
                    chars[i] = 'A';
                    wrap = true;
                } else {
                    chars[i] += 1;
                    wrap = false;
                }
                i--;
            }
            next = new String(chars);
            if (wrap) { next = 'A' + next; }
        }
        lastCode = next;
        return next;
    }

    private void createUsers() throws CSTransactionException {
        createUser("jo");
        createUser("rho");
    }

    private void createUser(String username) throws CSTransactionException {
        if (CSM.getAuthorizationManager().getUser(username) == null) {
            System.out.println("Creating user " + username);
            User u = new User();
            u.setLoginName(username);
            CSM.getAuthorizationManager().createUser(u);
        } else {
            System.out.println("User " + username + " already exists");
        }
    }

    private void createRoles() throws CSTransactionException {
        createRole("LX");
        createRole("SE");
    }

    private void createRole(String roleName) throws CSTransactionException {
        try {
            Role actual = CSM.getAuthorizationDao().getRole(roleName);
            if (actual != null) { // in CSM 4.2, the exception isn't actually thrown
                System.out.println("Role " + roleName + " already exists");
                return;
            }
        } catch (CSObjectNotFoundException cso) {
            // fall through
        }
        System.out.println("Creating role " + roleName);
        Role r = new Role();
        r.setName(roleName);
        CSM.getAuthorizationManager().createRole(r);
        Privilege p = new Privilege();
        p.setName(roleName);
        p.setDesc("The lone priv for " + roleName);
        CSM.getAuthorizationManager().createPrivilege(p);
        CSM.getAuthorizationManager().addPrivilegesToRole(r.getId().toString(), new String[] { p.getId().toString() });
    }

    public void go() throws CSTransactionException, CSObjectNotFoundException {
        buildWidgets(512 * 1024);
        createUsers();
        createRoles();
    }

    public static void main(String[] args) throws Exception {
        new Setup().go();
    }
}
