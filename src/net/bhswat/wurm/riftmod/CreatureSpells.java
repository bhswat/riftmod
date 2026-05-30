package net.bhswat.wurm.riftmod;

import com.wurmonline.shared.exceptions.WurmServerException;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.CreatureTemplateFactory;
import com.wurmonline.server.creatures.NoSuchCreatureException;

import com.wurmonline.server.zones.AreaSpellEffect;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;

import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Server;
import com.wurmonline.server.WurmId;

import java.util.TimerTask;
import java.util.Timer;
import java.util.ArrayList;

public class CreatureSpells
{
    public static ArrayList<Creature> getCreaturesInTile(final Creature performer, final int x, final int y, final boolean nonPlayers) {
        final ArrayList<Creature> creaturesList = new ArrayList<Creature>();
        final VolaTile tile = Zones.getTileOrNull(x, y, performer.isOnSurface());
        if (tile != null) {
            final Creature[] creatures = tile.getCreatures();
            if (creatures.length > 0) {
                for (int l = 0; l < creatures.length; ++l) {
                    if (creatures[l] != performer && !creatures[l].isUnique() && ((nonPlayers && !creatures[l].isPlayer() && !creatures[l].isDominated()) || (!nonPlayers && creatures[l].isPlayer() && creatures[l].getPower() <= 0) || (!nonPlayers && creatures[l].isDominated()))) {
                        creaturesList.add(creatures[l]);
                    }
                }
            }
        }
        return creaturesList;
    }

    public static ArrayList<Creature> getCreaturesInRadius(final Creature performer, final int radius, final boolean nonPlayers) {
        return getCreaturesInRadius(performer, performer.getCurrentTile().tilex, performer.getCurrentTile().tiley, radius, nonPlayers);
    }

    private static ArrayList<Creature> getCreaturesInRadius(final Creature performer, final int x, final int y, final int radius, final boolean nonPlayers) {
        final ArrayList<Creature> creaturesList = new ArrayList<Creature>();
        for (int a = Math.max(0, x - radius); a <= Math.min(Zones.worldTileSizeX - 1, x + radius); ++a) {
            for (int b = Math.max(0, y - radius); b <= Math.min(Zones.worldTileSizeY - 1, y + radius); ++b) {
                creaturesList.addAll(getCreaturesInTile(performer, a, b, nonPlayers));
            }
        }
        return creaturesList;
    }

    public static void explosion(final Creature performer, final int x, final int y, final int delay, final int damage) {
        new AreaSpellEffect(performer.getWurmId(), x, y, 0, (byte)51, System.currentTimeMillis() + delay * 1000L, 0.0f, 0, 0, true);
        final Timer timer = new Timer();
        final TimerTask tTask = new TimerTask() {
            @Override
            public void run() {
                new AreaSpellEffect(performer.getWurmId(), x, y, 0, (byte)35, System.currentTimeMillis() + 1000L, 0.0f, 0, 0, true);
                final ArrayList<Creature> creaturesList = CreatureSpells.getCreaturesInTile(performer, x, y, false);
                for (int i = 0; i < creaturesList.size(); ++i) {
                    final Creature target = creaturesList.get(i);
                    if (damage > 0) {
                        try {
                            target.addWoundOfType(performer, (byte)4, 1, true, 1.0f, true, (double)damage,0.0F, 0.0F, false, false);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        timer.schedule(tTask, delay * 1000);
    }

    public static void iceSpear(final Creature performer, final Creature target, final int damage) {
        final VolaTile exe = performer.getCurrentTile();
        final long shardId = WurmId.getNextTempItemId();
        if (exe != null) {
            exe.sendProjectile(shardId, (byte)4, "model.spell.ShardOfIce", "Shard Of Ice", (byte)0, performer.getPosX(), performer.getPosY(), performer.getPositionZ() + performer.getAltOffZ(), performer.getStatus().getRotation(), (byte)performer.getLayer(), (float)(int)target.getPosX(), (float)(int)target.getPosY(), target.getPositionZ() + target.getAltOffZ(), performer.getWurmId(), target.getWurmId(), 0.0f, 0.0f);
        }
        try {
            target.addWoundOfType(performer, (byte)8, 1, true, 1.0f, false, (double)damage,0.0F, 0.0F, false, false);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void iceSpears(final Creature performer, final int radius, final int damage) {
        Server.getInstance().broadCastAction(performer.getNameWithGenus() + " casts Ice Spears!", performer, 5, true);
        final ArrayList<Creature> creaturesList = getCreaturesInRadius(performer, radius, false);
        for (int i = 0; i < creaturesList.size(); ++i) {
            if (Server.rand.nextInt(2) == 0) {
                final Creature target = creaturesList.get(i);
                iceSpear(performer, target, damage);
            }
        }
    }

    public static void freeze(final Creature performer, final int radius, final int duration, final int damage) {
        final int x = performer.getCurrentTile().tilex;
        final int y = performer.getCurrentTile().tiley;
        Server.getInstance().broadCastAction(performer.getNameWithGenus() + " casts Freeze!", performer, 5, true);
        new AreaSpellEffect(performer.getWurmId(), x, y, performer.getLayer(), (byte)36, System.currentTimeMillis() + duration * 1000L, 0.0f, performer.getLayer(), 0, true);
        final ArrayList<Creature> creaturesList = getCreaturesInRadius(performer, radius, false);
        for (int i = 0; i < creaturesList.size(); ++i) {
            final Creature target = creaturesList.get(i);
            //Effects.freeze(target, duration);
            target.getCommunicator().sendCombatServerMessage("You are under the Freeze effect!", (byte)(-6), (byte)0, (byte)0);
            target.getMovementScheme().setFreezeMod(true);
            new AreaSpellEffect(target.getWurmId(), target.getCurrentTile().tilex, target.getCurrentTile().tiley, target.getLayer(), (byte)53, System.currentTimeMillis() + duration * 1000L, 0.0f, target.getLayer(), 0, true);
            if (damage > 0) {
                try {
                    target.addWoundOfType(performer, (byte)8, 1, true, 1.0f, false, (double)damage,0.0F, 0.0F, false, false);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        final Timer timer = new Timer();
        final TimerTask tTask = new TimerTask() {
            @Override
            public void run() {
                for (int i = 0; i < creaturesList.size(); ++i) {
                    final Creature target = creaturesList.get(i);
                    target.getMovementScheme().setFreezeMod(false);
                    target.getCommunicator().sendCombatServerMessage("You are no longer under the Freeze effect!", (byte)(-6), (byte)0, (byte)0);
                }
                Server.getInstance().broadCastAction("Freeze effect is gone!", performer, 5, true);
            }
        };
        timer.schedule(tTask, duration * 1000L);
    }

    public static void chaoticExplosions(final Creature performer, final int radius, final int delay, final int damage) {
        final int x = performer.getCurrentTile().tilex;
        final int y = performer.getCurrentTile().tiley;
        Server.getInstance().broadCastAction(performer.getNameWithGenus() + " casts Chaotic Explosions!", performer, 5, true);
        for (int a = Math.max(0, x - radius); a <= Math.min(Zones.worldTileSizeX - 1, x + radius); ++a) {
            for (int b = Math.max(0, y - radius); b <= Math.min(Zones.worldTileSizeY - 1, y + radius); ++b) {
                new AreaSpellEffect(performer.getWurmId(), a, b, 0, (byte)51, System.currentTimeMillis() + delay * 1000L, 0.0f, 0, 0, true);
            }
        }
        final Timer timer = new Timer();
        final TimerTask tTask = new TimerTask() {
            @Override
            public void run() {
                for (int a = Math.max(0, x - radius); a <= Math.min(Zones.worldTileSizeX - 1, x + radius); ++a) {
                    for (int b = Math.max(0, y - radius); b <= Math.min(Zones.worldTileSizeY - 1, y + radius); ++b) {
                        if (Server.rand.nextInt(3) == 0) {
                            new AreaSpellEffect(performer.getWurmId(), a, b, 0, (byte)35, System.currentTimeMillis() + 1000L, 0.0f, 0, 0, true);
                        }
                        final ArrayList<Creature> creaturesList = CreatureSpells.getCreaturesInTile(performer, a, b, false);
                        for (int i = 0; i < creaturesList.size(); ++i) {
                            final Creature target = creaturesList.get(i);
                            if (damage > 0) {
                                try {
                                    target.addWoundOfType(performer, (byte)4, 1, true, 0.2f, true, (double)damage,0.0F, 0.0F, false, false);
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        };
        timer.schedule(tTask, delay * 1000L);
    }

    public static final void summonBeasts(final Creature creature, final int number) {
        final int radius = 1;
        final int sx = creature.getCurrentTile().tilex;
        final int sy = creature.getCurrentTile().tiley;
        Server.getInstance().broadCastAction(creature.getNameWithGenus() + " casts Summon Beasts!", creature, 5, true);
        for (int i = 0; i < number; ++i) {
            final int x = sx + Server.rand.nextInt(radius * 2) - radius;
            final int y = sy + Server.rand.nextInt(radius * 2) - radius;
            final int id = 106;
            try {
                final CreatureTemplate cr = CreatureTemplateFactory.getInstance().getTemplate(id);
                Creature.doNew(id, x * 4, y * 4, creature.getStatus().getRotation(), 0, cr.getName(), (byte)Server.rand.nextInt(2));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void heal(final Creature performer, final int radius) {
        final ArrayList<Creature> creaturesList = getCreaturesInRadius(performer, radius, true);
        Server.getInstance().broadCastAction(performer.getNameWithGenus() + " casts Heal!", performer, 5, true);
        for (int i = 0; i < creaturesList.size(); ++i) {
            final Creature target = creaturesList.get(i);
            final VolaTile t = Zones.getTileOrNull(target.getTileX(), target.getTileY(), target.isOnSurface());
            if (t != null) {
                t.sendAttachCreatureEffect(target, (byte)11, (byte)0, (byte)0, (byte)0, (byte)0);
            }
            target.getBody().healFully();
        }
    }

    public static void vaccuum(final Creature performer, final int radius) {
        final ArrayList<Creature> creaturesList = getCreaturesInRadius(performer, radius, false);
        for (int i = 0; i < creaturesList.size(); ++i) {
            final Creature target = creaturesList.get(i);
            target.getCommunicator().sendCombatServerMessage("You are under the Vacuum effect!", (byte)(-6), (byte)0, (byte)0);
            Effects.blink(target, performer.getCurrentTile().tilex, performer.getCurrentTile().tiley);
        }
    }

    public static void unstableAura(final Creature performer, final int radius, final int damage) {
        final ArrayList<Creature> creaturesList = getCreaturesInRadius(performer, radius, false);
        for (int i = 0; i < creaturesList.size(); ++i) {
            final Creature target = creaturesList.get(i);
            target.getCommunicator().sendCombatServerMessage("You are under the Unstable Aura!", (byte)(-6), (byte)0, (byte)0);
            for (int j = 0; j < 4; ++j) {
                byte woundType = 4;
                switch (Server.rand.nextInt(6)) {
                    case 0: {
                        woundType = 4;
                        break;
                    }
                    case 1: {
                        woundType = 5;
                        break;
                    }
                    case 2: {
                        woundType = 6;
                        break;
                    }
                    case 3: {
                        woundType = 8;
                        break;
                    }
                    case 4: {
                        woundType = 9;
                        break;
                    }
                    case 5: {
                        woundType = 10;
                        break;
                    }
                }
                final VolaTile t = Zones.getTileOrNull(target.getTileX(), target.getTileY(), target.isOnSurface());
                if (t != null) {
                    t.sendAttachCreatureEffect(target, (byte)8, (byte)0, (byte)0, (byte)0, (byte)0);
                }
                try {
                    target.addWoundOfType(performer, woundType, 1, true, 1.0f, false, (double)damage,0.0F, 0.0F, false, false);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void fatigue(final Creature performer, final int radius) {
        final ArrayList<Creature> creaturesList = getCreaturesInRadius(performer, radius, false);
        for (int i = 0; i < creaturesList.size(); ++i) {
            final Creature target = creaturesList.get(i);
            performer.getCommunicator().sendCombatServerMessage("You are under the Fatigue!", (byte)(-6), (byte)0, (byte)0);
            target.getStatus().modifyStamina(-(20000.0f + Server.rand.nextInt(20000)));
        }
    }

    public static final void summonWolfs(final Creature performer) {
        int x = performer.getCurrentTile().tilex * 4;
        int y = performer.getCurrentTile().tiley * 4;
        x += Server.rand.nextInt(16) - 8;
        y += Server.rand.nextInt(16) - 8;
        try {
            final Creature[] crArr = new Creature[3];
            for (int i = 0; i < 3; ++i) {
                x += Server.rand.nextInt(16) - 8;
                y += Server.rand.nextInt(16) - 8;
                crArr[i] = Creature.doNew(524, true, (float)x, (float)y, performer.getStatus().getRotation(), performer.getLayer(), "Spirit Wolf", (byte)0, (byte)0, (byte)21, true);
            }
            final Timer timer = new Timer();
            final TimerTask tTask = new TimerTask() {
                @Override
                public void run() {
                    for (int i = 0; i < 3; ++i) {
                        if (crArr[i] != null && crArr[i].isAlive()) {
                            crArr[i].destroy();
                        }
                    }
                }
            };
            timer.schedule(tTask, 20000L);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final void darkRegeneration(final Creature performer, final int power) {
        final VolaTile t = Zones.getTileOrNull(performer.getTileX(), performer.getTileY(), performer.isOnSurface());
        Effects.regeneration(performer, power, 10, "Void Regeneration");
        if (t != null) {
            t.sendAttachCreatureEffect(performer, (byte)11, (byte)0, (byte)0, (byte)0, (byte)0);
        }
    }

    public static final void firePillars(final Creature performer, final int power) {
        for (int i = 0; i < Server.rand.nextInt(15) + 10; ++i) {
            int x = performer.getCurrentTile().tilex;
            int y = performer.getCurrentTile().tiley;
            x += Server.rand.nextInt(10) - 5;
            y += Server.rand.nextInt(10) - 5;
            startFirePillar(performer, x, y, power);
        }
    }

    public static final void iceSpear1(final Creature performer, final int power) {
        final long[] ids = performer.getLatestAttackers();
        if (ids.length > 0) {
            for (int i = 0; i < ids.length; ++i) {
                final long x1 = ids[i];
                try {
                    final Creature target = Server.getInstance().getCreature(x1);
                    if (!target.isDead()) {
                        final double range = Creature.getRange(performer, (double)target.getPosX(), (double)target.getPosY());
                        if (target.isOnSurface() == performer.isOnSurface() && range < 25.0) {
                            startIceSpear(performer, target, power);
                        }
                    }
                }
                catch (NoSuchPlayerException | NoSuchCreatureException ex2) {
                    final WurmServerException ex;
                    final WurmServerException e = ex2;
                    e.printStackTrace();
                }
            }
        }
    }

    private static void startFirePillar(final Creature performer, final int x, final int y, final int damage) {
        new AreaSpellEffect(performer.getWurmId(), x, y, performer.getLayer(), (byte)35, System.currentTimeMillis() + 1000L, 0.0f, performer.getLayer(), 0, true);
        final VolaTile t = Zones.getTileOrNull(x, y, performer.isOnSurface());
        if (t != null) {
            final Creature[] cretures = t.getCreatures();
            for (int i = 0; i < cretures.length; ++i) {
                final Creature target = cretures[i];
                if (target != performer) {
                    try {
                        final int pos = target.getBody().getRandomWoundPos();
                        target.addWoundOfType(performer, (byte)4, pos, false, 1.0f, false, (double)damage,0.0F, 0.0F, false, false);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void startIceSpear(final Creature performer, final Creature target, final int damage) {
        VolaTile exe = performer.getCurrentTile();
        final long shardId = WurmId.getNextTempItemId();
        if (exe != null) {
            exe.sendProjectile(shardId, (byte)4, "model.spell.ShardOfIce", "Shard Of Ice", (byte)0, performer.getPosX(), performer.getPosY(), performer.getPositionZ() + performer.getAltOffZ(), performer.getStatus().getRotation(), (byte)performer.getLayer(), (float)(int)target.getPosX(), (float)(int)target.getPosY(), target.getPositionZ() + target.getAltOffZ(), performer.getWurmId(), target.getWurmId(), 0.0f, 0.0f);
        }
        exe = target.getCurrentTile();
        if (exe != null) {
            exe.sendProjectile(shardId, (byte)4, "model.spell.ShardOfIce", "Shard Of Ice", (byte)0, performer.getPosX(), performer.getPosY(), performer.getPositionZ() + performer.getAltOffZ(), performer.getStatus().getRotation(), (byte)performer.getLayer(), (float)(int)target.getPosX(), (float)(int)target.getPosY(), target.getPositionZ() + target.getAltOffZ(), performer.getWurmId(), target.getWurmId(), 0.0f, 0.0f);
        }
        try {
            target.addWoundOfType(performer, (byte)8, (int)target.getBody().getCenterWoundPos(), false, 1.0f, false, (double)damage,0.0F, 0.0F, false, false);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
