package net.bhswat.wurm.riftmod;

import org.gotti.wurmunlimited.modsupport.ModSupportDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RiftEventModel {
    Integer identity;
    Rift.State state;
    long scheduledTime;
    long elapsedTime;
    int tileX;
    int tileY;

    RiftEventModel(Integer id, Rift.State s, long st, long et, int x, int y) {
        identity = id;
        state = s;
        scheduledTime = st;
        elapsedTime = et;
        tileX = x;
        tileY = y;
    }

    public static List<RiftEventModel> getAllActive() {
        List<RiftEventModel> allActiveRifts = new ArrayList<>();
        try {
            Connection dbcon = ModSupportDb.getModSupportDb();
            PreparedStatement ps = dbcon.prepareStatement("SELECT * FROM GlobalEvents WHERE NAME = \"RIFT\" AND " + Fields.state.toString() + " < " + Rift.State.Finished.value, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RiftEventModel rift = new RiftEventModel(
                        rs.getInt(Fields.identity.toString()),
                        Rift.State.fromInt(rs.getInt(Fields.state.toString())),
                        rs.getLong(Fields.scheduledTime.toString()),
                        rs.getLong(Fields.elapsedTime.toString()),
                        rs.getInt(Fields.tileX.toString()),
                        rs.getInt(Fields.tileY.toString())
                );
                allActiveRifts.add(rift);
            }
            rs.close();
            ps.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return allActiveRifts;
    }

    public static RiftEventModel makeByScheduledTime(long time, int tileX, int tileY) {
        RiftEventModel rift = new RiftEventModel(null, Rift.State.Idle, time, 0L, tileX, tileY);
        rift.insert();
        return rift;
    }

    private void insert() {
        try {
            Connection dbcon = ModSupportDb.getModSupportDb();
            PreparedStatement ps = dbcon.prepareStatement("INSERT INTO GlobalEvents (NAME, " +
                    Fields.state.toString() + ", " +
                    Fields.scheduledTime.toString() + ", " +
                    Fields.elapsedTime.toString() + ", " +
                    Fields.tileX.toString() + ", " +
                    Fields.tileY.toString() +
                    ") VALUES(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, "RIFT");
            ps.setInt(2, state.value);
            ps.setLong(3, scheduledTime);
            ps.setLong(4, elapsedTime);
            ps.setInt(5, tileX);
            ps.setInt(6, tileY);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            identity = rs.getInt(1);
            rs.close();
            ps.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void synchronize() {
        try {
            Connection dbcon = ModSupportDb.getModSupportDb();
            PreparedStatement ps = dbcon.prepareStatement("UPDATE GlobalEvents SET " +
                    Fields.state.toString() + " = " + state.value + ", " +
                    Fields.scheduledTime.toString() + " = " + scheduledTime + ", " +
                    Fields.elapsedTime.toString() + " = " + elapsedTime + ", " +
                    Fields.tileX.toString() + " = " + tileX + ", " +
                    Fields.tileY.toString() + " = " + tileY +
                    " WHERE " + Fields.identity.toString() + " = " + identity, Statement.RETURN_GENERATED_KEYS);
            ps.executeUpdate();
            ps.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private enum Fields {
        identity {
            @Override
            public String toString() { return "ID"; }
        },
        state {
            @Override
            public String toString() { return "STATE"; }
        },
        scheduledTime {
            @Override
            public String toString() { return "STARTTIME"; }
        },
        elapsedTime {
            @Override
            public String toString() { return "ELAPSEDTIME"; }
        },
        tileX {
            @Override
            public String toString() { return "TILEX"; }
        },
        tileY {
            @Override
            public String toString() { return "TILEY"; }
        }
    }
}
