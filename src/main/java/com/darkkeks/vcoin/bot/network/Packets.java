package com.darkkeks.vcoin.bot.network;

import java.util.List;
import java.util.stream.Collectors;

public class Packets {

    public static Packet transfer(int user, long amount) {
        return id -> String.format("P%d T %d %d", id, user, amount);
    }

    public static Packet captcha(int randomId, String result) {
        return id -> String.format("C%d %d %s", id, randomId, result);
    }

    public static Packet getScores(List<Integer> list) {
        String ids = list.stream().map(Object::toString).collect(Collectors.joining(" "));
        return id -> String.format("P%d GU %s", id, ids);
    }
}
