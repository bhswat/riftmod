package net.bhswat.wurm.riftmod;

import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.CreatureTemplateFactory;
import java.util.Timer;


public class RiftSpawner extends Thread
{
    private Rift rift;
    private int[] creatureNumber;
    private int[] creatureMaxNumber;
    private int tick;
    public boolean isFinish = false;

    public static void showBeam(Rift rift, com.wurmonline.server.players.Player player) {
        if ((player != null) && (rift != null) && (rift.beam != null)) {
            player.getCommunicator().sendAddEffect(rift.beam.getWurmId(), (short)25, rift.event.tileX * 4 + 2, rift.event.tileY * 4 + 2, 0.0F, (byte)0);
        }
    }

    RiftSpawner(Rift aRift, int[] aCretMaxNum, int aTick)
    {
        rift = aRift;
        creatureMaxNumber = aCretMaxNum;
        tick = aTick;
        creatureNumber = new int[7];
        if (creatureMaxNumber.length == 7) {
            start();
        }

    }

    public void reset() {
        rift = null;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Rift Spawner");

        try {
            boolean spawnerOn = true;
            boolean end = true;
            int maxNumber = 0;
            for (int i = 0; i < creatureMaxNumber.length; i++) {
                if (creatureMaxNumber[i] > maxNumber)
                    maxNumber = creatureMaxNumber[i];
            }
            while (spawnerOn) {
                if (rift == null) break;
                end = true;
                for (int i = 0; i < 7; i++) {
                    if (creatureNumber[i] < creatureMaxNumber[i]) {
                        end = false;
                        int id = 106 + i;
                        try {
                            CreatureTemplate cr = CreatureTemplateFactory.getInstance().getTemplate(id);

                            final Creature creature = Creature.doNew(id, rift.event.tileX * 4 + (Server.rand.nextInt(4) + Server.rand.nextFloat()), rift.event.tileY * 4 + (Server.rand.nextInt(4) + Server.rand.nextFloat()), Server.rand.nextFloat() * 360.0F, 0, cr.getName(), (byte)Server.rand.nextInt(2));

                            if (creature != null) {
                                rift.creatures.add(creature);

                                if (creature.getTemplate().getTemplateId() != 109) {
                                    sendRandomMove(creature, 9);

                                    Timer moveTimer = new Timer();
                                    java.util.TimerTask moveTask = new java.util.TimerTask() {
                                        @Override
                                        public void run() {
                                            int rd = 14;
                                            final int x = creature.getCurrentTile().tilex;
                                            final int y = creature.getCurrentTile().tiley;

                                            int tmpx = Server.rand.nextInt(rd);

                                            if (x < rift.event.tileX) {
                                                tmpx *= -1;
                                            }
                                            int tmpy = (int)Math.sqrt(Math.pow(rd, 2.0D) - Math.pow(tmpx, 2.0D));
                                            tmpx += rift.event.tileX;
                                            if (y < rift.event.tileY) {
                                                tmpy *= -1;
                                            }
                                            tmpy += rift.event.tileY;

                                            int tile = Server.surfaceMesh.getTile(x, y);

                                            creature.startPathingToTile(new com.wurmonline.server.creatures.ai.PathTile(tmpx, tmpy, tile, creature.isOnSurface(), creature.getFloorLevel()));
                                        }

                                    };
                                    moveTimer.schedule(moveTask, 9000L);
                                }
                            }
                        }
                        catch (Exception localException) {}

                        creatureNumber[i] += 1;
                    }
                    Thread.sleep(200L);
                }
                if (end) {
                    spawnerOn = false;
                }



                Thread.sleep(tick * 1000);
            }
        } catch (InterruptedException e) { e.printStackTrace();
        }
        isFinish = true;
    }

    private static void sendRandomMove(Creature creature, int radius) { int tmpx = Server.rand.nextInt(radius * 2) - radius;
        int tmpy = (int)Math.sqrt(Math.pow(radius, 2.0D) - Math.pow(tmpx, 2.0D));
        tmpx += creature.getCurrentTile().tilex;
        tmpy *= (Server.rand.nextBoolean() ? -1 : 1);
        tmpy += creature.getCurrentTile().tiley;


        int tile = Server.surfaceMesh.getTile(creature.getCurrentTile().tilex, creature.getCurrentTile().tiley);
        creature.startPathingToTile(new com.wurmonline.server.creatures.ai.PathTile(tmpx, tmpy, tile, creature.isOnSurface(), creature.getFloorLevel()));
    }
}
