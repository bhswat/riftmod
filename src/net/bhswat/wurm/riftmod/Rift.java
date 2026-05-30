package net.bhswat.wurm.riftmod;

import java.util.*;
import java.util.logging.Level;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.*;
import com.wurmonline.server.behaviours.CreatureBehaviour;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.skills.NoSuchSkillException;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;

public class Rift
{
    RiftEventModel event;
    RiftSpawner spawner;

    private int delayPoint = 0;

    Item beam = null;
    ArrayList<Creature> creatures = new ArrayList();
    private ArrayList<Item> decorations = new ArrayList();

    private long nextBellTime;
    private long expiredTime;
    private long elapsedTime;

    Rift(RiftEventModel model) {
        event = model;
        if (event.state != State.Idle) {
            showBeam();
            restoreItems();
            elapsedTime = event.elapsedTime;
        }
        expiredTime = RiftMod.END_DELAY;
        RiftMod.logger.log(Level.INFO, "RIFT with id: " + event.identity + " has been created in location (" + event.tileX + "," + event.tileY + ").");
        System.out.println("Rift has been created");
    }

    private static void createCoins(Creature targetCret, int copperCount) {
        if (copperCount > 0) {
            int[] coins = new int[4];
            if (copperCount / 100.0F > 0.0F) {
                coins[0] = ((int)(copperCount / 100.0F));
                copperCount -= coins[0] * 100;
            }
            if (copperCount / 20.0F > 0.0F) {
                coins[1] = ((int)(copperCount / 20.0F));
                copperCount -= coins[1] * 20;
            }
            if (copperCount / 5.0F > 0.0F) {
                coins[2] = ((int)(copperCount / 5.0F));
                copperCount -= coins[2] * 5;
            }
            coins[3] = copperCount;
            try
            {
                for (int i = 0; i < coins[0]; i++) {
                    Item ex = ItemFactory.createItem(52, 100.0F, null);
                    if (targetCret != null) {
                        targetCret.getInventory().insertItem(ex, true);
                    }
                }
                for (int i = 0; i < coins[1]; i++) {
                    Item ex = ItemFactory.createItem(58, 100.0F, null);
                    if (targetCret != null) {
                        targetCret.getInventory().insertItem(ex, true);
                    }
                }
                for (int i = 0; i < coins[2]; i++) {
                    Item ex = ItemFactory.createItem(54, 100.0F, null);
                    if (targetCret != null) {
                        targetCret.getInventory().insertItem(ex, true);
                    }
                }
                for (int i = 0; i < coins[3]; i++) {
                    Item ex = ItemFactory.createItem(50, 100.0F, null);
                    if (targetCret != null) {
                        targetCret.getInventory().insertItem(ex, true);
                    }
                }
            } catch (FailedException|NoSuchTemplateException e) { e.printStackTrace();
            }
        }
    }

    private static void setTile(int x, int y, byte id) { setTile(x, y, id, false); }

    private static void setTile(int x, int y, byte id, boolean withoutcond) {
        int tile = Server.surfaceMesh.getTile(x, y);
        byte oldType = Tiles.decodeType(tile);
        if ((canChangeTile(oldType)) || (withoutcond)) {
            int rocktile = Server.rockMesh.getTile(x, y);
            short newHeight = (short)Math.max(Tiles.decodeHeight(rocktile), Tiles.decodeHeight(tile));
            if (newHeight > 0)
            {
                Server.setSurfaceTile(x, y, newHeight, id, (byte)0);
                com.wurmonline.server.Players.getInstance().sendChangedTile(x, y, true, true);
            }
        }
    }

    private static boolean canChangeTile(byte id) {
        byte tmp = 0;
        for (int i = 0; i <= 20; i++) {
            switch (i) {
                case 0:  tmp = Tiles.Tile.TILE_SLATE_SLABS.id; break;
                case 1:  tmp = Tiles.Tile.TILE_ROCK.id; break;
                case 2:  tmp = Tiles.Tile.TILE_DIRT_PACKED.id; break;
                case 3:  tmp = Tiles.Tile.TILE_COBBLESTONE.id; break;
                case 4:  tmp = Tiles.Tile.TILE_COBBLESTONE.id; break;
                case 5:  tmp = Tiles.Tile.TILE_COBBLESTONE_ROUGH.id; break;
                case 6:  tmp = Tiles.Tile.TILE_COBBLESTONE_ROUND.id; break;
                case 7:  tmp = Tiles.Tile.TILE_CAVE_EXIT.id; break;
                case 8:  tmp = Tiles.Tile.TILE_MINE_DOOR_WOOD.id; break;
                case 9:  tmp = Tiles.Tile.TILE_MINE_DOOR_STONE.id; break;
                case 10:  tmp = Tiles.Tile.TILE_CLAY.id; break;
                case 11:  tmp = Tiles.Tile.TILE_SAND.id; break;
                case 12:  tmp = Tiles.Tile.TILE_KELP.id; break;
                case 13:  tmp = Tiles.Tile.TILE_FIELD.id; break;
                case 14:  tmp = Tiles.Tile.TILE_MARBLE_SLABS.id; break;
                case 15:  tmp = Tiles.Tile.TILE_HOLE.id; break;
                case 16:  tmp = Tiles.Tile.TILE_REED.id; break;
                case 17:  tmp = Tiles.Tile.TILE_TAR.id; break;
                case 18:  tmp = Tiles.Tile.TILE_CLIFF.id; break;
                case 19:  tmp = Tiles.Tile.TILE_PLANKS.id; break;
                case 20:  tmp = Tiles.Tile.TILE_PLANKS_TARRED.id;
            }

            if (id == tmp)
                return false;
        }
        return true;
    }

    public void processRiftTick() {
        long timetoRift;
        long numberOfDays;
        long numberOfHours;
        long numberOfMinutes;
        RiftMod.logger.log(Level.INFO, "RIFT[" + event.identity + "] tick: state: " + event.state.name() + " time: " + event.elapsedTime);

        switch (event.state) {
            case Idle:
                timetoRift = RiftMod.FIRST_DELAY - elapsedTime;
                numberOfDays = timetoRift / 86400;
                numberOfHours = (timetoRift % 86400) / 3600;
                numberOfMinutes = ((timetoRift % 86400) % 3600) / 60;
                RiftMod.sendEventChatMessage("Разлом откроется через " + numberOfHours + "ч. " + numberOfMinutes + "мин.", 255, 200, 0);
                nextBellTime = System.currentTimeMillis() + 15 * TimeConstants.MINUTE_MILLIS;
                playBellSound();
                showBeam();
                event.state = State.Beam;
                event.synchronize();
                break;
            case Beam:
                if (nextBellTime < System.currentTimeMillis() && elapsedTime < RiftMod.BELL_TIME) {
                    timetoRift = RiftMod.FIRST_DELAY - elapsedTime;
                    numberOfDays = timetoRift / 86400;
                    numberOfHours = (timetoRift % 86400) / 3600;
                    numberOfMinutes = ((timetoRift % 86400) % 3600) / 60;
                    RiftMod.sendEventChatMessage("Разлом откроется через " + numberOfHours + "ч. " + numberOfMinutes + "мин.", 255, 200, 0);
                    nextBellTime = System.currentTimeMillis() + 15 * TimeConstants.MINUTE_MILLIS;
                    playBellSound();
                }
                if (elapsedTime >= RiftMod.FIRST_DELAY) {
                    event.state = State.FirstWave;
                    event.synchronize();
                }
                break;
            case FirstWave:
                if (nextBellTime < System.currentTimeMillis() && elapsedTime < RiftMod.BELL_TIME) {
                    RiftMod.sendEventChatMessage("Разлом между мирами открылся и мир наполнился жуткими тварями!", 255, 200, 0);
                    nextBellTime = System.currentTimeMillis() + 15 * TimeConstants.MINUTE_MILLIS;
                    playBellSound();
                }
                if (spawner == null) {
                    RiftMod.sendEventChatMessage("Начинается первая волна.", 255, 200, 0);
                    createRiftArea();
                    spawner = new RiftSpawner(this, RiftMod.stageList[0], RiftMod.SPAWNER_TICK);
                }
                finishWaveIfNeed();
                event.elapsedTime += RiftMod.TICK;
                break;
            case SecondWave:
                if (nextBellTime < System.currentTimeMillis() && elapsedTime < RiftMod.BELL_TIME) {
                    nextBellTime = System.currentTimeMillis() + 15 * TimeConstants.MINUTE_MILLIS;
                    playBellSound();
                }
                if (spawner == null) {
                    RiftMod.sendEventChatMessage("Начинается вторая волна.", 255, 200, 0);
                    spawner = new RiftSpawner(this, RiftMod.stageList[1], RiftMod.SPAWNER_TICK);
                }
                finishWaveIfNeed();
                event.elapsedTime += RiftMod.TICK;
                break;
            case Boss:
                if (spawner == null) {
                    RiftMod.sendEventChatMessage("Начинается третья волна.", 255, 200, 0);
                    spawner = new RiftSpawner(this, RiftMod.stageList[2], RiftMod.SPAWNER_TICK);
                }
                if (spawner.isFinish && getCreatureNumber() == 0) {
                    finishSpawner();
                    RiftMod.sendEventChatMessage("Разлом закрыт! Участвующие игроки получили награду!", 255, 200, 0);
                    event.state = State.Reward;
                    event.synchronize();
                }
                event.elapsedTime += RiftMod.TICK;
                break;
            case Reward:
                sendAward();
                sendMessage("Проверьте награду в инвентаре.");
                sendMessage("Теперь вы можете собрать ресурсы.");
                expiredTime = RiftMod.FINISH_DELAY + elapsedTime;
                event.state = State.Gathering;
                event.synchronize();
                break;
            default:
                break;
        }
        elapsedTime += RiftMod.TICK;

        if (elapsedTime >= expiredTime) {
            RiftMod.logger.log(Level.INFO, "RIFT[" + event.identity + "]: Time expired");
            RiftMod.sendEventChatMessage("Время вышло! Разлом между мирами закрылся!", 255, 200, 0);
            destroyRift(event.state.value > State.Boss.value);
        }
    }

    private void finishSpawner() {
        if (spawner != null) {
            spawner.reset();
        }
        spawner = null;
    }

    private void finishWaveIfNeed() {
        int num = getCreatureNumber();
        if (num == 0 && spawner != null && spawner.isFinish) {
            if (delayPoint == 0) {
                delayPoint = (int) (event.elapsedTime + RiftMod.WAVE_DELAY);
                sendMessage((event.state == State.FirstWave ? "Первая" : "Вторая") + " волна окончена.");
            }
            if (event.elapsedTime >= delayPoint) {
                delayPoint = 0;
                finishSpawner();
                event.state = event.state == State.FirstWave ? State.SecondWave : State.Boss;
                event.synchronize();
            }
        }
    }

    private ArrayList<Creature> getPlayerInRadius(int aTileX, int aTileY, int aRadius) {
        ArrayList<Creature> players = new ArrayList();

        int sx = Zones.safeTileX(aTileX - aRadius);
        int ex = Zones.safeTileX(aTileX + aRadius);
        int sy = Zones.safeTileY(aTileY - aRadius);
        int ey = Zones.safeTileY(aTileY + aRadius);
        for (int x = sx; x <= ex; x++) {
            for (int y = sy; y <= ey; y++) {
                VolaTile t = Zones.getTileOrNull(x, y, true);
                if (t != null) {
                    Creature[] crets = t.getCreatures();
                    for (int i = 0; i < crets.length; i++) {
                        Creature target = crets[i];
                        if (target.isPlayer()) {
                            players.add(target);
                        }
                    }
                }
            }
        }
        return players;
    }

    private void sendMessage(String message) {
        ArrayList<Creature> players = getPlayerInRadius(event.tileX, event.tileY, RiftMod.RADIUS);
        if (players == null)
            return;
        if (players.size() > 0)
            for (int i = 0; i < players.size(); i++)
                ((Creature)players.get(i)).getCommunicator().sendNormalServerMessage("RIFT: " + message);
    }

    private void sendAward() {
        ArrayList<Creature> players = getPlayerInRadius(event.tileX, event.tileY, RiftMod.RADIUS);
        if (players.size() > 0)
            for (int i = 0; i < players.size(); i++)
            {

                try
                {
                    int id = 694;
                    switch (Server.rand.nextInt(3)) {
                        case 0:  id = 694; break;
                        case 1:  id = 698; break;
                        case 2:  id = 837;
                    }
                    Item lump = ItemFactory.createItem(id, 20 + Server.rand.nextInt(80) + Server.rand.nextFloat(), null);
                    lump.setRarity((byte)1);

                    if (Server.rand.nextInt(1000) < 20) {
                        lump.setRarity((byte)2);
                    }
                    lump.setWeight(RiftMod.LUMP_WEIGHT, true);
                    ((Creature)players.get(i)).getInventory().insertItem(lump, true);
                } catch (FailedException|NoSuchTemplateException e) { e.printStackTrace();
                }

                try
                {
                    int fs = (int)((Creature)players.get(i)).getSkills().getSkill(1023).getKnowledge();
                    int coins = 0;
                    if (fs >= RiftMod.normalFs) {
                        coins += RiftMod.normalCoins;
                    }
                    if (fs >= RiftMod.addFs) {
                        coins += RiftMod.addCoins;
                    }
                    if (coins > 0) {
                        createCoins((Creature)players.get(i), coins);
                    }
                }
                catch (NoSuchSkillException localNoSuchSkillException) {}

                if (Server.rand.nextInt(100) < RiftMod.MASK_CHANCE) {
                    try {
                        int id = 973;
                        switch (Server.rand.nextInt(8)) {
                            case 0:  id = 973; break;
                            case 1:  id = 974; break;
                            case 2:  id = 975; break;
                            case 3:  id = 976; break;
                            case 4:  id = 977; break;
                            case 5:  id = 978; break;
                            case 6:  id = 1306; break;
                            case 7:  id = 1099;
                        }
                        Item mask = ItemFactory.createItem(id, 20 + Server.rand.nextInt(80) + Server.rand.nextFloat(), null);

                        if (Server.rand.nextInt(1000) < 1) {
                            mask.setRarity((byte)3);
                        }
                        else if (Server.rand.nextInt(1000) < 10) {
                            mask.setRarity((byte)2);
                        }
                        else if (Server.rand.nextInt(1000) < 100) {
                            mask.setRarity((byte)1);
                        }
                        RiftMod.logger.log(Level.INFO, "RIFT[" + event.identity + "]: award: mask: player: [" + ((Creature)players.get(i)).getNameWithoutPrefixes() + "] ql/rarity: [" + mask.getQualityLevel() + "/" + mask.getRarity() + "]");
                        ((Creature)players.get(i)).getInventory().insertItem(mask, true);
                    } catch (FailedException|NoSuchTemplateException e) { e.printStackTrace();
                    }
                }


                if (Server.rand.nextInt(100) < RiftMod.SHOULDER_CHANCE) {
                    try {
                        int id = 1049 + Server.rand.nextInt(22);
                        if (id > 1066) {
                            id = id - 1067 + 1092;
                        }
                        Item shoulder = ItemFactory.createItem(id, 20 + Server.rand.nextInt(80) + Server.rand.nextFloat(), null);

                        if (Server.rand.nextInt(1000) < 1) {
                            shoulder.setRarity((byte)3);
                        }
                        else if (Server.rand.nextInt(1000) < 10) {
                            shoulder.setRarity((byte)2);
                        }
                        else if (Server.rand.nextInt(1000) < 100) {
                            shoulder.setRarity((byte)1);
                        }
                        RiftMod.logger.log(Level.INFO, "RIFT[" + event.identity + "]: award: shoulder: player: [" + ((Creature)players.get(i)).getNameWithoutPrefixes() + "] ql/rarity: [" + shoulder.getQualityLevel() + "/" + shoulder.getRarity() + "]");
                        ((Creature)players.get(i)).getInventory().insertItem(shoulder, true);
                    } catch (FailedException|NoSuchTemplateException e) { e.printStackTrace();
                    }
                }


                if (Server.rand.nextInt(100) < RiftMod.ACCESSORY_CHANCE)
                    try {
                        int id = 1076;

                        switch (Server.rand.nextInt(10)) {
                            case 0:  id = 1076; break;
                            case 1:  id = 1077; break;
                            case 2:  id = 1078; break;
                            case 3:  id = 1079; break;
                            case 4:  id = 1080; break;
                            case 5:  id = 1086; break;
                            case 6:  id = 1087; break;
                            case 7:  id = 1088; break;
                            case 8:  id = 1089; break;
                            case 9:  id = 1090;
                        }

                        Item accessory = ItemFactory.createItem(id, 20 + Server.rand.nextInt(80) + Server.rand.nextFloat(), null);

                        if (Server.rand.nextInt(1000) < 1) {
                            accessory.setRarity((byte)3);
                        }
                        else if (Server.rand.nextInt(1000) < 10) {
                            accessory.setRarity((byte)2);
                        }
                        else if (Server.rand.nextInt(1000) < 100)
                            accessory.setRarity((byte)1);
                        RiftMod.logger.log(Level.INFO, "RIFT[" + event.identity + "]: award: accessory: player: [" + ((Creature)players.get(i)).getNameWithoutPrefixes() + "] ql/rarity: [" + accessory.getQualityLevel() + "/" + accessory.getRarity() + "]");
                        ((Creature)players.get(i)).getInventory().insertItem(accessory, true);
                    } catch (FailedException|NoSuchTemplateException e) { e.printStackTrace();
                    }
            }
    }

    private void showBeam()
    {
        try
        {
            Item beacon = ItemFactory.createItem(344, 99.0F, event.tileX * 4 + 2, event.tileY * 4 + 2, Server.rand.nextInt(360), true, (byte)0, -10L, null);
            beacon.setIsNoTake(true);
            beam = beacon;
            Player[] players = Players.getInstance().getPlayers();
            if (players.length != 0) {
                for (int i = 0; i < players.length; i++) {
                    if (players[i] != null)
                        players[i].getCommunicator().sendAddEffect(beacon.getWurmId(), (short)25, event.tileX * 4 + 2, event.tileY * 4 + 2, 0.0F, (byte)0);
                }
            }
        } catch (NoSuchTemplateException|FailedException e) {
            e.printStackTrace();
        }
    }

    private void playBellSound()
    {
        Player[] players = Players.getInstance().getPlayers();
        if (players.length != 0) {
            for (int i = 0; i < players.length; i++) {
                if (players[i] != null)
                    com.wurmonline.server.sounds.SoundPlayer.playSound("sound.bell.dong.1", players[i], 3.0F);
            }
        }
    }

    private void createRiftArea() {
        int[] ellipseTop = new int[RiftMod.RADIUS * 2];
        int[] ellipseBot = new int[RiftMod.RADIUS * 2];
        for (int i = -RiftMod.RADIUS; i < RiftMod.RADIUS; i++) {
            int p = i + RiftMod.RADIUS;
            ellipseTop[p] = ((int)Math.sqrt(Math.pow(RiftMod.RADIUS, 2.0D) - Math.pow(i, 2.0D) * Math.pow(RiftMod.RADIUS, 2.0D) / Math.pow(RiftMod.RADIUS, 2.0D)));
            ellipseBot[p] = (-ellipseTop[p]);
            if (p <= RiftMod.RADIUS) {
                if (Server.rand.nextInt(p == 0 ? 1 : p) == 0) {
                    ellipseTop[p] -= Server.rand.nextInt(3);
                    ellipseBot[p] += Server.rand.nextInt(3);
                }

            }
            else if (Server.rand.nextInt(p - RiftMod.RADIUS == 0 ? 1 : p - RiftMod.RADIUS) == 0) {
                ellipseTop[p] -= Server.rand.nextInt(3);
                ellipseBot[p] += Server.rand.nextInt(3);
            }
            ellipseTop[p] += event.tileY;
            ellipseBot[p] += event.tileY;
        }
        for (int i = event.tileX - RiftMod.RADIUS; i < event.tileX + RiftMod.RADIUS; i++) {
            setTileCol(i, ellipseBot[(i - (event.tileX - RiftMod.RADIUS))], ellipseTop[(i - (event.tileX - RiftMod.RADIUS))]);
        }
    }

    private void setTileCol(int x, int sy, int ey)
    {
        for (int i = sy; i <= ey; i++) {
            if (Server.rand.nextInt(100) < 60) {
                byte id = Tiles.Tile.TILE_DIRT.id;
                if (Server.rand.nextInt(100) < 20) {
                    id = Tiles.Tile.TILE_DIRT_PACKED.id;
                }
                setTile(x, i, id);
            }
            if ((x == event.tileX) && (i == event.tileY)) {
                createRiftDecoration(x, i, 1045);
            }
            else if (Server.rand.nextInt(100) < RiftMod.DECORATION_CHANCE) {
                createRiftDecoration(x, i);
            }
        }
    }

    private void createRiftDecoration(int x, int y) { createRiftDecoration(x, y, -1); }

    private void createRiftDecoration(int x, int y, int id) {
        boolean special = false;
        if (id < 0) {
            if (Server.rand.nextInt(100) < 70) {
                id = 1041 + Server.rand.nextInt(4); //plant
            }
            else if (Server.rand.nextInt(100) < 70) {
                id = 1033 + Server.rand.nextInt(4); //stone
            }
            else {
                id = 1037 + Server.rand.nextInt(4); //crystal
            }
        }
        else {
            special = true;
        }
        try {
            Item item = ItemFactory.createItem(id, Server.rand.nextInt(100) + Server.rand.nextFloat(), null);

            item.setWeight(RiftMod.RESOURCE_WEIGHT, true);

            if (special) {
                item.setPosXY(x * 4 + 2, y * 4 + 2);
            } else {
                item.setPosXY(x * 4 + (Server.rand.nextInt(4) + Server.rand.nextFloat()), y * 4 + (Server.rand.nextInt(4) + Server.rand.nextFloat()));
            }
            VolaTile tile = Zones.getOrCreateTile(x, y, true);

            decorations.add(item);

            tile.addItem(item, false, false);
        }
        catch (FailedException|NoSuchTemplateException e) {
            e.printStackTrace();
        }
    }

    private void removeEffectsFromItem(Item item) {
        Player[] players = Players.getInstance().getPlayers();
        if (players.length != 0) {
            for (int j = 0; j < players.length; j++) {
                if (players[j] != null)
                    players[j].getCommunicator().sendRemoveEffect(item.getWurmId());
            }
        }
    }

    private void removeBlackBeams() {
        if (beam != null) {
            removeEffectsFromItem(beam);
            com.wurmonline.server.Items.destroyItem(beam.getWurmId());
        }
    }

    public Boolean isDestroyed() {
        return event.state.value >= State.Finished.value;
    }

    public void destroyRift(boolean finished) {
        removeBlackBeams();
        Player[] players = Players.getInstance().getPlayers();
        Arrays.stream(players).forEach(player -> {
            if (player != null && event.identity != null) {
                player.getCommunicator().sendRemoveEffect(event.identity, (byte)25);
            }
        });
        decorations.forEach(item -> item.setWeight(0, true));
        creatures.forEach(Creature::destroy);
        event.state = finished ? State.Finished : State.Expired;
        event.synchronize();
        RiftMod.logger.log(Level.INFO, "RIFT[" + event.identity + "]: Rift has been destroyed");
        RiftMod.scheduleNextRiftTime();
    }

    private void restoreItems() {
        for (int r = RiftMod.RADIUS, x2 = event.tileX - r; x2 < event.tileX + r; ++x2) {
            for (int y2 = event.tileY - r; y2 < event.tileY + r; ++y2) {
                final VolaTile t = Zones.getTileOrNull(x2, y2, true);
                if (t != null) {
                    final Creature[] crets = t.getCreatures();
                    for (int j = 0; j < crets.length; ++j) {
                        final Creature target = crets[j];
                        if (target.getTemplate().getTemplateId() >= 106 && target.getTemplate().getTemplateId() <= 112 && !target.isDominated()) {
                            creatures.add(target);
                        }
                    }
                    final Item[] items = t.getItems();
                    for (int k = 0; k < items.length; ++k) {
                        final Item target2 = items[k];
                        if (target2.getTemplateId() >= 1033 && target2.getTemplateId() <= 1045) {
                            decorations.add(target2);
                        }
                    }
                }
            }
        }
    }

    static void clearRift(Player player) {
        int x = player.getCurrentTile().tilex;
        int y = player.getCurrentTile().tiley;
        for (int r = 50, x2 = x - r; x2 < x + r; ++x2) {
            for (int y2 = y - r; y2 < y + r; ++y2) {
                final VolaTile t = Zones.getTileOrNull(x2, y2, player.isOnSurface());
                if (t != null) {
                    final Creature[] crets = t.getCreatures();
                    for (int j = 0; j < crets.length; ++j) {
                        final Creature target = crets[j];
                        if (target.getTemplate().getTemplateId() >= 106 && target.getTemplate().getTemplateId() <= 112 && !target.isDominated()) {
                            target.destroy();
                        }
                    }
                    final Item[] items = t.getItems();
                    for (int k = 0; k < items.length; ++k) {
                        final Item target2 = items[k];
                        if (target2.getTemplateId() >= 1033 && target2.getTemplateId() <= 1045) {
                            target2.setWeight(0, true);
                        }
                    }
                }
            }
        }
    }

    private int getCreatureNumber() {
        for (int i = 0; i < creatures.size(); i++) {
            try {
                Creature creature = (Creature)creatures.get(i);
                Server.getInstance().getCreature(creature.getWurmId());
                if (Creature.getTileRange(creature, event.tileX, event.tileY) > RiftMod.RADIUS) {
                    RiftMod.logger.log(Level.INFO, "RIFT[" + event.identity + "]: getCreatureNumber: start teleporting: creature: [" + creature.getNameWithoutPrefixes() + "]");
                    creature.getBody().healFully();
                    CreatureBehaviour.blinkTo(creature, event.tileX * 4 + 2, event.tileY * 4 + 2, creature.getLayer(), creature.getPositionZ(), creature.getBridgeId(), creature.getFloorLevel());

                    creature.clearOrders();
                    creature.setLeader(null);
                    if (creature.getStatus().getPath() != null) {
                        creature.getStatus().setPath(null);
                        creature.getStatus().setMoving(false);
                    }
                    creature.setPathing(false, true);
                    RiftMod.logger.log(Level.INFO, "RIFT[" + event.identity + "]: getCreatureNumber: stop teleporting");
                }
            }
            catch (NoSuchCreatureException|NoSuchPlayerException e) {
                creatures.remove(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return creatures.size();
    }

    public int getDecorationNumber() {
        return decorations.size();
    }

    public enum State {
        Idle(-1),
        Beam(0),
        FirstWave(1),
        SecondWave(2),
        Boss(3),
        Reward(4),
        Gathering(5),
        Finished(6),
        Expired(7);
        public int value;
        private State(final int value) {
            this.value = value;
        }
        public static State fromInt(int intValue) {
            return Arrays.stream(values()).filter(state -> state.value == intValue).findFirst().orElse(null);
        }
    }
}