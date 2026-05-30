package net.bhswat.wurm.riftmod;

import com.wurmonline.server.behaviours.CreatureBehaviour;
import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.Server;
import java.util.TimerTask;
import java.util.Timer;
import com.wurmonline.server.modifiers.FixedDoubleValueModifier;
import com.wurmonline.server.modifiers.DoubleValueModifier;
import com.wurmonline.server.creatures.Creature;
import java.util.List;

public class Effects
{
    public static final String STR_ADD_EFFECT = "You are under the effect";
    public static final String STR_TP_EFFECT = "was teleported.";
    public static final String STR_DEL_EFFECT = "You are no longer under the effect";

    private static void sendEffect(final int id, final String name, final int duration, final long wurmid, final boolean isNegative, final int type) {
        sendEffect(id, name, duration, -1, wurmid, isNegative, type);
    }

    private static void sendEffect(final int id, final String name, final int duration, final int power, final long wurmid, final boolean isNegative, final int type) {
        final Effect currEffect = getEffect(id, wurmid);
        switch (type) {
            case 0: {
                if (currEffect == null) {
                    new Effect(id, name, duration, power, wurmid, isNegative);
                    break;
                }
                break;
            }
            case 1: {
                if (currEffect == null) {
                    sendEffect(id, name, duration, wurmid, isNegative, 0);
                    break;
                }
                final Effect effect = currEffect;
                effect.power += power;
                break;
            }
            case 2: {
                if (currEffect == null) {
                    sendEffect(id, name, duration, wurmid, isNegative, 0);
                    break;
                }
                final Effect effect2 = currEffect;
                effect2.duration += duration;
                break;
            }
            case 3: {
                if (currEffect == null) {
                    sendEffect(id, name, duration, wurmid, isNegative, 0);
                    break;
                }
                if (currEffect.power == power) {
                    final Effect effect3 = currEffect;
                    effect3.duration += duration;
                    break;
                }
                break;
            }
            case 4: {
                if (currEffect == null) {
                    sendEffect(id, name, duration, wurmid, isNegative, 0);
                    break;
                }
                final Effect effect4 = currEffect;
                effect4.power += power;
                final Effect effect5 = currEffect;
                effect5.duration += duration;
                break;
            }
            case 5: {
                if (currEffect == null) {
                    sendEffect(id, name, duration, wurmid, isNegative, 0);
                    break;
                }
                if (currEffect.power <= power) {
                    currEffect.power = power;
                    currEffect.duration = duration;
                    break;
                }
                break;
            }
        }
    }

    public static boolean hasEffect(final int id, final long wurmid) {
        return getEffect(id, wurmid) != null;
    }

    public static Effect getEffect(final int id, final long wurmid) {
        final List<Effect> effects = RiftMod.effects;
        for (int i = 0; i < effects.size(); ++i) {
            final Effect effect = effects.get(i);
            if (effect.id == id && effect.wurmid == wurmid) {
                return effect;
            }
        }
        return null;
    }

    public static List<Effect> getEffects() {
        return RiftMod.effects;
    }

    private static boolean checkCret(final Creature creature) {
        return creature != null && creature.isAlive();
    }

    public static void freeze(final Creature creature, final int duration) {
        final int id = 2;
        final long wurmid = creature.getWurmId();
        final String effectName = "Freeze";
        //sendEffect(id, effectName, duration, wurmid, true, 2);
        //if (creature.getMovementScheme().getSpeedModifier() > 0.0f) {
            creature.getCommunicator().sendCombatServerMessage("You are under the Freeze effect!", (byte)(-6), (byte)0, (byte)0);
            creature.getMovementScheme().addModifier((DoubleValueModifier)new FixedDoubleValueModifier(-10.0));
            final Timer timer = new Timer();
            final TimerTask tTask = new TimerTask() {
                @Override
                public void run() {
                    //if (!Effects.hasEffect(id, wurmid)) {
                        //if (!checkCret(creature)) {
                        //    this.cancel();
                        //}
                        //else {
                            creature.getMovementScheme().removeModifier((DoubleValueModifier)new FixedDoubleValueModifier(-10.0));
                            //timer.cancel();
                        //}
                    //}
                }
            };
            timer.schedule(tTask, duration * 1000L);
        //}
    }

    public static void blink(final Creature performer, final int x, final int y) {
        CreatureBehaviour.blinkTo(performer, x * 4 + 2, y * 4 + 2, performer.getLayer(), performer.getPositionZ(), performer.getBridgeId(), performer.getFloorLevel());
    }

    public static void regeneration(final Creature performer, final int power, final int duration, String effectName) {
        final int id = 3;
        final long wurmid = performer.getWurmId();
        if (effectName == null) {
            effectName = "Regeneration";
        }
        sendEffect(id, effectName, duration, power, wurmid, false, 5);
        performer.getCommunicator().sendCombatServerMessage("You are under the Regeneration effect!", (byte)0, (byte)(-6), (byte)0);
        final Timer timer = new Timer();
        final TimerTask tTask = new TimerTask() {
            @Override
            public void run() {
                if (!Effects.hasEffect(id, wurmid)) {
                    timer.cancel();
                }
                else if (!checkCret(performer)) {
                    this.cancel();
                }
                else {
                    Effects.restoreHealth(performer, power, false);
                }
            }
        };
        timer.schedule(tTask, 1000L, 1000L);
    }

    public static void restoreHealth(final Creature performer, int power, final boolean onewound) {
        while (power > 0) {
            if (performer != null && performer.getBody() != null && performer.getBody().getWounds() != null) {
                final Wound[] wounds = performer.getBody().getWounds().getWounds();
                if (wounds != null && wounds.length > 0) {
                    final int num = Server.rand.nextInt(wounds.length);
                    if (wounds[num].getSeverity() < power) {
                        power -= (int)wounds[num].getSeverity();
                        wounds[num].heal();
                    }
                    else {
                        wounds[num].modifySeverity(-power);
                        power = 0;
                    }
                }
                else {
                    power = 0;
                }
            }
            else {
                power = 0;
            }
            if (onewound) {
                power = 0;
            }
        }
    }
}