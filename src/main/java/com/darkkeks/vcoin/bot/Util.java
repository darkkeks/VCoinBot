package com.darkkeks.vcoin.bot;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    public static String evaluateJS(String js) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        ScriptContext context = engine.getContext();
        StringWriter writer = new StringWriter();
        context.setWriter(writer);

    	js = "var window = {};window['location'] = { host: \"https://vk.com\"};window['parseInt']=parseInt;" + js;

        try {
            Object obj = engine.eval(js);
            if(obj instanceof Double) {
                return String.valueOf(((Double) obj).intValue());
            }
            return String.valueOf(obj);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException(js);
    }

    public static int extractUserId(String path) {
        Matcher matcher = Pattern.compile("vk_user_id=(\\d+)").matcher(path);
        boolean found = matcher.find();
        if(!found) {
            throw new IllegalArgumentException("path");
        }
        return Integer.valueOf(matcher.group(1));
    }

    public static String getWsUrl(String path) {
        try {
            URL url = new URL(path);

            int userid = extractUserId(path);
            int channel = userid % 32;

            return url.getProtocol().replace("http", "ws").replace("https", "wss") +
                    "://" +
                    url.getHost() +
                    "/channel/" +
                    channel +
                    "?" +
                    url.getQuery() +
                    "&ver=1&pass=" + hashPassCoin(userid);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int hashPassCoin(int userid) {
        return userid - 1;
    }
}
