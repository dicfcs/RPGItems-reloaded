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

    public enum SlotGroup {
        ANY(EquipmentSlotGroup.ANY),
        MAINHAND(EquipmentSlotGroup.MAINHAND),
        OFFHAND(EquipmentSlotGroup.OFFHAND),
        HAND(EquipmentSlotGroup.HAND),
        FEET(EquipmentSlotGroup.FEET),
        LEGS(EquipmentSlotGroup.LEGS),
        CHEST(EquipmentSlotGroup.CHEST),
        HEAD(EquipmentSlotGroup.HEAD),
        ARMOR(EquipmentSlotGroup.ARMOR);

        private final EquipmentSlotGroup group;
        SlotGroup(EquipmentSlotGroup group) {
            this.group = group;
        }

        public EquipmentSlotGroup getGroup() {
            return group;
        }
    }

    @Property
    public Operation operation = Operation.MULTIPLY;

    @Property
    public SlotGroup slot = SlotGroup.ANY;

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

    public SlotGroup getSlot() {
        return slot;
    }

    @Override
    public String displayText() {
        // TODO: i18n
        return I18n.formatDefault("power.cooldownreduce");
    }
}
