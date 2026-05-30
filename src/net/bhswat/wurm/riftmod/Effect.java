package net.bhswat.wurm.riftmod;

public class Effect
{
    public int id;
    public String name;
    public int duration;
    public int power;
    public long wurmid;
    public boolean isNegative;

    Effect(final int aId, final String aName, final int aDuration, final int aPower, final long aWurmId, final boolean aIsNegative) {
        this.id = aId;
        this.name = aName;
        this.duration = aDuration;
        this.power = aPower;
        this.wurmid = aWurmId;
        this.isNegative = aIsNegative;
        RiftMod.effects.add(this);
    }
}