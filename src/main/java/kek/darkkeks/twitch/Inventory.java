package kek.darkkeks.twitch;

import com.google.gson.JsonElement;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicReference;

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
            income += item.getIncome();
            count.get(item).add();
        });
    }

    public ItemStack getBestItem(long score) {
        AtomicReference<Double> bestRatio = new AtomicReference<>((double) 0);
        AtomicReference<ItemStack> best = new AtomicReference<>();
        count.forEach((item, stack) -> {
            double currentRatio = 1.0 * item.getIncome() / stack.getNextPrice();

            if(stack.getNextPrice() <= score * 2 && currentRatio > bestRatio.get()) {
                bestRatio.set(currentRatio);
                best.set(stack);
            }
        });
        return best.get();
    }

    public long getIncome() {
        return income;
    }

}
