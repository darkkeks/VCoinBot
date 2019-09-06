package com.darkkeks.vcoin.bot.network;

public interface VCoinListener {
    void onStart(VCoinHandler client);

    void onStop(VCoinHandler client);

    void onStatusUpdate(VCoinHandler client);

    void onTransfer(VCoinHandler client, long delta, int from);
}
