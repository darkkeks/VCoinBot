package com.darkkeks.vcoin.bot.network;

public interface VCoinListener {
    default void onStart(VCoinHandler client) {}

    default void onStop(VCoinHandler client) {}

    default void onStatusUpdate(VCoinHandler client) {}

    default void onTransfer(VCoinHandler client, long delta, int from) {}
}
