package com.thespuff.plugins.telepad;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class TelePad
        extends JavaPlugin
        implements Listener
{
  public static String pluginName;
  public static String pluginVersion;
  public static Server server;
  public static TelePad plugin;
  private Set<Player> teleportWarm;


  public void onDisable(){
    saveConfig();
    log("Disabled");
  }

  public void onEnable(){
    pluginName = getDescription().getName();
    pluginVersion = getDescription().getVersion();
    server = getServer();
    plugin = this;
    this.teleportWarm = new HashSet();

    getServer().getPluginManager().registerEvents(this, this);

    setMetadata();

    log("Enabled.");
  }

  public void log(String paramString)
  {
    System.out.println("[" + pluginName + "] " + paramString);
  }

  public void log(int paramInt)
  {
    log(String.valueOf(paramInt));
  }

  private void setMetadata(){
    log("Setting TelePad Metadata...");

    if(!this.getConfig().contains("telepads")) this.saveDefaultConfig();
    try{
      Set<String> keys=getConfig().getConfigurationSection("telepads").getKeys(false);

      for(String key : keys){
        String destination=getConfig().getString("telepads." + key + ".destination");
        getLocation(key).getBlock().setMetadata("TelePad", new FixedMetadataValue(this, destination));
      }
    } catch (Exception e){
      log("Failed to load TelePads.");
    }
  }

  //------------------------------------------------------------

  @EventHandler
  public void onPlayerMakeTelePad(BlockBreakEvent event){
    if(!event.getPlayer().isOp()) return;
    if(!(event.getBlock() instanceof Sign)) return;

    Sign sign = (Sign) event.getBlock();
 
    if(sign.getLine(1)!="TelePad") return;

    saveLocation(sign.getLine(0), event.getBlock().getLocation(), sign.getLine(2));
    event.getPlayer().sendMessage("TelePad created");
  }

  @EventHandler
  public void onEnterTelepad(PlayerMoveEvent event){
    if(event.getFrom().equals(event.getTo())) return;
    if(this.teleportWarm.contains(event.getPlayer())) return;
    final Player localPlayer = event.getPlayer();
    Block block = localPlayer.getLocation().getBlock();
    if(!block.hasMetadata("TelePad")) return;
    String destinationPad=block.getMetadata("TelePad").get(0).asString();
    this.teleportWarm.add(localPlayer);
    localPlayer.teleport(getLocation(destinationPad));
    server.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
      public void run(){TelePad.this.cool(localPlayer);}}, 80L);
  }

  private void cool(Player paramPlayer)
  {
    if (this.teleportWarm.contains(paramPlayer)) {
      this.teleportWarm.remove(paramPlayer);
    }
  }

  private void saveLocation(String padName, Location location, String destinationName){
    getConfig().set("telepads."+padName+".world", location.getWorld().getName());
    getConfig().set("telepads."+padName+".x", location.getX());
    getConfig().set("telepads."+padName+".y", location.getY());
    getConfig().set("telepads."+padName+".z", location.getZ());
    getConfig().set("telepads."+padName+".destination", destinationName);
    saveConfig();
  }

  private Location getLocation(String padName){
    ConfigurationSection telepads = getConfig().getConfigurationSection("telepads");
    if(telepads.contains(padName)){
      String world=telepads.getString(padName+".world");
      double x=telepads.getDouble(padName+".x");
      double y=telepads.getDouble(padName+".y");
      double z=telepads.getDouble(padName+".z");
      return new Location(server.getWorld(world),x,y,z);
    }

    return server.getWorlds().get(0).getSpawnLocation();
  }

  public boolean onCommand(CommandSender paramCommandSender, Command paramCommand, String paramString, String[] paramArrayOfString)
  {
    if (paramCommand.getName().equalsIgnoreCase("telepadreload"))
    {
      reloadConfig();
      log("Config reloaded.");
      return true;
    }
    return false;
  }
}