package me.islandscout.hawk.modules;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.PhantomBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;

public class PlayerManager implements Listener {

    private final Hawk hawk;

    public PlayerManager(Hawk hawk) {
        this.hawk = hawk;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        Player p = e.getPlayer();
        hawk.addProfile(p); //This line is necessary since it must get called BEFORE hawk listens to the player's packets
        hawk.getHawkPlayer(p).setOnline(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        hawk.removeProfile(e.getPlayer().getUniqueId());
        hawk.getCheckManager().removeData(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (!e.getTo().getWorld().equals(e.getFrom().getWorld())) {
            return;
        }
        HawkPlayer pp = hawk.getHawkPlayer(e.getPlayer());
        pp.setTeleporting(true);
        pp.setTeleportLoc(e.getTo());
        pp.setLocation(e.getTo());
        pp.setLastTeleportTime(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void worldChangeEvent(PlayerChangedWorldEvent e) {
        HawkPlayer pp = hawk.getHawkPlayer(e.getPlayer());
        pp.setTeleporting(true);
        pp.setTeleportLoc(e.getPlayer().getLocation());
        pp.setLocation(e.getPlayer().getLocation());
        pp.setLastTeleportTime(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        HawkPlayer pp = hawk.getHawkPlayer(e.getPlayer());
        pp.setTeleporting(true);
        pp.setTeleportLoc(e.getRespawnLocation());
    }

    //TODO: No... just, no...
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent e) {
        HawkPlayer pp = hawk.getHawkPlayer(e.getPlayer());
        Bukkit.getScheduler().scheduleSyncDelayedTask(hawk, () -> {
            PhantomBlock pBlockDel = null;
            for (PhantomBlock pBlock : pp.getPhantomBlocks()) {
                Location a = pBlock.getLocation();
                Location b = e.getBlockPlaced().getLocation();
                if ((int) a.getX() == (int) b.getX() && (int) a.getY() == (int) b.getY() && (int) a.getZ() == (int) b.getZ()) {
                    pBlockDel = pBlock;
                    break;
                }
            }
            if (pBlockDel == null)
                return;
            pp.getPhantomBlocks().remove(pBlockDel);
        }, 1 + pp.getPing());
    }

}