/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.panryba.mc.live;

import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

/**
 *
 * @author PanRyba.pl
 */
public class Plugin extends JavaPlugin {
    
    private class ChatSubThread extends Thread {
        private ChatSubscribed sub;
        private String host;
        private String prefix;
        
        public ChatSubThread(ChatSubscribed sub, String host, String prefix) {
            this.sub = sub;
            this.host = host;
            this.prefix = prefix;
        }
        
        @Override
        public void run() {
            Jedis jedis = new Jedis(host);        
            jedis.subscribe(this.sub, this.prefix + ":chatapi");
        }
        
    }  
    
    private class ChatSubscribed extends JedisPubSub {
        
        private PluginApi api;
        
        public ChatSubscribed(PluginApi api) {
            this.api = api;
        }
        
        @Override
        public void onMessage(String channel, String msg) {
            api.onChatApi(msg);
        }

        @Override
        public void onPMessage(String string, String string1, String string2) {
        }

        @Override
        public void onSubscribe(String string, int i) {
        }

        @Override
        public void onUnsubscribe(String string, int i) {
        }

        @Override
        public void onPUnsubscribe(String string, int i) {
        }

        @Override
        public void onPSubscribe(String string, int i) {
            
        }
    }
    
    private class CommandsSubThread extends Thread {
        private CommandsSubscribed sub;
        private String host;
        private String prefix;
        
        public CommandsSubThread(CommandsSubscribed sub, String host, String prefix) {
            this.sub = sub;
            this.host = host;
            this.prefix = prefix;
        }
        
        @Override
        public void run() {
            Jedis jedis = new Jedis(host);        
            jedis.subscribe(this.sub, this.prefix + ":commandapi");
        }
        
    }    
    
    private class CommandsSubscribed extends JedisPubSub {
        private PluginApi api;
        
        public CommandsSubscribed(PluginApi api) {
            this.api = api;
        }
        
        @Override
        public void onMessage(String channel, String msg) {
            try
            {
                this.api.onCommand(msg);
            } catch(Exception ex) {
                Bukkit.getLogger().info(ex.toString());
            }
        }

        @Override
        public void onPMessage(String string, String string1, String string2) {
        }

        @Override
        public void onSubscribe(String string, int i) {
        }

        @Override
        public void onUnsubscribe(String string, int i) {
        }

        @Override
        public void onPUnsubscribe(String string, int i) {
        }

        @Override
        public void onPSubscribe(String string, int i) {
        }
    }
    
    private ChatSubscribed chatSub;
    private CommandsSubscribed commandsSub;
    
    @Override
    public void onEnable() {
        FileConfiguration config = getConfig();
        
        String redisHost = config.getString("redis.host");
        String redisPrefix = config.getString("redis.prefix");
        
        PluginApi.setup(redisHost, redisPrefix);
        PluginApi api = new PluginApi(getServer());
        
        chatSub = new ChatSubscribed(api);        
        ChatSubThread chatThread = new ChatSubThread(chatSub, redisHost, redisPrefix);
        chatThread.start();

        commandsSub = new CommandsSubscribed(api);        
        CommandsSubThread commandsThread = new CommandsSubThread(commandsSub, redisHost, redisPrefix);
        commandsThread.start();
        
        getServer().getPluginManager().registerEvents(new LiveListener(api), this);        
    }

    @Override
    public void onDisable() {
        if(this.chatSub != null) {
            this.chatSub.unsubscribe();
        }
        
        if(this.commandsSub != null) {
            this.commandsSub.unsubscribe();
        }
    }
    
    
}
