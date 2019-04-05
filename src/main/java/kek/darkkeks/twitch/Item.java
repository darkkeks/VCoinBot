package kek.darkkeks.twitch;

import java.util.HashMap;
import java.util.Map;

public enum Item {
    CURSOR(30, 1),
    CPU(100, 3),
    CPU_STACK(1000, 10),
    COMPUTER(10000, 30),
    SERVER_VK(50000, 100),
    QUANTUM_PC(200000, 500),
    DATACENTER(5000000, 1000);

    private static final Map<String, Item> itemByName = new HashMap<>();

    static {
        for (Item value : values()) {
            itemByName.put(value.getName(), value);
        }
    }

    private String name;
    private int price;
    private int income;

    Item(int price, int income) {
        this.price = price;
        this.income = income;
        this.name = this.toString().toLowerCase();
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getIncome() {
        return income;
    }

    public static Item getItemByName(String name) {
        return itemByName.get(name);
    }
}
