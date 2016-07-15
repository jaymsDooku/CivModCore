package vg.civcraft.mc.civmodcore.areas;

import java.util.Collection;
import java.util.HashSet;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

public class EllipseArea extends AbstractYLimitedArea {

	private Location center;
	private double xSize;
	private double zSize;

	public EllipseArea(double lowerYBound, double upperYBound, Collection <Biome> allowedBiomes, Location center,
			double xSize, double zSize) {
		super(lowerYBound, upperYBound, allowedBiomes);
		this.center = center;
		this.xSize = xSize;
		this.zSize = zSize;
	}

	@Override
	public Collection<Chunk> getChunks() {
		Collection<Chunk> chunks = new HashSet<Chunk>();
		for (double x = center.getX() - xSize; x <= center.getX() + xSize; x += 16) {
			for (double z = center.getZ() - zSize; z <= center.getZ() + zSize; z += 16) {
				Chunk c = new Location(center.getWorld(), x, center.getY(), z)
						.getChunk();
				// if one of the corners is in the area the chunk is inside
				if (isInArea(c.getBlock(0, (int) getLowerYBound(), 15)
						.getLocation())
						|| isInArea(c.getBlock(0, (int) getLowerYBound(), 0)
								.getLocation())
						|| isInArea(c.getBlock(15, (int) getLowerYBound(), 0)
								.getLocation())
						|| isInArea(c.getBlock(15, (int) getLowerYBound(), 15)
								.getLocation())) {
					chunks.add(c);
				}
			}
		}
		return chunks;
	}

	@Override
	public Location getCenter() {
		return center;
	}

	@Override
	public World getWorld() {
		return center.getWorld();
	}

	@Override
	public boolean isInArea(Location loc) {
		double xDist = center.getX() - loc.getX();
		double zDist = center.getZ() - loc.getZ();
		return super.isInArea(loc)
				&& ((xDist * xDist) / (xSize * xSize))
						+ ((zDist * zDist) / (zSize * zSize)) <= 1;
	}

	/**
	 * @return Half of the diameter of this ellipse in x dimension
	 */
	public double getXSize() {
		return xSize;
	}

	/**
	 * @return Half of the diameter of this ellipse in z dimension
	 */
	public double getZSize() {
		return zSize;
	}
}