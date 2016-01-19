/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.panryba.mc.live;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

/**
 *
 * @author PanRyba.pl
 */
class PluginApi {
    private Server server;
    private static String redisHost;
    private static String redisPrefix;
    private Map<String, Date> lastActivity;
    private Map<String, Long> totalDiffs;

    public static void setup(String redisHost, String regisPrefix) {
        PluginApi.redisHost = redisHost;
        PluginApi.redisPrefix = regisPrefix;
    }
    
    private static ThreadLocal<Jedis> jedis = new ThreadLocal() {
        @Override
        protected Jedis initialValue() {
            return new Jedis(PluginApi.redisHost);
        }
    };

    public PluginApi(Server server) {
        this.server = server;
        this.lastActivity = new HashMap<>();
        this.totalDiffs = new HashMap<>();

        refreshOnlinePlayers();
    }

    void playerJoined(Player player) {
        addToOnlinePlayers(player);

        if (player.isOp()) {
            PluginApi.jedis.get().sadd(PluginApi.redisPrefix + ":ops", player.getName());
        }
    }

    void playerLeft(Player player) {
        String name = player.getName();

        Jedis localJedis = PluginApi.jedis.get();

        localJedis.srem(PluginApi.redisPrefix + ":players", name);
        localJedis.srem(PluginApi.redisPrefix + ":ops", name);
        localJedis.publish(PluginApi.redisPrefix + ":quit", name);
    }

    private void addToOnlinePlayers(Player player) {
        Jedis localJedis = PluginApi.jedis.get();

        String name = player.getName();
        localJedis.sadd(PluginApi.redisPrefix + ":players", name);
        localJedis.publish(PluginApi.redisPrefix + ":join", name);
    }

    private void refreshOnlinePlayers() {
        Jedis localJedis = PluginApi.jedis.get();
        localJedis.del(PluginApi.redisPrefix + ":players");

        for (Player player : this.server.getOnlinePlayers()) {
            addToOnlinePlayers(player);
        }
    }

    void onChat(Player player, String message) {
        Jedis localJedis = PluginApi.jedis.get();
        localJedis.publish(PluginApi.redisPrefix + ":chat", player.getName() + ": " + message);
    }

    void onPlayerActivity(Player player) {
        String name = player.getName();
        Date now = new Date();
        long nowLong = now.getTime();

        Jedis localJedis = PluginApi.jedis.get();

        Date last = getLastActivity(name);
        setLastActivity(name, now);

        if (last == null) {
            return;
        }

        long lastLong = last.getTime();
        long diff = nowLong - lastLong;

        if (diff > 1000) {
            this.totalDiffs.remove(name);            
            return;
        }

        Long totalDiff = this.totalDiffs.get(name);
        if (totalDiff == null) {
            totalDiff = 0L;
        }

        totalDiff += diff;
        
        if (totalDiff < 1000) {
            this.totalDiffs.put(name, totalDiff);
            return;
        }

        String totalKey = PluginApi.redisPrefix + ":act:tot:" + name;
        String totalString = localJedis.get(totalKey);

        long total;
        if (totalString != null) {
            total = Long.parseLong(totalString);
        } else {
            total = 0;
        }

        total += totalDiff;
        
        localJedis.set(totalKey, Long.toString(total));
        this.totalDiffs.remove(name);
    }

    private Date getLastActivity(String name) {
        return this.lastActivity.get(name);
    }

    private void setLastActivity(String name, Date now) {
        this.lastActivity.put(name, now);
    }
    
    private String replaceColors (String message)
    {
	message = message.replaceAll("(?i)&([a-f0-9])", "\u00A7$1");
        message = message.replaceAll("(?i)&r", ChatColor.RESET.toString());
        
        return message;
    }    

    void onChatApi(String message) {
        this.server.broadcastMessage(replaceColors(message));
        
        Jedis localJedis = PluginApi.jedis.get();
        localJedis.publish(PluginApi.redisPrefix + ":chat", message);
    }

    void onCommand(String msg) {
        Bukkit.getLogger().info("* EXECUTING: " + msg);
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), msg);
    }
}
