package com.ucweb.gerrit.tools.converter;

import org.ini4j.Wini;

import java.io.File;
import java.sql.ResultSet;

public class GerritDatabaseConverter {
    public void loadConfig(String oldConf, String newConf) {
        try {
            // make path absolute
            String curDir = System.getProperty("user.dir");
            File oldFile = new File(oldConf);
            File newFile = new File(newConf);
            if (!oldFile.isAbsolute()) {
                oldFile = new File(new File(curDir), oldConf);
            }
            if (!newFile.isAbsolute()) {
                newFile = new File(new File(curDir), newConf);
            }
            String oldBaseDir = new File(oldFile.getParent()).getParent();

            Wini oldIni = new Wini(oldFile);
            Wini newIni = new Wini(newFile);
            String oldType = oldIni.get("database", "type");
            String newType = newIni.get("database", "type");

            if ("h2".equalsIgnoreCase(oldType)) {
                String dbPath = oldIni.get("database", "database");
                dbPath = new File(new File(oldBaseDir), dbPath).toString();
                h2Session = createH2DatabaseSession(dbPath);
                //h2Session.debugPrintTables();
            }

            if ("mysql".equalsIgnoreCase(newType)) {
                String dbName = newIni.get("database", "database");
                String dbUser = newIni.get("database", "username");
                String dbHost = newIni.get("database", "hostname");
                String dbPass = newIni.get("database", "password");
                mysqlSession = createMySQLDatabaseSession(dbName, dbUser, dbHost, dbPass);
                //mysqlSession.debugPrintTables();
            }
        } catch (Throwable e) {
            System.out.println("ERROR:Fail when loading gerrit config.");
            e.printStackTrace();
        }
    }

    public void transferData() {
        String[] dataTables = new String[]{
                "accounts",
                "account_external_ids",
                "account_groups",
                "account_group_by_id",
                "account_group_by_id_aud",
                "account_group_members",
                "account_group_members_audit",
                "account_group_names",
                "changes",
                "change_messages",
                "patch_comments",
                "patch_sets",
                "patch_set_approvals",
                "schema_version",
                "system_config",
//				"account_diff_preferences",
//				"account_patch_reviews",
//				"account_project_watches",
//				"account_ssh_keys",
//				"patch_set_ancestors",
//				"starred_changes",
//				"submodule_subscriptions"
        };
        try {
            System.out.println("Start transfer database for Gerrit 2.14.14.");
            for (String dataTable : dataTables) {
                ResultSet rs = h2Session.getTableContent(dataTable);
                mysqlSession.feedResultsToTable(rs, dataTable);
            }
            mysqlSession.fixIncrement();
            System.out.println("  Done transfer database data from H2 to MySQL.");
            System.out.println("  REMEMBER to update `groups' file in meta/config tag of `All-Project' to make Administrator UUID correct.");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void cleanup() {

        if (h2Session != null) {
            h2Session.cleanup();
        }
        if (mysqlSession != null) {
            mysqlSession.cleanup();
        }
    }

    private IDatabaseSession createMySQLDatabaseSession(String dbName,
                                                        String dbUser, String dbHost, String dbPass) throws Throwable {
        return new MySQLDatabaseSession(dbName, dbUser, dbHost, dbPass);
    }

    private IDatabaseSession createH2DatabaseSession(String dbPath) throws Throwable {
        return new H2DatabaseSession(dbPath);
    }

    IDatabaseSession h2Session = null;
    IDatabaseSession mysqlSession = null;
}
