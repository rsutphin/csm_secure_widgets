package edu.northwestern.nubic.nci.widgets.csmext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Rhett Sutphin
 */
// Deliberately patterned after CSM's Queries 
public class SQLQueries {
    /**
     * This is identical to {@link gov.nih.nci.security.dao.Queries#getQueryforUserPEPrivilegeMap}, except that it drops
     * support for parent protection groups.  In informal testing, this results in a 30x speedup.
     *
     * @param user_id
     * @param application_id
     * @param cn
     * @return
     * @throws SQLException
     */
    protected static PreparedStatement getQueryforUserPEPrivilegeMap(String user_id, int application_id, Connection cn) throws SQLException {
        StringBuffer stbr = new StringBuffer();

        stbr.append("SELECT DISTINCT pe.protection_element_id as pe_id, p.privilege_id as p_id");
        stbr.append("      FROM  csm_protection_element pe,");
        stbr.append("            csm_protection_group pg,");
        stbr.append("            csm_privilege p,");
        stbr.append("            csm_group g,");
        stbr.append("            csm_user_group ug,");
        stbr.append("            csm_user u,");
        stbr.append("            csm_pg_pe pgpe,");
        stbr.append("            csm_role r,");
        stbr.append("            csm_role_privilege rp,");
        stbr.append("            csm_user_group_role_pg ugrpg");
        stbr.append("      WHERE pgpe.protection_group_id = ugrpg.protection_group_id");
        stbr.append("            AND ugrpg.role_id = rp.role_id");
        stbr.append("            AND rp.privilege_id = p.privilege_id");
        stbr.append("            AND pg.protection_group_id = pgpe.protection_group_id");
        stbr.append("            AND pg.application_id=?");
        stbr.append("            AND pe.application_id=?");
        stbr.append("            AND pgpe.protection_element_id = pe.protection_element_id");
        stbr.append("            AND ug.group_id = ugrpg.group_id");
        stbr.append("            AND ug.user_id = ?");
        stbr.append(" UNION ALL ");
        stbr.append("SELECT DISTINCT pe.protection_element_id as pe_id, p.privilege_id as p_id");
        stbr.append("      FROM  csm_protection_element pe,");
        stbr.append("            csm_protection_group pg,");
        stbr.append("            csm_privilege p,");
        stbr.append("            csm_user u,");
        stbr.append("            csm_pg_pe pgpe,");
        stbr.append("            csm_role r,");
        stbr.append("            csm_role_privilege rp,");
        stbr.append("            csm_user_group_role_pg ugrpg");
        stbr.append("      WHERE pgpe.protection_group_id = ugrpg.protection_group_id");
        stbr.append("            AND ugrpg.role_id = rp.role_id");
        stbr.append("            AND rp.privilege_id = p.privilege_id");
        stbr.append("            AND pg.protection_group_id = pgpe.protection_group_id");
        stbr.append("            AND pg.application_id=?");
        stbr.append("            AND pe.application_id=?");
        stbr.append("            AND pgpe.protection_element_id = pe.protection_element_id");
        stbr.append("            AND ugrpg.user_id = ?");
        stbr.append(" UNION ALL ");
        stbr.append("SELECT DISTINCT upe.protection_element_id as pe_id, 0 as p_id");
        stbr.append("      FROM csm_user_pe upe, csm_protection_element cpe");
        stbr.append("      WHERE cpe.protection_element_id = upe.protection_element_id ");
        stbr.append("      and upe.user_id = ?");
        stbr.append("      and cpe.application_id = ?");
        stbr.append(" ORDER BY pe_id, p_id");


        int i = 1;
        PreparedStatement pstmt = cn.prepareStatement(stbr.toString());
        pstmt.setInt(i++, application_id);
        pstmt.setInt(i++, application_id);
        pstmt.setInt(i++, new Integer(user_id));
        pstmt.setInt(i++, application_id);
        pstmt.setInt(i++, application_id);
        pstmt.setInt(i++, new Integer(user_id));
        pstmt.setInt(i++, new Integer(user_id));
        pstmt.setInt(i++, application_id);
        return pstmt;
    }
}
