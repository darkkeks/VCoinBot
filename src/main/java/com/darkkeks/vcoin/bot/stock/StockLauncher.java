package com.darkkeks.vcoin.bot.stock;

import com.darkkeks.vcoin.bot.Util;
import com.darkkeks.vcoin.bot.network.WSFactory;

import java.util.Objects;

public class StockLauncher {

    private static final int SINK_ID = 456171173;
    private static final int STOCK_ID = 482252730;

    private static final String DATABASE_URL = "DATABASE_URL";
    private static final String DATABASE_USERNAME = "DATABASE_USERNAME";
    private static final String DATABASE_PASSWORD = "DATABASE_PASSWORD";

    public static void main(String[] args) {
        new StockLauncher().run();
    }

    private void run() {
        String databaseUrl = Util.getenv(DATABASE_URL).orElseThrow(IllegalStateException::new);
        String databaseUsername = Util.getenv(DATABASE_USERNAME).orElseThrow(IllegalStateException::new);
        String databasePassword = Util.getenv(DATABASE_PASSWORD).orElseThrow(IllegalStateException::new);

        Database database = new Database(databaseUrl, databaseUsername, databasePassword);
        StockController stockController = new StockController(STOCK_ID, SINK_ID, database);

        WSFactory websocketFactory = new WSFactory(0, 4);

        Util.readUrls().stream()
                .filter(url -> !url.startsWith("#"))
                .map(Util::getWsUrl)
                .map(Util::toUri)
                .filter(Objects::nonNull)
                .forEach(url -> websocketFactory.connect(url, stockController));
    }

}
