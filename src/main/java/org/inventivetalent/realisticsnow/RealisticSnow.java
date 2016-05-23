package org.inventivetalent.realisticsnow;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.inventivetalent.pluginannotations.config.ConfigValue;
import org.mcstats.MetricsLite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RealisticSnow extends JavaPlugin implements Listener {

	@ConfigValue(path = "interval")
	public static long        interval               = 80;
	@ConfigValue(path = "radius")
	public static int         blockRadius            = 8;
	public static int         replacementProbability = 50;
	public static List<Biome> snowBiomes             = new ArrayList<>();

	public static Random random;

	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);

		random = new Random();

		saveDefaultConfig();
		replacementProbability = getConfig().getInt("probability", replacementProbability);
		replacementProbability = 100 - Math.min(100, replacementProbability);

		snowBiomes.clear();
		for (String s : getConfig().getStringList("biomes")) {
			try {
				snowBiomes.add(Biome.valueOf(s));
			} catch (Exception e) {
				getLogger().warning("Invalid biome name: " + s);
			}
		}

		try {
			MetricsLite metrics = new MetricsLite(this);
			if (metrics.start()) {
				getLogger().info("Metrics started");
			}
		} catch (Exception e) {
		}

		new BukkitRunnable() {

			@Override
			public void run() {
				Iterator<? extends Player> iterator = Bukkit.getOnlinePlayers().iterator();
				while (iterator.hasNext()) {
					final Player next = iterator.next();
					final int bX = next.getLocation().getBlockX();
					final int bY = next.getLocation().getBlockY();
					final int bZ = next.getLocation().getBlockZ();
					if (!doesBiomeHaveSnow(next.getWorld().getBiome(bX, bZ))) { continue; }
					if (!next.getWorld().hasStorm()) { continue; }
					Bukkit.getScheduler().runTaskAsynchronously(RealisticSnow.this, new Runnable() {

						@Override
						public void run() {
							for (int x = -blockRadius; x < blockRadius; x++) {
								for (int z = -blockRadius; z < blockRadius; z++) {
									for (int y = -blockRadius; y < blockRadius; y++) {
										final int xx = x;
										final int zz = z;
										final int yy = y;

										Bukkit.getScheduler().runTask(RealisticSnow.this, new Runnable() {

											@Override
											public void run() {
												Block b = next.getWorld().getBlockAt(bX + xx, bY + yy, bZ + zz);
												if (!doesBiomeHaveSnow(b.getBiome())) { return; }
												if (b.getType() == Material.SNOW) {
													performReplacement(b);
												}
											}
										});
									}
								}
							}
						}
					});
				}
			}
		}.runTaskTimer(this, interval, interval);
	}

	public static void performReplacement(Block block) {
		if (random.nextInt(replacementProbability) != 0) {
			return;
		}
		byte prevData = block.getData();
		byte newData = (byte) (prevData + 1);
		if (newData < 7) {
			block.setData(newData);
		} else {
			newData = 0;
			Block above = block.getRelative(BlockFace.UP);
			if (above.getType() == Material.AIR) {
				block.setType(Material.SNOW_BLOCK);
				above.setType(Material.SNOW);
				above.setData(newData);
			}
		}
	}

	public static boolean doesBiomeHaveSnow(Biome biome) {
		return snowBiomes.contains(biome);
	}

}
