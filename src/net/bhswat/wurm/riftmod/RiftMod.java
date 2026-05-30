package net.bhswat.wurm.riftmod;

import com.wurmonline.server.*;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.combat.CombatMove;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.CreatureTemplateFactory;
import com.wurmonline.server.players.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.skills.Skills;
import com.wurmonline.server.skills.SkillsFactory;
import com.wurmonline.server.webinterface.WcKingdomChat;
import javassist.*;
import javassist.bytecode.Descriptor;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;


public class RiftMod implements WurmServerMod, Initable, PreInitable, Configurable, PlayerMessageListener, PlayerLoginListener, ServerStartedListener, ServerPollListener {
    static int TICK = 15;
    static int scheduleRiftInDayOfWeek = 7;
    static int scheduleRiftAtHour = 18;
    static int DECORATION_CHANCE = 10;
    static long FIRST_DELAY = 60;
    static long WAVE_DELAY = 1;
    static long END_DELAY = 180;
    static long FINISH_DELAY = 30;
    static int SPAWNER_TICK = 1;
    static int RADIUS = 25;
    static long BELL_TIME = 10;
    static int SHOULDER_CHANCE = 10;
    static int ACCESSORY_CHANCE = 10;
    static int RESOURCE_WEIGHT = 1000;
    static int LUMP_WEIGHT = 15000;
    static int MASK_CHANCE = 10;
    static int normalCoins = 200;
    static int addCoins = 300;
    static int normalFs = 50;
    static int addFs = 70;


    static int[][] stageList = new int[3][7];
    //public static ArrayList<Rift> rifts = new ArrayList<Rift>();
    //static List<Effect> effects = new ArrayList<Effect>();
    public static ArrayList<Effect> effects = new ArrayList<Effect>();

    private long lastProcessedTick = 0;
    public static long scheduledRiftTime = 0;
    private ArrayList<Rift> activeRifts = new ArrayList<>();
    private boolean resheduleRiftTimerOnStart = false;
    private RiftSpotFinder spotFinder = new RiftSpotFinder();

    public static Logger logger = Logger.getLogger(RiftMod.class.getName());

    public void configure(Properties properties) {
        spotFinder.configure(properties);
        TICK = Integer.parseInt(properties.getProperty("TICK", Integer.toString(TICK)));
        resheduleRiftTimerOnStart = Boolean.parseBoolean(properties.getProperty("resheduleRiftTimerOnStart", Boolean.toString(resheduleRiftTimerOnStart)));
        scheduleRiftInDayOfWeek = Integer.parseInt(properties.getProperty("scheduleRiftInDayOfWeek", Integer.toString(scheduleRiftInDayOfWeek)));
        scheduleRiftAtHour = Integer.parseInt(properties.getProperty("scheduleRiftAtHour", Integer.toString(scheduleRiftAtHour)));
        DECORATION_CHANCE = Integer.parseInt(properties.getProperty("decorationChance", Integer.toString(DECORATION_CHANCE)));
        FIRST_DELAY = TimeConstants.MINUTE * Integer.parseInt(properties.getProperty("riftFirstDelay", Long.toString(FIRST_DELAY)));
        WAVE_DELAY = TimeConstants.MINUTE * Integer.parseInt(properties.getProperty("riftWaveDelay", Long.toString(WAVE_DELAY)));
        END_DELAY = TimeConstants.MINUTE * Integer.parseInt(properties.getProperty("riftEndDelay", Long.toString(END_DELAY)));
        FINISH_DELAY = TimeConstants.MINUTE * Integer.parseInt(properties.getProperty("riftFinishDelay", Long.toString(FINISH_DELAY)));
        SPAWNER_TICK = Integer.parseInt(properties.getProperty("spawnerTick", Integer.toString(SPAWNER_TICK)));
        RADIUS = Integer.parseInt(properties.getProperty("radius", Integer.toString(RADIUS)));
        BELL_TIME = TimeConstants.MINUTE * Integer.parseInt(properties.getProperty("bellTime", Long.toString(BELL_TIME)));
        SHOULDER_CHANCE = Integer.parseInt(properties.getProperty("shoulder.chance", Integer.toString(SHOULDER_CHANCE)));
        ACCESSORY_CHANCE = Integer.parseInt(properties.getProperty("accessory.chance", Integer.toString(ACCESSORY_CHANCE)));
        RESOURCE_WEIGHT = Integer.parseInt(properties.getProperty("resource.weight", Integer.toString(RESOURCE_WEIGHT)));
        LUMP_WEIGHT = Integer.parseInt(properties.getProperty("lump.weight", Integer.toString(LUMP_WEIGHT)));
        MASK_CHANCE = Integer.parseInt(properties.getProperty("mask.chance", Integer.toString(MASK_CHANCE)));

        normalCoins = Integer.parseInt(properties.getProperty("coins.normalcoins", Integer.toString(normalCoins)));
        addCoins = Integer.parseInt(properties.getProperty("coins.additionalcoins", Integer.toString(addCoins)));
        normalFs = Integer.parseInt(properties.getProperty("coins.normalfs", Integer.toString(normalFs)));
        addFs = Integer.parseInt(properties.getProperty("coins.highfs", Integer.toString(addFs)));

        stageList[0][0] = Integer.parseInt(properties.getProperty("wave1.beast", Integer.toString(15)));
        stageList[0][1] = Integer.parseInt(properties.getProperty("wave1.jackal", Integer.toString(0)));
        stageList[0][2] = Integer.parseInt(properties.getProperty("wave1.ogre", Integer.toString(5)));
        stageList[0][3] = Integer.parseInt(properties.getProperty("wave1.warmaster", Integer.toString(0)));
        stageList[0][4] = Integer.parseInt(properties.getProperty("wave1.caster", Integer.toString(10)));
        stageList[0][5] = Integer.parseInt(properties.getProperty("wave1.ogreMage", Integer.toString(5)));
        stageList[0][6] = Integer.parseInt(properties.getProperty("wave1.summoner", Integer.toString(0)));
        stageList[1][0] = Integer.parseInt(properties.getProperty("wave2.beast", Integer.toString(5)));
        stageList[1][1] = Integer.parseInt(properties.getProperty("wave2.jackal", Integer.toString(10)));
        stageList[1][2] = Integer.parseInt(properties.getProperty("wave2.ogre", Integer.toString(0)));
        stageList[1][3] = Integer.parseInt(properties.getProperty("wave2.warmaster", Integer.toString(0)));
        stageList[1][4] = Integer.parseInt(properties.getProperty("wave2.caster", Integer.toString(15)));
        stageList[1][5] = Integer.parseInt(properties.getProperty("wave2.ogreMage", Integer.toString(5)));
        stageList[1][6] = Integer.parseInt(properties.getProperty("wave2.summoner", Integer.toString(0)));
        stageList[2][0] = Integer.parseInt(properties.getProperty("wave3.beast", Integer.toString(0)));
        stageList[2][1] = Integer.parseInt(properties.getProperty("wave3.jackal", Integer.toString(0)));
        stageList[2][2] = Integer.parseInt(properties.getProperty("wave3.ogre", Integer.toString(0)));
        stageList[2][3] = Integer.parseInt(properties.getProperty("wave3.warmaster", Integer.toString(1)));
        stageList[2][4] = Integer.parseInt(properties.getProperty("wave3.caster", Integer.toString(0)));
        stageList[2][5] = Integer.parseInt(properties.getProperty("wave3.ogreMage", Integer.toString(0)));
        stageList[2][6] = Integer.parseInt(properties.getProperty("wave3.summoner", Integer.toString(0)));

        logger.info("Riftmod initialized.");
    }

    @Override
    @Deprecated
    public boolean onPlayerMessage(Communicator communicator, String message) {
        Player performer = communicator.getPlayer();
        if (message.startsWith("#rift")) {
            if (performer.getPower() < 2) {
                communicator.sendNormalServerMessage("You have no power to use developer command.");
                logger.info("Player " + performer.getName() + " have no power to use developer command " + message);
                return false;
            } else {
                if (message.startsWith("#riftnow")) {
                    spotFinder.coordinates = null;
                    scheduledRiftTime = System.currentTimeMillis();
                } else
                if (message.startsWith("#riftstart")) {
                    RiftEventModel model = RiftEventModel.makeByScheduledTime(System.currentTimeMillis(), performer.getCurrentTile().tilex, performer.getCurrentTile().tiley);
                    addActiveRift(new Rift(model));
                    communicator.sendNormalServerMessage("Rift started at " + performer.getCurrentTile().tilex + " " + performer.getCurrentTile().tiley);
                    logger.info("Rift started at " + performer.getCurrentTile().tilex + " " + performer.getCurrentTile().tiley + " by " + performer.getName());
                } else if (message.startsWith("#riftdel")) {
                    Rift nearPerformerRift = activeRifts.stream().filter(rift -> rift.event.tileX == performer.getTileX() && rift.event.tileY == performer.getTileY()).findFirst().orElse(null);
                    if (nearPerformerRift != null) {
                        nearPerformerRift.destroyRift(true);
                    }
                } else if (message.startsWith("#riftcretdel")) {
                    Rift.clearRift(performer);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPlayerLogin(Player player) {
        activeRifts.forEach(rift -> RiftSpawner.showBeam(rift, player));
    }

    private static void changeRiftBeast() {
        try {
            CreatureTemplate template = CreatureTemplateFactory.getInstance().getTemplate(106);
            if(template != null) {
                template.setBaseCombatRating(50.0F);

                Skills skills = SkillsFactory.createSkills("Rift Beast");
                skills.learnTemp(SkillList.BODY_STRENGTH, 40.0F);
                skills.learnTemp(SkillList.BODY_CONTROL, 40.0F);
                skills.learnTemp(SkillList.BODY_STAMINA, 30.0F);
                skills.learnTemp(SkillList.MIND_LOGICAL, 10.0F);
                skills.learnTemp(SkillList.MIND_SPEED, 10.0F);
                skills.learnTemp(SkillList.SOUL_STRENGTH, 40.0F);
                skills.learnTemp(SkillList.SOUL_DEPTH, 10.0F);
                skills.learnTemp(SkillList.WEAPONLESS_FIGHTING, 60.0F);

                skills.learnTemp(SkillList.GROUP_FIGHTING, 50.0f);
                skills.learnTemp(SkillList.FIGHT_AGGRESSIVESTYLE, 30.0f);
                skills.learnTemp(SkillList.FIGHT_DEFENSIVESTYLE, 30.0f);
                skills.learnTemp(SkillList.FIGHT_NORMALSTYLE, 30.0f);
                ReflectionUtil.setPrivateField(template, ReflectionUtil.getField(template.getClass(), "skills"), skills);
            } // if
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void changeRiftJackal() {
        try {
            CreatureTemplate template = CreatureTemplateFactory.getInstance().getTemplate(107);
            if(template != null) {
                template.setBaseCombatRating(50.0F);
                template.setCombatMoves(new int[] { 7 });

                Skills skills = SkillsFactory.createSkills("Rift Jackal");
                skills.learnTemp(SkillList.BODY_STRENGTH, 40.0F);//BODY_STRENGTH
                skills.learnTemp(SkillList.BODY_CONTROL, 40.0F);//BODY_CONTROL
                skills.learnTemp(SkillList.BODY_STAMINA, 40.0F);//BODY_STAMINA
                skills.learnTemp(SkillList.MIND_LOGICAL, 20.0F);//MIND_LOGICAL
                skills.learnTemp(SkillList.MIND_SPEED, 20.0F);//MIND_SPEED
                skills.learnTemp(SkillList.SOUL_STRENGTH, 30.0F);//SOUL_STRENGTH
                skills.learnTemp(SkillList.SOUL_DEPTH, 20.0F);//SOUL_DEPTH
                skills.learnTemp(SkillList.WEAPONLESS_FIGHTING, 60.0F);//WEAPONLESS_FIGHTING

                skills.learnTemp(SkillList.GROUP_FIGHTING, 90.0f);
                skills.learnTemp(SkillList.FIGHT_AGGRESSIVESTYLE, 60.0f);
                skills.learnTemp(SkillList.FIGHT_DEFENSIVESTYLE, 60.0f);
                skills.learnTemp(SkillList.FIGHT_NORMALSTYLE, 60.0f);
                ReflectionUtil.setPrivateField(template, ReflectionUtil.getField(template.getClass(), "skills"), skills);
            } // if
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void changeRiftOgre() {
        try {
            CreatureTemplate template = CreatureTemplateFactory.getInstance().getTemplate(108);
            if(template != null) {
                template.setBaseCombatRating(60.0F);
                template.setCombatMoves(new int[] { 5, 7, 8 });

                Skills skills = SkillsFactory.createSkills("Rift Ogre");
                skills.learnTemp(SkillList.BODY_STRENGTH, 65.0F);//BODY_STRENGTH
                skills.learnTemp(SkillList.BODY_CONTROL, 30.0F);//BODY_CONTROL
                skills.learnTemp(SkillList.BODY_STAMINA, 30.0F);//BODY_STAMINA
                skills.learnTemp(SkillList.MIND_LOGICAL, 20.0F);//MIND_LOGICAL
                skills.learnTemp(SkillList.MIND_SPEED, 20.0F);//MIND_SPEED
                skills.learnTemp(SkillList.SOUL_STRENGTH, 30.0F);//SOUL_STRENGTH
                skills.learnTemp(SkillList.SOUL_DEPTH, 20.0F);//SOUL_DEPTH
                skills.learnTemp(SkillList.WEAPONLESS_FIGHTING, 40.0F);//WEAPONLESS_FIGHTING

                skills.learnTemp(SkillList.GROUP_FIGHTING, 70.0f);
                skills.learnTemp(SkillList.FIGHT_AGGRESSIVESTYLE, 50.0f);
                skills.learnTemp(SkillList.FIGHT_DEFENSIVESTYLE, 50.0f);
                skills.learnTemp(SkillList.FIGHT_NORMALSTYLE, 50.0f);
                ReflectionUtil.setPrivateField(template, ReflectionUtil.getField(template.getClass(), "skills"), skills);
            } // if
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void changeRiftWarmaster() {
        try {
            CreatureTemplate template = CreatureTemplateFactory.getInstance().getTemplate(109);
            if(template != null) {
                template.setBaseCombatRating(95.0F);
                template.setCombatMoves(new int[] { 17, 18, 19 });

                Skills skills = SkillsFactory.createSkills("Rift Warmaster");
                skills.learnTemp(SkillList.BODY_STRENGTH, 90.0F);//BODY_STRENGTH
                skills.learnTemp(SkillList.BODY_CONTROL, 40.0F);//BODY_CONTROL
                skills.learnTemp(SkillList.BODY_STAMINA, 40.0F);//BODY_STAMINA
                skills.learnTemp(SkillList.MIND_LOGICAL, 20.0F);//MIND_LOGICAL
                skills.learnTemp(SkillList.MIND_SPEED, 20.0F);//MIND_SPEED
                skills.learnTemp(SkillList.SOUL_STRENGTH, 40.0F);//SOUL_STRENGTH
                skills.learnTemp(SkillList.SOUL_DEPTH, 90.0F);//SOUL_DEPTH
                skills.learnTemp(SkillList.WEAPONLESS_FIGHTING, 40.0F);//WEAPONLESS_FIGHTING

                skills.learnTemp(SkillList.GROUP_FIGHTING, 75.0f);
                skills.learnTemp(SkillList.FIGHT_AGGRESSIVESTYLE, 75.0f);
                skills.learnTemp(SkillList.FIGHT_DEFENSIVESTYLE, 75.0f);
                skills.learnTemp(SkillList.FIGHT_NORMALSTYLE, 75.0f);
                ReflectionUtil.setPrivateField(template, ReflectionUtil.getField(template.getClass(), "skills"), skills);
            } // if
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void changeRiftCaster() {
        try {
            CreatureTemplate template = CreatureTemplateFactory.getInstance().getTemplate(110);
            if(template != null) {
                template.setBaseCombatRating(40.0F);
                template.setCombatMoves(new int[] { 12, 13 });

                Skills skills = SkillsFactory.createSkills("Rift Caster");
                skills.learnTemp(SkillList.BODY_STRENGTH, 40.0F);//BODY_STRENGTH
                skills.learnTemp(SkillList.BODY_CONTROL, 30.0F);//BODY_CONTROL
                skills.learnTemp(SkillList.BODY_STAMINA, 20.0F);//BODY_STAMINA
                skills.learnTemp(SkillList.MIND_LOGICAL, 40.0F);//MIND_LOGICAL
                skills.learnTemp(SkillList.MIND_SPEED, 40.0F);//MIND_SPEED
                skills.learnTemp(SkillList.SOUL_STRENGTH, 30.0F);//SOUL_STRENGTH
                skills.learnTemp(SkillList.SOUL_DEPTH, 20.0F);//SOUL_DEPTH
                skills.learnTemp(SkillList.WEAPONLESS_FIGHTING, 40.0F);//WEAPONLESS_FIGHTING
                skills.learnTemp(SkillList.CHANNELING, 60.0F);//CHANNELING

                skills.learnTemp(SkillList.GROUP_FIGHTING, 50.0f);
                skills.learnTemp(SkillList.FIGHT_AGGRESSIVESTYLE, 40.0f);
                skills.learnTemp(SkillList.FIGHT_DEFENSIVESTYLE, 40.0f);
                skills.learnTemp(SkillList.FIGHT_NORMALSTYLE, 40.0f);
                ReflectionUtil.setPrivateField(template, ReflectionUtil.getField(template.getClass(), "skills"), skills);
            } // if
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void changeRiftOgreMage() {
        try {
            CreatureTemplate template = CreatureTemplateFactory.getInstance().getTemplate(111);
            if(template != null) {
                template.setBaseCombatRating(50.0F);
                template.setCombatMoves(new int[] { 5, 14, 8 });

                Skills skills = SkillsFactory.createSkills("Rift Ogre Mage");
                skills.learnTemp(SkillList.BODY_STRENGTH, 60.0F);//BODY_STRENGTH
                skills.learnTemp(SkillList.BODY_CONTROL, 30.0F);//BODY_CONTROL
                skills.learnTemp(SkillList.BODY_STAMINA, 20.0F);//BODY_STAMINA
                skills.learnTemp(SkillList.MIND_LOGICAL, 50.0F);//MIND_LOGICAL
                skills.learnTemp(SkillList.MIND_SPEED, 50.0F);//MIND_SPEED
                skills.learnTemp(SkillList.SOUL_STRENGTH, 30.0F);//SOUL_STRENGTH
                skills.learnTemp(SkillList.SOUL_DEPTH, 30.0F);//SOUL_DEPTH
                skills.learnTemp(SkillList.WEAPONLESS_FIGHTING, 50.0F);//WEAPONLESS_FIGHTING
                skills.learnTemp(SkillList.CHANNELING, 50.0F);//CHANNELING

                skills.learnTemp(SkillList.GROUP_FIGHTING, 70.0f);
                skills.learnTemp(SkillList.FIGHT_AGGRESSIVESTYLE, 50.0f);
                skills.learnTemp(SkillList.FIGHT_DEFENSIVESTYLE, 50.0f);
                skills.learnTemp(SkillList.FIGHT_NORMALSTYLE, 50.0f);
                ReflectionUtil.setPrivateField(template, ReflectionUtil.getField(template.getClass(), "skills"), skills);
            } // if
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void changeRiftSummoner() {
        try {
            CreatureTemplate template = CreatureTemplateFactory.getInstance().getTemplate(112);
            if(template != null) {
                template.setBaseCombatRating(40.0F);
                template.setCombatMoves(new int[] { 15, 16 });

                Skills skills = SkillsFactory.createSkills("Rift Summoner");
                skills.learnTemp(SkillList.BODY_STRENGTH, 60.0F);//BODY_STRENGTH
                skills.learnTemp(SkillList.BODY_CONTROL, 30.0F);//BODY_CONTROL
                skills.learnTemp(SkillList.BODY_STAMINA, 30.0F);//BODY_STAMINA
                skills.learnTemp(SkillList.MIND_LOGICAL, 20.0F);//MIND_LOGICAL
                skills.learnTemp(SkillList.MIND_SPEED, 20.0F);//MIND_SPEED
                skills.learnTemp(SkillList.SOUL_STRENGTH, 40.0F);//SOUL_STRENGTH
                skills.learnTemp(SkillList.SOUL_DEPTH, 20.0F);//SOUL_DEPTH
                skills.learnTemp(SkillList.WEAPONLESS_FIGHTING, 50.0F);//WEAPONLESS_FIGHTING
                skills.learnTemp(SkillList.CHANNELING, 80.0F);//CHANNELING

                skills.learnTemp(SkillList.GROUP_FIGHTING, 70.0f);
                skills.learnTemp(SkillList.FIGHT_AGGRESSIVESTYLE, 50.0f);
                skills.learnTemp(SkillList.FIGHT_DEFENSIVESTYLE, 50.0f);
                skills.learnTemp(SkillList.FIGHT_NORMALSTYLE, 50.0f);
                ReflectionUtil.setPrivateField(template, ReflectionUtil.getField(template.getClass(), "skills"), skills);
            } // if
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendEventChatMessage(final String message, final int red, final int green, final int blue){
        String toSend = "";

        toSend = message;

        Player[] playarr = Players.getInstance().getPlayers();
        Message mess = new Message((Creature)null, (byte)16, Servers.localServer.getAbbreviation() + "-Event", "<Event> " + toSend, red, green, blue);

        for(int x = 0; x < playarr.length; ++x) {
            if (!playarr[x].getCommunicator().isInvulnerable() && playarr[x].isKingdomChat()) {
                playarr[x].getCommunicator().sendMessage(mess);
            }
        }
    }

    public CombatMove createMove(int _number, String _name, float _difficulty, String aActionString, float aBaseDamage, float aRarity, byte aWoundType){
        try{
            return ReflectionUtil.callPrivateConstructor(
                    CombatMove.class.getDeclaredConstructor(int.class, String.class, float.class, String.class, float.class, float.class, byte.class),
                    _number, _name, _difficulty,  aActionString, aBaseDamage, aRarity, aWoundType
            );
        }catch (Exception e){
            logger.warning(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static boolean doCombatMoves(String name, int number, Creature creature) {
        if ((number<12) || (number>19)) {
            return false;
        }
        if (!creature.isUnique() || creature.getHugeMoveCounter() >= 2) {
            creature.playAnimation("fight_" + name, false);
        }
        switch(number) {
            case 12:
                CreatureSpells.iceSpears(creature,5, 18000);
                break;
            case 13:
                CreatureSpells.freeze(creature,5, 20, 6000);
                break;
            case 14:
                CreatureSpells.chaoticExplosions(creature,3, 7, 60000);
                break;
            case 15:
                CreatureSpells.summonBeasts(creature,2);
                break;
            case 16:
                CreatureSpells.heal(creature,6);
                break;
            case 17:
                CreatureSpells.vaccuum(creature,3);
                break;
            case 18:
                CreatureSpells.unstableAura(creature,3, 6000);
                break;
            case 19:
                CreatureSpells.fatigue(creature, 3);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void preInit() {
        ModActions.init();

        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();
            Class<RiftMod> thisClass = RiftMod.class;

            CtClass ctCombatMove = classPool.getCtClass("com.wurmonline.server.combat.CombatMove");

            ctCombatMove.getMethod("perform","(Lcom/wurmonline/server/creatures/Creature;)V")
                    .insertBefore(String.format("if (%s.doCombatMoves(this.getName(), this.number, creature)) return; ", RiftMod.class.getName()));

            CtField f1 = CtField.make("public static final int ICE_SPEARS = 12;", ctCombatMove);
            ctCombatMove.addField(f1);
            CtField f2 = CtField.make("public static final int FREEZE = 13;", ctCombatMove);
            ctCombatMove.addField(f2);
            CtField f3 = CtField.make("public static final int CHAOTIC_EXPLOSIONS = 14;", ctCombatMove);
            ctCombatMove.addField(f3);
            CtField f4 = CtField.make("public static final int SUMMON_BEASTS = 15;", ctCombatMove);
            ctCombatMove.addField(f4);
            CtField f5 = CtField.make("public static final int HEAL = 16;", ctCombatMove);
            ctCombatMove.addField(f5);
            CtField f6 = CtField.make("public static final int VACUUM = 17;", ctCombatMove);
            ctCombatMove.addField(f6);
            CtField f7 = CtField.make("public static final int UNSTABLE_AURA = 18;", ctCombatMove);
            ctCombatMove.addField(f7);
            CtField f8 = CtField.make("public static final int FATIGUE = 19;", ctCombatMove);
            ctCombatMove.addField(f8);

            createMove(12, "Ice Spears", 20.0f, " casts Ice Spears!", 5000.0f, 0.07f, (byte) 9);
            createMove(13, "Freeze", 20.0f, " casts Freeze!", 5000.0f, 0.03f, (byte) 9);
            createMove(14, "Chaotic Explosions", 20.0f, " casts Chaotic Explosions!", 5000.0f, 0.05f, (byte) 9);
            createMove(15, "Summon Beasts", 20.0f, " casts Summon Beasts!", 5000.0f, 0.05f, (byte) 9);
            createMove(16, "Heal", 20.0f, " casts Heal!", 5000.0f, 0.1f, (byte) 9);
            createMove(17, "Vacuum", 20.0f, " casts Vacuum!", 5000.0f, 0.015f, (byte) 9);
            createMove(18, "Unstable Aura", 20.0f, " casts Unstable Aura!", 5000.0f, 0.04f, (byte) 9);
            createMove(19, "Fatigue", 20.0f, " casts Fatigue!", 5000.0f, 0.05f, (byte) 9);


            //String replace;
            //CtClass ctCombatHandler = classPool.get("com.wurmonline.server.creatures.CombatHandler");
            //CtClass ctCreature = classPool.get("com.wurmonline.server.creatures.Creature");
            //CtClass ctAction = classPool.get("com.wurmonline.server.behaviours.Action");
            //CtClass[] params4 = {
            //        ctCreature,
            //        CtClass.intType,
            //        CtClass.booleanType,
            //        CtClass.floatType,
            //        ctAction
            //};
            //String desc4 = Descriptor.ofMethod(CtClass.booleanType, params4);
            //ctCombatHandler.getMethod("attack", desc4).insertBefore(RiftMod.class.getName() + ".attackHandled($0.creature, $1, $2, $3, $4, $5);");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() {
        try {
            CtClass[] paramTypes1 = {
                    HookManager.getInstance().getClassPool().get("com.wurmonline.server.creatures.Creature"),
                    HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
                    HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item"),
                    CtPrimitiveType.floatType,
                    HookManager.getInstance().getClassPool().get("com.wurmonline.server.behaviours.Action")
            };

            HookManager.getInstance().registerHook("com.wurmonline.server.behaviours.MethodsItems", "gatherRiftResource", Descriptor.ofMethod(CtPrimitiveType.booleanType, paramTypes1), new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
                        @Override
                        public Object invoke(Object object, Method method, Object[] args) throws Throwable {
                            if (activeRifts.stream().anyMatch(rift -> rift.event.state == Rift.State.Gathering)) {
                                return method.invoke(object, args);
                            }
                            return true;
                        }
                    };
                }
            });
        } catch (NotFoundException e ) {
            throw new HookException(e);
        }
    }

    @Override
    public void onServerStarted() {
        changeRiftBeast();
        changeRiftJackal();
        changeRiftOgre();
        changeRiftWarmaster();
        changeRiftCaster();
        changeRiftOgreMage();
        changeRiftSummoner();

        RiftModDatabase.onServerStarted();
        scheduledRiftTime = resheduleRiftTimerOnStart ? 0 : RiftModDatabase.getScheduledRiftTime();
        if (scheduledRiftTime == 0) {
            scheduleNextRiftTime();
        }
        List<RiftEventModel> models = RiftEventModel.getAllActive();
        models.forEach(model -> addActiveRift(new Rift(model)));
    }

    @Override
    public void onServerPoll() {
        long currentTime = System.currentTimeMillis();
        if ((lastProcessedTick + TICK * TimeConstants.SECOND_MILLIS) < currentTime) {
            lastProcessedTick = currentTime;
            Predicate<Rift> isDestroyed = Rift::isDestroyed;
            activeRifts.removeIf(isDestroyed);
            activeRifts.forEach(Rift::processRiftTick);
        }
        if (scheduledRiftTime != 0 && scheduledRiftTime < currentTime && activeRifts.isEmpty()) {
            if (spotFinder.coordinates == null) {
                spotFinder.findCoordinates();
            } else {
                RiftEventModel model = RiftEventModel.makeByScheduledTime(scheduledRiftTime, spotFinder.coordinates.getX(), spotFinder.coordinates.getY());
                RiftMod.logger.log(Level.INFO, "RIFT: on server poll " + scheduledRiftTime + " < " + currentTime);
                addActiveRift(new Rift(model));
                spotFinder.coordinates = null;
            }
        }
    }

    private void addActiveRift(Rift rift) {
        if (rift == null) return;
        activeRifts.add(rift);
    }

    public static void scheduleNextRiftTime() {
        int dayOfWeekWhenRiftStarted = scheduleRiftInDayOfWeek;
        int hourWhenRiftStarted = scheduleRiftAtHour;
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        switch (currentDayOfWeek) {
            case Calendar.SUNDAY:
                currentDayOfWeek = 7;
                break;
            case Calendar.MONDAY:
                currentDayOfWeek = 1;
                break;
            case Calendar.TUESDAY:
                currentDayOfWeek = 2;
                break;
            case Calendar.WEDNESDAY:
                currentDayOfWeek = 3;
                break;
            case Calendar.THURSDAY:
                currentDayOfWeek = 4;
                break;
            case Calendar.FRIDAY:
                currentDayOfWeek = 5;
                break;
            case Calendar.SATURDAY:
                currentDayOfWeek = 6;
                break;
        }
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        int addedDaysToNextStartRift = 0;
        if (currentDayOfWeek == dayOfWeekWhenRiftStarted) {
            if (currentHour >= hourWhenRiftStarted) {
                addedDaysToNextStartRift = 7;
            }
        } else if (currentDayOfWeek < dayOfWeekWhenRiftStarted) {
            addedDaysToNextStartRift = dayOfWeekWhenRiftStarted - currentDayOfWeek;
        } else {
            //addedDaysToNextStartRift = dayOfWeekWhenRiftStarted + (currentDayOfWeek - dayOfWeekWhenRiftStarted);
            addedDaysToNextStartRift = dayOfWeekWhenRiftStarted + (7 - currentDayOfWeek);
        }
        RiftMod.logger.log(Level.INFO, "RIFT: current day of week is " + currentDayOfWeek + " current hour " + currentHour + " need add days " + addedDaysToNextStartRift);

        calendar.set(Calendar.HOUR_OF_DAY, hourWhenRiftStarted);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DATE, addedDaysToNextStartRift);
        scheduledRiftTime = calendar.getTimeInMillis();
        RiftModDatabase.setNextRiftTime(scheduledRiftTime);
        RiftMod.logger.log(Level.INFO, "RIFT: Rift has been scheduled " + calendar.getTime().toString());
    }

    public static void attackHandled(Creature attacker, Creature opponent, int combatCounter, boolean opportunity, float actionCounter, Action act) {
        //RiftMod.logger.log(Level.INFO, "RIFT: attacker " + attacker.getName() + " opponent " + opponent.getName() + " action " + act.toString());
    }
}