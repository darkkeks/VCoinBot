package com.darkkeks.vcoin.bot.sink;

import com.darkkeks.vcoin.bot.Util;
import com.darkkeks.vcoin.bot.network.WSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class SinkLauncher {

    private static final Logger logger = LoggerFactory.getLogger(SinkLauncher.class);

    private static final int SINK_ID = 456171173;

    private static final String TOKEN_VARIABLE = "BOT_TOKEN";


    public static void main(String[] args) {
        new SinkLauncher().run();
    }

    private void run() {
        String token = Util.getenv(TOKEN_VARIABLE).orElseGet(() -> {
            logger.warn("WARNING: no token in env variables, using \"debug\"");
            return "debug";
        });

        SinkController sinkController = new SinkController(SINK_ID);

        WebServer server = new WebServer(token, sinkController);
        WSFactory websocketFactory = new WSFactory();

        server.start();

        Util.readUrls().stream()
                .filter(url -> !url.startsWith("#"))
                .map(Util::getWsUrl)
                .map(Util::toUri)
                .filter(Objects::nonNull)
                .forEach(url -> websocketFactory.connect(url, sinkController));
    }
}
