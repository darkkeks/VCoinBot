package com.darkkeks.vcoin.bot.stock;

import com.darkkeks.vcoin.bot.network.VCoinHandler;
import com.darkkeks.vcoin.bot.network.VCoinListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockController implements VCoinListener {

    private static final Logger logger = LoggerFactory.getLogger(StockController.class);

    private int stockId;
    private int sinkId;
    private Database database;

    private long locked;

    public StockController(int stockId, int sinkId, Database database) {
        this.stockId = stockId;
        this.sinkId = sinkId;
        this.database = database;
    }

    @Override
    public void onStart(VCoinHandler client) {
        if(client.getId() == sinkId) {
            logger.info("Started");
        }
    }

    @Override
    public void onStatusUpdate(VCoinHandler client) {
        if(client.getId() == stockId && client.getScore() > locked) {
            client.transfer(sinkId, client.getScore() - locked);
        }
    }

    @Override
    public void onTransfer(VCoinHandler client, long delta, int from) {
        if(client.getId() == stockId) {
            logger.info("Incoming transfer from {} of {} coins", from, delta);
            locked += delta;
            boolean result = database.createTransaction(from, client.getId(), delta);
            result &= database.addScore(from, delta);
            if(!result) {
                logger.error("Error saving transfer to database, returning");
                client.transfer(from, delta).thenAccept(error -> {
                    if(!error.isPresent()) {
                        locked -= delta;
                    } else {
                        logger.error("Error returning back to user - {} (From {}, amount {})",
                                error.get(), from, delta);
                    }
                });
            } else {
                locked -= delta;
            }
        }
    }
}
