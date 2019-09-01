package think.rpgitems.power.impl;

import cat.nyaa.nyaacore.Pair;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public abstract class BasePropertyHolder implements PropertyHolder {

    RPGItem item;

    @Override
    public void init(ConfigurationSection section) {
        PowerMeta powerMeta = this.getClass().getAnnotation(PowerMeta.class);
        Map<String, Pair<Method, PowerProperty>> properties = PowerManager.getProperties(this.getClass());
        for (Map.Entry<String, Pair<Method, PowerProperty>> entry : properties.entrySet()) {
            String name = entry.getKey();
            PowerProperty property = entry.getValue().getValue();
            Field field = property.field();
            if (name.equals("triggers") && powerMeta.immutableTrigger()) {
                continue;
            }
            if (field.getType().isAssignableFrom(ItemStack.class)) {
                ItemStack itemStack = section.getItemStack(name);
                if (itemStack != null) {
                    try {
                        field.set(this, itemStack);
                        continue;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            String value = section.getString(name);
            if (value == null) {
                for (String alias : property.alias()) {
                    value = section.getString(alias);
                    if (value != null) break;
                }
            }
            if (name.equals("cost") && value == null) {
                value = section.getString("consumption");
            }
            if (value != null) {
                Utils.setPowerPropertyUnchecked(Bukkit.getConsoleSender(), this, field, value);
            }
        }
    }

    @Override
    public void save(ConfigurationSection section) {
        Map<String, Pair<Method, PowerProperty>> properties = PowerManager.getProperties(this.getClass());
        PowerMeta powerMeta = this.getClass().getAnnotation(PowerMeta.class);

        for (Map.Entry<String, Pair<Method, PowerProperty>> entry : properties.entrySet()) {
            String name = entry.getKey();
            PowerProperty property = entry.getValue().getValue();
            Field field = property.field();
            if (name.equals("triggers") && powerMeta.immutableTrigger()) {
                continue;
            }
            try {
                Utils.saveProperty(this, section, name, field);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public NamespacedKey getNamespacedKey() {
        return new NamespacedKey(RPGItems.plugin, getName());
    }

    @Override
    public String getLocalizedName(String locale) {
        return I18n.format("power.properties." + getName() + ".main_name");
    }

    @Override
    public RPGItem getItem() {
        return item;
    }

    @Override
    public void setItem(RPGItem item) {
        this.item = item;
    }
}