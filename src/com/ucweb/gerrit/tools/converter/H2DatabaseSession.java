package com.ucweb.gerrit.tools.converter;

import java.sql.*;

public class H2DatabaseSession implements IDatabaseSession {
    static boolean sDbInited = false;
    String dbPath;
    Connection conn;
    PreparedStatement stmt = null;

    public H2DatabaseSession(String dbPath) throws Throwable {
        super();
        if (!sDbInited) {
            sDbInited = true;
            Class.forName("org.h2.Driver");
        }
        this.dbPath = dbPath;

        conn = DriverManager.
                getConnection("jdbc:h2:" + dbPath);
    }

    @Override
    public void debugPrintTables() {
        if (conn == null)
            return;
        try {
            DatabaseMetaData m = conn.getMetaData();
            ResultSet tables = m.getTables(conn.getCatalog(), null, "%", null);
            while (tables.next()) {
                System.out.println("table = " + tables.getString(3));
            }
            tables.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public ResultSet getTableContent(String tableName) throws SQLException {
        cleanupResultStatment();
        PreparedStatement stmt = conn.prepareStatement(String.format("select * from %s", tableName));
        return stmt.executeQuery();
    }

    @Override
    public void feedResultsToTable(ResultSet resultSet, String tableName) {
        // TODO Auto-generated method stub

    }

    @Override
    public void fixIncrement() {
        // Do noting
    }

    @Override
    public void cleanup() {
        cleanupResultStatment();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void cleanupResultStatment() {
        if (stmt != null) {
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
