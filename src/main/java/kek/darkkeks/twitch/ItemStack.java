package kek.darkkeks.twitch;

public class ItemStack {
    private Item item;
    private int count;
    private long nextPrice;

    public ItemStack(Item item) {
        this.item = item;
        nextPrice = item.getPrice();
    }

    public void add() {
        count += 1;
        nextPrice = (long) Math.ceil(1.3 * nextPrice);
    }

    public long getNextPrice() {
        return nextPrice;
    }

    public Item getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }
}
