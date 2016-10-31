package vg.civcraft.mc.civmodcore.itemHandling;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Based on a specific ItemStack and various wild cards, this class allows
 * defining a set of possible ItemStack variations. All checks done by this
 * class will completly ignore the amount of any ItemStacks involved, the only
 * things that matter are material, durability and ItemMeta. Note that material
 * and durability are NOT part of the ItemMeta.
 * 
 * Direct usage of this class is possible, but not recommended, use ItemMaps
 * instead if possible.
 * 
 * This class is immutable
 *
 */
public class ItemWrapper implements ConfigurationSerializable, Cloneable {

	private final ItemStack item;

	private final boolean wildcardDurability;

	private final boolean wildcardLore;

	private final boolean wildcardEnchants;

	private final boolean wildcardName;

	private final boolean wildcardItemMeta;

	private final boolean wildcardNonExplicitItemMeta;

	/**
	 * Creates an ItemWrapper while setting all wild cards to false, meaning
	 * only this exact item will be accepted
	 * 
	 * @param is
	 *            ItemStack to base instance on
	 */
	public ItemWrapper(ItemStack is) {
		this(is, false, false, false, false, false, false);
	}

	public ItemWrapper(ItemStack is, boolean wildcardDurability) {
		this(is, wildcardDurability, false, false, false, false, false);
	}

	public ItemWrapper(ItemStack is, boolean wildcardDurability, boolean wildcardEnchants, boolean wildcardLore,
			boolean wildcardName, boolean wildcardItemMeta, boolean wildcardNonExplicitItemMeta) {
		this.item = ItemMap.createMapConformCopy(is);
		this.wildcardDurability = wildcardDurability;
		this.wildcardEnchants = wildcardEnchants;
		this.wildcardLore = wildcardLore;
		this.wildcardName = wildcardName;
		this.wildcardItemMeta = wildcardItemMeta;
		this.wildcardNonExplicitItemMeta = wildcardNonExplicitItemMeta;
	}

	public ItemWrapper(Map<String, Object> serializedMap) {
		item = (ItemStack) serializedMap.get("item");
		wildcardDurability = (boolean) serializedMap.get("wildcardDurability");
		wildcardLore = (boolean) serializedMap.get("wildcardLore");
		wildcardName = (boolean) serializedMap.get("wildcardName");
		wildcardEnchants = (boolean) serializedMap.get("wildcardEnchants");
		wildcardItemMeta = (boolean) serializedMap.get("wildcardItemMeta");
		wildcardNonExplicitItemMeta = (boolean) serializedMap.get("wildcardNonExplicitItemMeta");
	}

	/**
	 * Checks whether the given ItemStack is within the set of ItemStack defined
	 * by this instance, meaning whether it fulfills the criterias specified
	 * through wildcards etc.
	 * 
	 * @param comp
	 *            ItemStack to check
	 * @return True if the given ItemStack fullfills all criterias set by this
	 *         instance, false if not
	 */
	public boolean fulfillsCriterias(ItemStack comp) {
		if (comp == null) {
			return false;
		}
		if (comp.getType() != item.getType()) {
			return false;
		}
		if (!(isDurabilityWildcarded() || comp.getDurability() == item.getDurability())) {
			return false;
		}
		if (wildcardItemMeta) {
			return true;
		}
		ItemMeta originalMeta = item.getItemMeta();
		ItemMeta compMeta = comp.getItemMeta();
		if (!wildcardLore) {
			if (originalMeta.hasLore()) {
				if (!compMeta.hasLore()) {
					return false;
				} else {
					if (!compMeta.getLore().equals(originalMeta.getLore())) {
						return false;
					}
				}
			} else {
				if (compMeta.hasLore()) {
					return false;
				}
			}
		}
		if (!wildcardEnchants) {
			if (originalMeta.hasEnchants()) {
				if (!compMeta.hasEnchants()) {
					return false;
				} else {
					if (!compMeta.getEnchants().equals(originalMeta.getEnchants())) {
						return false;
					}
				}
			} else {
				if (compMeta.hasEnchants()) {
					return false;
				}
			}
		}
		if (!wildcardName) {
			if (originalMeta.hasDisplayName()) {
				if (!compMeta.hasDisplayName()) {
					return false;
				} else {
					if (!compMeta.getDisplayName().equals(originalMeta.getDisplayName())) {
						return false;
					}
				}
			} else {
				if (compMeta.hasDisplayName()) {
					return false;
				}
			}
		}

		if (compMeta.spigot().isUnbreakable() != originalMeta.spigot().isUnbreakable()) {
			return false;
		}

		// we already did all explicit checks here, so if non explicit wild card
		// is true,
		// we just assume its valid
		if (wildcardNonExplicitItemMeta) {
			return true;
		}

		// equalize metas in point that were already checked and make them
		// identical so wildcards for them actually work
		compMeta.setLore(originalMeta.getLore());
		compMeta.setDisplayName(originalMeta.getDisplayName());
		for (Enchantment ench : originalMeta.getEnchants().keySet()) {
			originalMeta.removeEnchant(ench);
		}
		for (Enchantment ench : compMeta.getEnchants().keySet()) {
			compMeta.removeEnchant(ench);
		}
		return compMeta.equals(originalMeta);
	}

	/**
	 * Checks whether the given ItemWrapper is more specific than this instance,
	 * meaning the set containing all ItemStacks, which fulfill the criterias of
	 * the given wrapper is a true subset of the set of ItemStacks, which
	 * fulfill the criterias of this instance. For two given ItemWrappers a and
	 * b, a.isMoreSpecific(b) and b.isMoreSpecific(a) might both return false,
	 * if neither of them is explicitly a subset of the other. Only one of them
	 * may return true though
	 * 
	 * @return True if the given ItemWrapper defines a true subset of this
	 *         instance
	 */
	public boolean isMoreSpecific(ItemWrapper comp) {
		if (!fulfillsCriterias(comp.getItem())) {
			return false;
		}
		boolean foundSuperior = false;
		// check durability
		if (comp.isDurabilityWildcarded()) {
			if (!isDurabilityWildcarded()) {
				return false;
			}
		} else {
			if (isDurabilityWildcarded()) {
				foundSuperior = true;
			}
		}
		// check general item meta
		if (comp.isItemMetaWildcarded()) {
			if (!isItemMetaWildcarded()) {
				return false;
			}
		} else {
			if (isItemMetaWildcarded()) {
				foundSuperior = true;
			}
		}
		// check lore
		if (comp.isLoreWildcarded()) {
			if (!isLoreWildcarded()) {
				return false;
			}
		} else {
			if (isLoreWildcarded()) {
				foundSuperior = true;
			}
		}
		// check enchants
		if (comp.areEnchantsWildcarded()) {
			if (!areEnchantsWildcarded()) {
				return false;
			}
		} else {
			if (areEnchantsWildcarded()) {
				foundSuperior = true;
			}
		}
		// check name
		if (comp.isNameWildcarded()) {
			if (!isNameWildcarded()) {
				return false;
			}
		} else {
			if (isNameWildcarded()) {
				foundSuperior = true;
			}
		}
		// check non specific itemmeta
		if (comp.isNonSpecficItemMetaWildcarded()) {
			if (!isNonSpecficItemMetaWildcarded()) {
				return false;
			}
		} else {
			if (isNonSpecficItemMetaWildcarded()) {
				foundSuperior = true;
			}
		}
		return foundSuperior;
	}

	/**
	 * Gets a representation of this instance, which can be used in GUIs. If
	 * wildcards are specified, descriptions of those will be appended to the
	 * returned items lore
	 * 
	 * @return GUI representation of this instance
	 */
	public ItemStack getGUIRepresentation() {
		ItemStack is = getItem();
		if (hasWildcard()) {
			ISUtils.addLore(is, ChatColor.GOLD + "------------------");
			if (isDurabilityWildcarded()) {
				ISUtils.addLore(is, ChatColor.GOLD + "May have any durability");
			}
			if (isItemMetaWildcarded()) {
				ISUtils.addLore(is, ChatColor.GOLD + "May have any item meta data");
			} else {
				if (areEnchantsWildcarded()) {
					ISUtils.addLore(is, ChatColor.GOLD + "May have any enchants");
				}
				if (isLoreWildcarded()) {
					ISUtils.addLore(is, ChatColor.GOLD + "May have any lore");
				}
				if (isNameWildcarded()) {
					ISUtils.addLore(is, ChatColor.GOLD + "May have any custom name");
				}
				if (isNonSpecficItemMetaWildcarded()) {
					ISUtils.addLore(is, ChatColor.GOLD + "May have any other NBT tags");
				}
			}
		}
		return is;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ItemWrapper)) {
			return false;
		}
		ItemWrapper comp = (ItemWrapper) o;
		return comp.isDurabilityWildcarded() == isDurabilityWildcarded()
				&& comp.isItemMetaWildcarded() == isItemMetaWildcarded()
				&& comp.isLoreWildcarded() == isLoreWildcarded() && comp.isNameWildcarded() == isNameWildcarded()
				&& comp.areEnchantsWildcarded() == areEnchantsWildcarded()
				&& comp.isNonSpecficItemMetaWildcarded() == isNonSpecficItemMetaWildcarded()
				&& comp.fulfillsCriterias(item) && fulfillsCriterias(comp.getItem());
	}

	/**
	 * Gets the durability defined for this ItemWrapper. If durability is
	 * wildcarded, -1 will always be returned, otherwise if a set durability is
	 * defined, that will be returned
	 * 
	 * @return Lore defined for this instance
	 */
	public short getDurability() {
		if (wildcardDurability) {
			return -1;
		}
		return item.getDurability();
	}

	/**
	 * @return Whether durability is wildcarded for this instance, meaning it is
	 *         ignored during checks
	 */
	public boolean isDurabilityWildcarded() {
		return wildcardDurability;
	}

	/**
	 * Gets the enchants of this ItemWrapper. If enchants are wildcarded, null
	 * will always be returned, otherwise if an set of enchants is set, those
	 * will be returned and if none are set an empty map will be returned
	 * 
	 * @return Lore defined for this instance
	 */
	public Map<Enchantment, Integer> getEnchants() {
		if (areEnchantsWildcarded()) {
			return null;
		}
		ItemMeta im = item.getItemMeta();
		if (im.hasEnchants()) {
			return im.getEnchants();
		}
		return new HashMap<Enchantment, Integer>();
	}

	/**
	 * @return Whether enchants are wildcarded for this instance, meaning any
	 *         enchants are ignored during checks
	 */
	public boolean areEnchantsWildcarded() {
		return wildcardEnchants || wildcardItemMeta;
	}

	/**
	 * @return A copy of the base item around which this instance is based,
	 *         ignoring any wildcards
	 */
	public ItemStack getItem() {
		return item.clone();
	}

	/**
	 * Gets the lore of this ItemWrapper. If the lore is wildcarded, null will
	 * always be returned, otherwise if an explicit lore is set, it will be
	 * returned and if none is set an empty list will be returned
	 * 
	 * @return Lore defined for this instance
	 */
	public List<String> getLore() {
		if (isLoreWildcarded()) {
			return null;
		}
		ItemMeta im = item.getItemMeta();
		if (im.hasLore()) {
			return im.getLore();
		}
		return new LinkedList<String>();
	}

	/**
	 * @return Whether lore is wildcarded for this instance, meaning any lore
	 *         differences are ignored during checks
	 */
	public boolean isLoreWildcarded() {
		return wildcardLore || wildcardItemMeta;
	}

	public boolean isNameWildcarded() {
		return wildcardName || wildcardItemMeta;
	}

	/**
	 * Gets the display name of this ItemWrapper. If the display name is
	 * wildcarded, null will always be returned, otherwise if an explicit
	 * display name is set, it will be returned and if none is set an empty
	 * string will be returned
	 * 
	 * @return Display name of this instance
	 */
	public String getName() {
		if (isNameWildcarded()) {
			return null;
		}
		ItemMeta im = item.getItemMeta();
		if (im.hasDisplayName()) {
			return im.getDisplayName();
		}
		return "";
	}

	/**
	 * @return Whether all ItemMeta should be wildcarded, which means that
	 *         material and durability are the only values checked
	 */
	public boolean isItemMetaWildcarded() {
		return wildcardItemMeta;
	}

	/**
	 * @return Whether all other item meta, which doesnt have an explicit toggle
	 *         here is ignored, meaning basically any item meta, except for
	 *         display name, enchants and lore
	 */
	public boolean isNonSpecficItemMetaWildcarded() {
		return wildcardNonExplicitItemMeta;
	}

	/**
	 * @return Whether any wild cards are set for this instance
	 */
	public boolean hasWildcard() {
		return wildcardDurability || wildcardItemMeta || wildcardEnchants || wildcardLore || wildcardName
				|| wildcardNonExplicitItemMeta;
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> serial = new HashMap<String, Object>();
		serial.put("item", item.serialize());
		serial.put("wildcardDurability", wildcardDurability);
		serial.put("wildcardLore", wildcardLore);
		serial.put("wildcardName", wildcardName);
		serial.put("wildcardEnchants", wildcardEnchants);
		serial.put("wildcardItemMeta", wildcardItemMeta);
		serial.put("wildcardNonExplicitItemMeta", wildcardNonExplicitItemMeta);
		return serial;
	}

	public ItemWrapper clone() {
		return new ItemWrapper(getItem(), wildcardDurability, wildcardEnchants, wildcardLore, wildcardName,
				wildcardItemMeta, wildcardNonExplicitItemMeta);
	}
}
