package com.darkkeks.vcoin.bot.network;

public interface Packet {
    String serialize(int requestId);
}
