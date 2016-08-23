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

public class ItemWrapper implements ConfigurationSerializable {

	private final ItemStack item;

	private final boolean wildcardDurability;

	private final boolean wildcardLore;

	private final boolean wildcardEnchants;

	private final boolean wildcardName;

	private final boolean wildcardItemMeta;

	public ItemWrapper(ItemStack is) {
		this(is, false, false, false, false, false);
	}

	public ItemWrapper(ItemStack is, boolean wildcardDurability) {
		this(is, wildcardDurability, false, false, false, false);
	}

	public ItemWrapper(ItemStack is, boolean wildcardDurability, boolean wildcardEnchants, boolean wildcardLore,
			boolean wildcardName, boolean wildcardItemMeta) {
		this.item = is;
		this.wildcardDurability = wildcardDurability;
		this.wildcardEnchants = wildcardEnchants;
		this.wildcardLore = wildcardLore;
		this.wildcardName = wildcardName;
		this.wildcardItemMeta = wildcardItemMeta;
	}

	public ItemWrapper(Map<String, Object> serializedMap) {
		item = (ItemStack) serializedMap.get("item");
		wildcardDurability = (boolean) serializedMap.get("wildcardDurability");
		wildcardLore = (boolean) serializedMap.get("wildcardLore");
		wildcardName = (boolean) serializedMap.get("wildcardName");
		wildcardEnchants = (boolean) serializedMap.get("wildcardEnchants");
		wildcardItemMeta = (boolean) serializedMap.get("wildcardItemMeta");
	}

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
		return foundSuperior;
	}

	public ItemStack getGUIRepresentation() {
		ItemStack is = getItem();
		if (hasWildcard()) {
			ISUtils.addLore(is, ChatColor.GOLD + "------------------");
			if (isDurabilityWildcarded()) {
				ISUtils.addLore(is, ChatColor.GOLD + "May have any durability");
			}
			if (isItemMetaWildcarded()) {
				ISUtils.addLore(is, ChatColor.GOLD + "May have any item meta data");
			}
			else {
				if (areEnchantsWildcarded()) {
					ISUtils.addLore(is, ChatColor.GOLD + "May have any enchants");
				}
				if (isLoreWildcarded()) {
					ISUtils.addLore(is, ChatColor.GOLD + "May have any lore");
				}
				if (isNameWildcarded()) {
					ISUtils.addLore(is, ChatColor.GOLD + "May have any custom name");
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
				&& comp.areEnchantsWildcarded() == areEnchantsWildcarded() && comp.fulfillsCriterias(item)
				&& fulfillsCriterias(comp.getItem());
	}

	public short getDurability() {
		if (wildcardDurability) {
			return -1;
		}
		return item.getDurability();
	}

	public boolean isDurabilityWildcarded() {
		return wildcardDurability;
	}

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

	public boolean areEnchantsWildcarded() {
		return wildcardEnchants || wildcardItemMeta;
	}

	public ItemStack getItem() {
		return item.clone();
	}

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

	public boolean isLoreWildcarded() {
		return wildcardLore || wildcardItemMeta;
	}

	public boolean isNameWildcarded() {
		return wildcardName || wildcardItemMeta;
	}

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

	public boolean isItemMetaWildcarded() {
		return wildcardItemMeta;
	}

	public boolean hasWildcard() {
		return wildcardDurability || wildcardItemMeta || wildcardEnchants || wildcardLore || wildcardName;
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
		return serial;
	}

}
