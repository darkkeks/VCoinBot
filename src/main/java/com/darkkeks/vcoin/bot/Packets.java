package com.darkkeks.vcoin.bot;

public class Packets {

    public static Packet buy(Item item) {
        return id -> String.format("P%d B %s", id, item.getName());
    }

    public static Packet click(int count, int randomId) {
        return id -> String.format("C%d %d %d", count, randomId, 1);
    }

    public static Packet transfer(int user, long amount) {
        return id -> String.format("P%d T %d %d", id, user, amount);
    }

    public static Packet captcha(int randomId, String result) {
        return id -> String.format("C%d %d %s", id, randomId, result);
    }
}
