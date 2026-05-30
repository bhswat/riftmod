package net.bhswat.wurm.riftmod;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.gotti.wurmunlimited.modsupport.ModSupportDb;

public class RiftModDatabase {
    private static Logger logger = Logger.getLogger(RiftModDatabase.class.getName());

    public static void onServerStarted() {
        try {
            Connection con = ModSupportDb.getModSupportDb();
            String sql;
            String tableName = "GlobalEvents";
            if (!ModSupportDb.hasTable(con, tableName)) {
                logger.info(tableName+" table not found in ModSupport. Creating table now.");
                sql = "CREATE TABLE "+tableName+" (ID INTEGER NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL DEFAULT '', STATE INT NOT NULL DEFAULT -1, TILEX INT NOT NULL, TILEY INT NOT NULL, STARTTIME LONG NOT NULL, ELAPSEDTIME LONG NOT NULL DEFAULT 0)";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.execute();
                ps.close();
            }
            tableName = "ObjectiveTimers";
            if (!ModSupportDb.hasTable(con, tableName)) {
                logger.info(tableName + " table not found in ModSupport. Creating table now.");
                sql = "CREATE TABLE " + tableName + " (ID VARCHAR(30) NOT NULL DEFAULT 'Unknown', TIMER LONG NOT NULL DEFAULT 0)";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.execute();
                ps = con.prepareStatement("INSERT OR IGNORE INTO ObjectiveTimers (ID, TIMER) VALUES(\"RIFT\", 0)");
                ps.execute();
                ps.close();
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static long getScheduledRiftTime() {
        long scheduledTime = 0;
        try {
            Connection dbcon = ModSupportDb.getModSupportDb();
            PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM ObjectiveTimers WHERE ID = \"RIFT\"");
            ResultSet rs = ps.executeQuery();
            scheduledTime = rs.getLong("TIMER");
            rs.close();
            ps.close();
            dbcon.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return scheduledTime;
    }

    public static void setNextRiftTime(long time) {
        try {
            Connection dbcon = ModSupportDb.getModSupportDb();
            PreparedStatement ps = dbcon.prepareStatement("UPDATE ObjectiveTimers SET TIMER = ? WHERE ID = \"RIFT\"");
            ps.setLong(1, time);
            ps.execute();
            ps.close();
            dbcon.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
