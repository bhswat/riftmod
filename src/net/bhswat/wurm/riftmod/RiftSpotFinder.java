package net.bhswat.wurm.riftmod;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Point;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Terraforming;
import com.wurmonline.server.structures.*;
import com.wurmonline.server.villages.Villages;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;

import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;

public class RiftSpotFinder {
    private Random random = new Random();
    private int maxTriesCount = 100;
    private int maxHeightDiff = 400;
    private int defaultSpotX = 1000;
    private int defaultSpotY = 1000;
    private int possibleWaterTiles = 0;
    private int possibleRoadTiles = 0;
    private int possibleHeightTiles = 0;
    private int possibleBuildingTiles = 0;
    private int radius = 30;
    private int findTries = 0;
    public Point coordinates = null;

    public void configure(Properties properties) {
        maxTriesCount = Integer.parseInt(properties.getProperty("findspot.tries", Integer.toString(maxTriesCount)));
        maxHeightDiff = Integer.parseInt(properties.getProperty("findspot.maxHeightDiff", Integer.toString(maxHeightDiff)));
        defaultSpotX = Integer.parseInt(properties.getProperty("findspot.default.x", Integer.toString(defaultSpotX)));
        defaultSpotY = Integer.parseInt(properties.getProperty("findspot.default.y", Integer.toString(defaultSpotY)));
        possibleWaterTiles = Integer.parseInt(properties.getProperty("findspot.possibleWaterTiles", Integer.toString(possibleWaterTiles)));
        possibleRoadTiles = Integer.parseInt(properties.getProperty("findspot.possibleRoadTiles", Integer.toString(possibleRoadTiles)));
        possibleHeightTiles = Integer.parseInt(properties.getProperty("findspot.possibleHeightTiles", Integer.toString(possibleHeightTiles)));
        possibleBuildingTiles = Integer.parseInt(properties.getProperty("findspot.possibleBuildingTiles", Integer.toString(possibleBuildingTiles)));
        radius = Integer.parseInt(properties.getProperty("radius", Integer.toString(radius)));
    }

    public void findCoordinates() {
        findTries++;
        if (findTries > maxTriesCount) {
            RiftMod.logger.log(Level.INFO, "RIFT: Find spot tries count limit, use default coordinates.");
            coordinates = new Point(defaultSpotX, defaultSpotY);
            findTries = 0;
            return;
        }
        int x, y;
        int centerX, centerY;
        int padding = Zones.worldTileSizeX / 20;
        final int[] maxHeight = {0};
        final int[] minHeight = {0};
        // Gets random X, Y tile coordinates. Stays away from the server
        // border, 5 % tiles of the world's size.
        centerX = x = random.nextInt(Zones.worldTileSizeX - padding * 2) + padding;
        centerY = y = random.nextInt(Zones.worldTileSizeY - padding * 2) + padding;
        RiftMod.logger.log(Level.INFO, "RIFT: Attempt to find spot at " + centerX + "," + centerY);
        int currentRadius = 1;
        int tries = 0;
        while (currentRadius < radius) {
            if (tries > maxTriesCount) {
                RiftMod.logger.log(Level.INFO, "RIFT: Fail find spot at " + centerX + "," + centerY + " try random position next time.");
                return;
            }
            if (!isPossibleTile(x, y - currentRadius)) {
                y += (radius - currentRadius);
                currentRadius = 1;
                tries++;
                continue;
            }
            if (!isPossibleTile(x, y + currentRadius)) {
                y -= (radius - currentRadius);
                currentRadius = 1;
                tries++;
                continue;
            }
            if (!isPossibleTile(x - currentRadius, y)) {
                x += (radius - currentRadius);
                currentRadius = 1;
                tries++;
                continue;
            }
            if (!isPossibleTile(x + currentRadius, y)) {
                x -= (radius - currentRadius);
                currentRadius = 1;
                tries++;
                continue;
            }
            currentRadius++;
        }

        maxHeight[0] = Integer.MIN_VALUE;
        minHeight[0] = Integer.MAX_VALUE;

        int centerTile = Server.surfaceMesh.getTile(x, y);
        int leftTile = Server.surfaceMesh.getTile(x - radius, y);
        int rightTile = Server.surfaceMesh.getTile(x + radius, y);
        int topTile = Server.surfaceMesh.getTile(x, y - radius);
        int bottomtTile = Server.surfaceMesh.getTile(x, y + radius);
        int[] tileArray = {centerTile, leftTile, rightTile, topTile, bottomtTile};
        Arrays.stream(tileArray).forEach(tile -> {
            int height = Tiles.decodeHeight(tile);
            maxHeight[0] = Math.max(maxHeight[0], height);
            minHeight[0] = Math.min(minHeight[0], height);
        });
        if (maxHeight[0] - minHeight[0] > maxHeightDiff) {
            RiftMod.logger.log(Level.INFO, "RIFT: Fail find spot at " + centerX + "," + centerY + " max height diff " + (maxHeight[0] - minHeight[0]));
            return;
        }
        if (Villages.getVillageWithPerimeterAt(x - radius, y, true) != null ||
            Villages.getVillageWithPerimeterAt(x + radius, y, true) != null ||
            Villages.getVillageWithPerimeterAt(x, y - radius, true) != null ||
            Villages.getVillageWithPerimeterAt(x, y + radius, true) != null) {
            RiftMod.logger.log(Level.INFO, "RIFT: Fail find spot at " + centerX + "," + centerY + " village here");
            return;
        }

        float angle = 0;
        for (int sa = 0; sa < 360; sa += 15) {
            angle = (float) Math.ceil( (180 + sa) - angle / 360 ) % 360;
            if (traceBlockingSpot(angle, x, y)) {
                return;
            }
        }
        coordinates = new Point(x, y);
        findTries = 0;
    }

    private boolean traceBlockingSpot(float angle, int x, int y) {
        for (int i = 0; i < radius; i++) {
            int dx = x + (int) (radius / (i + 1) * Math.sin((double) angle / 180 * Math.PI));
            int dy = y + (int) (-radius / (i + 1) * Math.cos((double) angle / 180 * Math.PI));
            Structure structure = Structures.getStructureForTile(dx, dy, true);

            if (structure != null && (structure.isTypeHouse() || structure.isTypeBridge())) {
                RiftMod.logger.log(Level.INFO, "RIFT: Fail find spot at " + x + "," + y + " house or bridge found here.");
                return true;
            }
            if (structure != null && structure.getWalls().length > 0) {
                RiftMod.logger.log(Level.INFO, "RIFT: Fail find spot at " + x + "," + y + " walls found here.");
                return true;
            }
            if (!isPossibleTile(dx, dy)) {
                RiftMod.logger.log(Level.INFO, "RIFT: Fail find spot at " + x + "," + y + " road or water found here.");
                return true;
            }
        }
        return false;
    }

    private boolean isPossibleTile(int tileX, int tileY) {
        VolaTile t = Zones.getTileOrNull(tileX, tileY, true);
        if (t != null) {
            if (t.getWallsForLevel(0).length > 0) {
                RiftMod.logger.log(Level.INFO, "RIFT: Fail find spot at " + tileX + "," + tileY + " walls found here.");
                return false;
            }
            if (t.getFencesForLevel(0).length > 0) {
                RiftMod.logger.log(Level.INFO, "RIFT: Fail find spot at " + tileX + "," + tileY + " fence found here.");
                return false;
            }
        }
        int l = Server.surfaceMesh.getData().length;
        int s = Server.surfaceMesh.getSizeLevel();
        int i = tileX | tileY << s;
        if (i >= l) {
            return false;
        }
        int tile = Server.surfaceMesh.getTile(tileX, tileY);
        byte tileDocedeType = Tiles.decodeType(tile);
        if (Terraforming.isTileUnderWater(tile, tileX, tileY, true) || tileDocedeType == Tiles.Tile.TILE_LAVA.id) {
            return false;
        }
        return !Tiles.isRoadType(tile) && tileDocedeType != Tiles.Tile.TILE_FIELD.id && tileDocedeType != Tiles.Tile.TILE_FIELD2.id;
    }
}
