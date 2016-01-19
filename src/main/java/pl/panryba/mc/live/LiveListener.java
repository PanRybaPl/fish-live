/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.panryba.mc.live;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author PanRyba.pl
 */
class LiveListener implements Listener {
    private PluginApi api;
    
    public LiveListener(PluginApi api) {
        this.api = api;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        api.playerJoined(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        api.playerLeft(player);
    }
    
    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if(event.isCancelled())
            return;
        
        api.onChat(event.getPlayer(), event.getMessage());
    }
    
    /*
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(event.isCancelled())
            return;
        
        api.onPlayerActivity(event.getPlayer());
    }
    */
}
