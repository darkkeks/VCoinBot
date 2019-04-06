package kek.darkkeks.twitch;

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

    public static int evaluateJS(String js) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        ScriptContext context = engine.getContext();
        StringWriter writer = new StringWriter();
        context.setWriter(writer);
 

	js = "var window = {};window['location'] = { host: \"https://vk.com\"};window['parseInt']=parseInt;" + js;

        try {
            Object obj = engine.eval(js);
            if(obj instanceof Double) {
                return ((Double) obj).intValue();
            }
        } catch (ScriptException ignored) { }
        return -1;
    }

    public static int extractUserId(String path) {
        Matcher matcher = Pattern.compile("vk_user_id=(\\d+)").matcher(path);
        matcher.find();
        return Integer.valueOf(matcher.group(1));
    }

    public static String getWsUrl(String path) {
        try {
            URL url = new URL(path);

            int userid = extractUserId(path);
            int channel = userid % 16;

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
        return (userid % 2 == 0 ? userid - 15 : userid - 109);
    }

}
