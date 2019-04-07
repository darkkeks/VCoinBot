package kek.darkkeks.twitch;

import com.google.gson.JsonElement;

import java.util.EnumMap;
import java.util.Map;

public class Inventory {

    private EnumMap<Item, ItemStack> count;
    private long income;

    public Inventory(JsonElement obj) {
        this.count = new EnumMap<>(Item.class);
        for(Item item : Item.values()) {
            count.put(item, new ItemStack(item));
        }

        obj.getAsJsonArray().forEach(name -> {
            Item item = Item.getItemByName(name.getAsString());
            if(item != null) {
                income += item.getIncome();
                count.get(item).add();
            }
        });
    }

    public ItemStack getBestItem() {
        double bestRatio = 0;
        ItemStack best = null;
        for (Map.Entry<Item, ItemStack> entry : count.entrySet()) {
            Item item = entry.getKey();
            ItemStack stack = entry.getValue();
            double currentRatio = 1.0 * item.getIncome() / stack.getNextPrice();

            if (currentRatio > bestRatio) {
                bestRatio = currentRatio;
                best = stack;
            }
        }
        return best;
    }

    public long getIncome() {
        return income;
    }

}
