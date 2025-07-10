package think.rpgitems.power.impl;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.fasterxml.jackson.databind.annotation.JsonAppend.Prop;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.minecraft.world.item.alchemy.Potion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.eclipse.sisu.Parameters;

import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.event.BeamEndEvent;
import think.rpgitems.event.BeamHitBlockEvent;
import think.rpgitems.event.BeamHitEntityEvent;
import think.rpgitems.event.PowerActivateEvent;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Power immunity.
 * <p>
 * The immunity power will allow the player become immune to some status effects
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", generalInterface = {
        PowerLeftClick.class,
        PowerRightClick.class,
        PowerPlain.class,
        PowerSneak.class,
        PowerLivingEntity.class,
        PowerSprint.class,
        PowerHurt.class,
        PowerHit.class,
        PowerHitTaken.class,
        PowerBowShoot.class,
        PowerBeamHit.class,
        PowerLocation.class
}, implClass = Immunity.Impl.class)
public class Immunity extends BasePower {
    private static final AtomicInteger rc = new AtomicInteger(0);
    private static Listener listener;
    // private static final Cache<UUID, Long> stucked = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).concurrencyLevel(2).build();
    // private static final Cache<UUID, Long> unstucked = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).concurrencyLevel(2).build();
    public record PotionEffectData(long timestamp, String effect) {}
    private static final Cache<UUID, PotionEffectData> potioned = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .concurrencyLevel(2)
            .build();
    public enum Type {
        POTION_EFFECT,
        POTION_ALL,
        STUCK
    }

    @Deserializer(PotionEffectUtils.class)
    @Serializer(PotionEffectUtils.class)
    @Property(order = 3, required = true)
    @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
    public PotionEffectType potionEffect = PotionEffectType.BLINDNESS;

    @Property
    public Type type = Type.STUCK;

    @Property
    public boolean broadcast = false; // Whether to broadcast the immunity activation message

    // 1/x
    @Property
    public int chance = 1;

    @Property
    public int cooldown = 0;

    @Property
    public int cost = 0;

    @Property(order = 1)
    public int duration = 100; // in ticks, 5 seconds by default

    @Override
    public void init(ConfigurationSection section) {
        int orc = rc.getAndIncrement();
        super.init(section);
        if (orc == 0) {
            listener = new Listener() {
                // Handle stuck effect
                // Handle potion effects
                @EventHandler
                void onPotionEffect(EntityPotionEffectEvent e) {
                    try {
                        Long timestamp = potioned.get(e.getEntity().getUniqueId(), () -> new PotionEffectData(Long.MIN_VALUE, "")).timestamp;
                        if (timestamp >= (System.currentTimeMillis() - getDuration() * 50)) {
                            if (getType() == Type.POTION_EFFECT && e.getNewEffect().getType().equals(getPotionEffect())) {
                                e.setCancelled(true);
                            } else if (getType() == Type.POTION_ALL) {
                                e.setCancelled(true);
                            }
                        }
                    } catch (ExecutionException ex) {
                        ex.printStackTrace();
                    }
                }
            };
            Bukkit.getPluginManager().registerEvents(listener, RPGItems.plugin);
        }
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    /**
     * Chance of this power
     */
    public int getChance() {
        return chance;
    }

    /**
     * Cooldown time of this power
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Cost of this power
     */
    public int getCost() {
        return cost;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public String getName() {
        return "immunity";
    }

    /**
     * Potion effect type of this power
     */
    public PotionEffectType getPotionEffect() {
        return potionEffect;
    }

    /**
     * Type of this power
     */
    public Type getType() {
        return type;
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.immunity.display",
                "<lang:potion.minecraft." + getPotionEffect().key().value() + ">",
                getPotionEffect().getCategory() == PotionEffectTypeCategory.BENEFICIAL ? "beneficial" :
                        getPotionEffect().getCategory() == PotionEffectTypeCategory.HARMFUL ? "harmful" : "neutral",
                getChance());
    }

    public class Impl implements PowerHit, PowerHitTaken, PowerLeftClick, PowerRightClick, PowerOffhandClick, PowerProjectileHit, PowerSneak, PowerSneaking, PowerSprint, PowerOffhandItem, PowerMainhandItem, PowerTick, PowerPlain, PowerLivingEntity, PowerHurt, PowerBowShoot, PowerBeamHit, PowerConsume, PowerJump, PowerSwim {

        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            return fire(player, stack, null, null);
        }

        @Override
        public PowerResult<Void> fire(Player player, ItemStack stack, LivingEntity entity, Double damage) {
            HashMap<String,Object> argsMap = new HashMap<>();
            argsMap.put("target",entity);
            argsMap.put("damage",damage);
            PowerActivateEvent powerEvent = new PowerActivateEvent(player,stack,getPower(),argsMap);
            if(!powerEvent.callEvent()) {
                return PowerResult.fail();
            }
            potioned.put(player.getUniqueId(), new PotionEffectData(System.currentTimeMillis(), getPotionEffect().key().value()));
            if (isBroadcast()) {
                player.sendMessage(I18n.formatDefault("power.immunity.info", getPotionEffect().key().value()));
            }
            return PowerResult.ok();
        }

        @Override
        public Power getPower() {
            return Immunity.this;
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
            if (event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack, null, damage).with(damage);
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
            if (event instanceof EntityDamageByEntityEvent) {
                return fire(target, stack, null, event.getDamage());
            }
            return PowerResult.noop();
        }

        @Override
        public PowerResult<Void> offhandClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> projectileHit(Player player, ItemStack stack, ProjectileHitEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Boolean> swapToMainhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
            return fire(player, stack).with(true);
        }

        @Override
        public PowerResult<Boolean> swapToOffhand(Player player, ItemStack stack, PlayerSwapHandItemsEvent event) {
            return fire(player, stack).with(true);
        }

        @Override
        public PowerResult<Void> tick(Player player, ItemStack stack) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sneaking(Player player, ItemStack stack) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(player, itemStack).with(e.getForce());
        }

        @Override
        public PowerResult<Double> hitEntity(Player player, ItemStack stack, LivingEntity entity, double damage, BeamHitEntityEvent event) {
            return fire(player, stack, entity, damage).with(damage);
        }

        @Override
        public PowerResult<Void> hitBlock(Player player, ItemStack stack, Location location, BeamHitBlockEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> beamEnd(Player player, ItemStack stack, Location location, BeamEndEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> consume(Player player, ItemStack stack, PlayerItemConsumeEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> jump(Player player, ItemStack stack, PlayerJumpEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> swim(Player player, ItemStack stack, EntityToggleSwimEvent event) {
            return fire(player, stack);
        }
    }
}
