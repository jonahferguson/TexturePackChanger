package me.jtjj222.TexturePackChanger;

import java.util.HashMap;
import java.util.Set;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.Packet250CustomPayload;

import org.bukkit.ChatColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;


public class TexturePackChanger extends JavaPlugin implements Listener{
	
	HashMap<String,String> PlayerTexturePacks = new HashMap<String,String>(); //<player name, texture pack url
	HashMap<String,String> TexturePacksbyName = new HashMap<String,String>(); //<tpack name, tpack url>
	
	public void onEnable() {
		
		this.saveDefaultConfig(); //create a config if it doesnt allready exist
		
		boolean deep = false;
		
		//load the texture pack url's
		ConfigurationSection TexturesConfig = getConfig().getConfigurationSection("Textures.");

		Set<String> tPackNames = TexturesConfig.getKeys(deep);
		
		for (String key: tPackNames) {			
			TexturePacksbyName.put(key,getConfig().getString("Textures."+key+".url"));			
		}
		
		//then load the players choice of texture
		ConfigurationSection playerConfig = getConfig().getConfigurationSection("Players.");

		Set<String> players = playerConfig.getKeys(deep);
		
		for (String key: players) {			
			PlayerTexturePacks.put(key,getConfig().getString("Players."+key+".texture"));			
		}
		
		getServer().getPluginManager().registerEvents(this, this);
	}
	public void onDisable() {
		
		for (String key : PlayerTexturePacks.keySet()) {
			getConfig().set("Players."+key+".texture", PlayerTexturePacks.get(key));
		}
		saveConfig();
	}
		
	@EventHandler
	public void onPlayerJoinWorld(PlayerJoinEvent e) {				
		final Player p = e.getPlayer();
		
		if (PlayerTexturePacks.containsKey(p.getName())) {
			p.sendMessage(ChatColor.GREEN + "You are currently using: " + ChatColor.RED + PlayerTexturePacks.get(p.getName()) + ChatColor.GREEN + " as your texture pack");

			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

				   public void run() {
						EntityPlayer victim = ((CraftPlayer) p).getHandle();
						String texturePackName = PlayerTexturePacks.get(p.getName());
					   victim.netServerHandler.sendPacket(getTexturePackChangePacket(TexturePacksbyName.get(texturePackName)));
				   }
				}, 50L);
			
		} else {
			p.sendMessage("Please select your texture pack");
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
				PlayerTexturePacks.put(e.getPlayer().getName(), texturePackName);
				
				//change their texture pack
				
				EntityPlayer victim = ((CraftPlayer) e.getPlayer()).getHandle();
				victim.netServerHandler.sendPacket(getTexturePackChangePacket(TexturePacksbyName.get(texturePackName)));
				
				this.onDisable(); //save the config
			}
		}
	}
	
	public Packet250CustomPayload getTexturePackChangePacket(String url) {
		
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
