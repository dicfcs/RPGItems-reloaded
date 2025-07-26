package think.rpgitems.power.impl;

import org.bukkit.inventory.EquipmentSlotGroup;
import think.rpgitems.I18n;
import think.rpgitems.power.*;

@Meta
public class CooldownReduce extends BasePower {
    @Property
    public float amount = 1; // 100% or 1 tick reduction by default

    public enum Operation {
        SUBTRACT,
        MULTIPLY
    }

    @Property
    public Operation operation = Operation.MULTIPLY;

    @Property
    public EquipmentSlotGroup slot = null;

    @Override
    public String getName() {
        return "cooldownreduce";
    }

    public float getAmount() {
        return amount;
    }

    public Operation getOperation() {
        return operation;
    }

    public EquipmentSlotGroup getSlot() {
        return slot;
    }

    @Override
    public String displayText() {
        // TODO: i18n
        return I18n.formatDefault("power.cooldownreduce");
    }
}
