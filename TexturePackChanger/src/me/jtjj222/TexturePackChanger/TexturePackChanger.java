package me.jtjj222.TexturePackChanger;

import java.util.HashMap;
import java.util.Set;

import net.minecraft.server.v1_4_6.EntityPlayer;
import net.minecraft.server.v1_4_6.Packet;
import net.minecraft.server.v1_4_6.Packet250CustomPayload;

import org.bukkit.ChatColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;


public class TexturePackChanger extends JavaPlugin implements Listener{
	
	HashMap<String,String> PlayerTexturePacks = new HashMap<String,String>(); //<player name, texture pack name>
	HashMap<String,String> TexturePacksbyName = new HashMap<String,String>(); //<tpack name, tpack url>
	
	
	public void onEnable() {
		
		this.saveDefaultConfig();
		getServer().getPluginManager().registerEvents(this, this);
		
		//load the texture pack url's
		ConfigurationSection TexturesConfig = getConfig().getConfigurationSection("Textures.");

		Set<String> tPackNames = TexturesConfig.getKeys(false);
		
		for (String key: tPackNames) {		
			TexturePacksbyName.put(key,getConfig().getString("Textures."+key+".url"));			
		}
		
		//then load the players choice of texture
		ConfigurationSection playerConfig = getConfig().getConfigurationSection("Players.");

		Set<String> players = playerConfig.getKeys(false);
		
		for (String key: players) 	{
			PlayerTexturePacks.put(key,getConfig().getString("Players."+key+".texture"));
		}	

	}
	
	public void UpdateTextureInConfig(String playername) {		
		
		getConfig().set("Players."+playername+".texture", PlayerTexturePacks.get(playername));
		
		saveConfig();
	}
		
	@EventHandler
	public void onPlayerJoinWorld(PlayerJoinEvent e) {				
		final Player p = e.getPlayer();
		
		if (PlayerTexturePacks.containsKey(p.getName())) {
			p.sendMessage(ChatColor.GREEN + "You are currently using: " + ChatColor.RED + PlayerTexturePacks.get(p.getName()) + ChatColor.GREEN + " as your texture pack");

			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

				   public void run() {
					   changeTexturePack(p, TexturePacksbyName.get(PlayerTexturePacks.get(p.getName())));
				   }
				}, 50L);
			
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {

		if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
				
		BlockState block = e.getClickedBlock().getState();
		
		if (block instanceof Sign) {
			Sign sign = (Sign) block;
			
			String SignType = sign.getLine(0);
			
			if (SignType.equalsIgnoreCase("[TexturePack]")) {
				
				String texturePackName = sign.getLine(1);
				if (TexturePacksbyName.containsKey(texturePackName)) {
					PlayerTexturePacks.put(e.getPlayer().getName(), texturePackName);
					e.getPlayer().sendMessage("You are now using " + texturePackName + " for your texture pack. Please make sure you have 'Server textures' on under video settings. If your texture pack hasn't changed, try logging in again.");
				}
				else {
					e.getPlayer().sendMessage("Sorry, I couldn't find that texture pack. Please notify your server administrator.");
					return;
				}				
				
				this.UpdateTextureInConfig(e.getPlayer().getName());
				
				changeTexturePack(e.getPlayer(), TexturePacksbyName.get(PlayerTexturePacks.get(e.getPlayer().getName())));				
			}
		}
	}
	
	public void changeTexturePack(Player p, String url) {
		if (url == null) p.sendMessage("I'm sorry, I couldn't change your texture pack because the texture pack you chose hadn't been configured properly. There is no url for it set.");
		
		EntityPlayer player = (EntityPlayer) (((CraftPlayer)p).getHandle());
		player.playerConnection.sendPacket(getTexturePackChangePacket(url));
		
	}

	private Packet getTexturePackChangePacket(String url) {
		Packet250CustomPayload packet = new Packet250CustomPayload();

		int size = 16;
		String message = url + "\u0000" + size; //build the data

		byte[] data	= message.getBytes();

		packet.data = data;
		packet.length = data.length;
		packet.lowPriority = false;
		packet.tag = "MC|TPack"; //set the channel

		return packet;
	}
}
