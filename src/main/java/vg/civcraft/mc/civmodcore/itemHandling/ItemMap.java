package vg.civcraft.mc.civmodcore.itemHandling;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.minecraft.server.v1_10_R1.NBTTagCompound;
import net.minecraft.server.v1_10_R1.NBTTagList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.craftbukkit.v1_10_R1.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Allows the storage and comparison of itemstacks while ignoring their maximum
 * possible stack sizes. This offers various tools to compare inventories, to
 * store recipe costs or to specify setupcosts. Take great care when dealing
 * with itemstacks with negative amounnts, while this implementation should be
 * consistent even with negative values, they create possibly unexpected
 * results. For example an empty inventory/itemmap will seem to contain items
 * when compared to a map containing negative values. This class is based around
 * ItemWrappers and the wildcards specified by those are completly respected.
 * You can use this class without touching ItemWrapper as at all, but at the
 * same time setting up complex wild card system is possible.
 */
public class ItemMap implements ConfigurationSerializable, Cloneable {

	private HashMap<ItemWrapper, Integer> items;
	private static final Logger log = Bukkit.getLogger();

	private int totalItems;

	/**
	 * Empty constructor to create empty item map
	 */
	public ItemMap() {
		items = new HashMap<>();
		totalItems = 0;
	}

	/**
	 * Constructor to create an item map based on the content of an inventory.
	 * The ItemMap will not be in sync with the inventory, it will only update
	 * if it's explicitly told to do so
	 *
	 * @param inv
	 *            Inventory to base the item map on
	 */
	public ItemMap(Inventory inv) {
		totalItems = 0;
		update(inv);
	}

	/**
	 * Constructor to create an ItemMap based on a single ItemStack
	 *
	 * @param is
	 *            ItemStack to start with
	 */
	public ItemMap(ItemStack... is) {
		items = new HashMap<>();
		totalItems = 0;
		for (ItemStack i : is) {
			addItemStack(i);
		}
	}

	/**
	 * Constructor to create an item map based on a collection of ItemStacks
	 *
	 * @param stacks
	 *            Stacks to add to the map
	 */
	public ItemMap(Collection<ItemStack> stacks) {
		items = new HashMap<>();
		totalItems = 0;
		addAll(stacks);
	}

	@SuppressWarnings("unchecked")
	public ItemMap(Map<String, Object> serializedMap) {
		this.items = (HashMap<ItemWrapper, Integer>) serializedMap.get("items");
		int count = 0;
		for (Integer i : items.values()) {
			count += i;
		}
		this.totalItems = count;
	}

	public void addItemWrapper(ItemWrapper wrapper, int amount) {
		Integer i;
		if ((i = items.get(wrapper)) != null) {
			items.put(wrapper, i + amount);
		} else {
			items.put(wrapper, amount);
		}
		totalItems += amount;
	}

	/**
	 * Removes the given amount of the given ItemWrapper from this instance.
	 * 
	 * @param wrapper
	 *            Wrapper to remove
	 * @param amount
	 *            Amount to remove
	 * @return Amount of items that could not be removed
	 */
	public int removeItemWrapper(ItemWrapper wrapper, int amount) {
		Integer value = items.get(wrapper);
		if (value != null) {
			int newVal = value - amount;
			totalItems -= amount;
			if (newVal > 0) {
				items.put(wrapper, newVal);
				return 0;
			} else {
				int itemAmountNotRemoved = Math.abs(newVal);
				totalItems += itemAmountNotRemoved;
				items.remove(wrapper);
				return itemAmountNotRemoved;
			}
		} else {
			return amount;
		}
	}

	/**
	 * Clones the given itemstack, sets its amount to one and checks whether a
	 * stack equaling the created one exists in the item map. If yes the amount
	 * of the given stack (before the amount was set to 1) will be added to the
	 * current amount in the item map, if not a new entry in the map with the
	 * correct amount will be created
	 *
	 * @param input
	 *            ItemStack to insert
	 */
	public void addItemStack(ItemStack input) {
		if (input != null) {
			ItemWrapper wrapper = createMapConformItemWrapper(input);
			if (wrapper == null) {
				return;
			}
			addItemWrapper(wrapper, input.getAmount());
		}
	}

	/**
	 * Removes the given ItemStack from this map. Only the amount of the given
	 * ItemStack will be removed, not all of them. If the amount of the given
	 * itemstack is bigger than the existing ones in this map, not more than the
	 * amount in this map will be removed
	 *
	 * @param input
	 *            ItemStack to remove
	 */
	public int removeItemStack(ItemStack input) {
		ItemWrapper wrapper = createMapConformItemWrapper(input);
		if (wrapper == null) {
			return input.getAmount();
		}
		return removeItemWrapper(wrapper, input.getAmount());
	}

	/**
	 * Completly removes the given itemstack of this item map, completly
	 * independent of its amount
	 *
	 * @param input
	 *            ItemStack to remove
	 */
	public boolean removeItemStackCompletly(ItemStack input) {
		ItemWrapper wrapper = createMapConformItemWrapper(input);
		if (wrapper == null) {
			return false;
		}
		Integer amount = items.get(wrapper);
		if (amount == null) {
			return false;
		}
		removeItemWrapper(wrapper, amount);
		return true;
	}

	public int hashCode() {
		int res = 0;
		for (Entry<ItemWrapper, Integer> entry : items.entrySet()) {
			res += entry.hashCode();
		}
		return res;
	}

	/**
	 * Adds all the stacks given in the collection to this map
	 *
	 * @param stacks
	 *            Stacks to add
	 */
	public void addAll(Collection<ItemStack> stacks) {
		for (ItemStack is : stacks) {
			if (is != null) {
				addItemStack(is);
			}
		}
	}

	/**
	 * Merges the given item map into this instance
	 *
	 * @param im
	 *            ItemMap to merge
	 */
	public void merge(ItemMap im) {
		for (Entry<ItemWrapper, Integer> entry : im.getEntrySet()) {
			addItemWrapper(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Updates this ItemMap to make its content identical with the given
	 * inventory. Any preexisting items will be removed. After this method is
	 * called, further changes to the inventory will NOT update this instance
	 * 
	 * @param inv
	 *            Inventory to base instance on
	 */
	public void update(Inventory inv) {
		items = new HashMap<>();
		totalItems = 0;
		for (int i = 0; i < inv.getSize(); i++) {
			ItemStack is = inv.getItem(i);
			if (is != null) {
				addItemStack(is);
			}
		}
	}

	/**
	 * Adds all items from a given entry set to this instance
	 * 
	 * @param entries
	 *            Entries to add
	 */
	public void addEntrySet(Set<Entry<ItemStack, Integer>> entries) {
		for (Entry<ItemStack, Integer> entry : entries) {
			addItemAmount(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Utility method, which has the amount of items to add as parameter.
	 *
	 * @param input
	 *            ItemStack to sort into the map
	 * @param amount
	 *            Amount associated with the given ItemStack
	 */
	public void addItemAmount(ItemStack input, int amount) {
		ItemStack copy = createMapConformCopy(input);
		if (copy == null) {
			return;
		}
		copy.setAmount(amount);
		addItemStack(copy);
	}

	/**
	 * Gets how many many wrappers, which would accept the given ItemStacks, are
	 * in this instance
	 *
	 * @param is
	 *            Exact ItemStack to search for
	 * @return amount of items like the given stack in this map
	 */
	public int getAmount(ItemStack is) {
		int amount = 0;
		for (Entry<ItemWrapper, Integer> entry : getEntrySet()) {
			ItemWrapper current = entry.getKey();
			if (current.fulfillsCriterias(is)) {
				amount += entry.getValue();
			}
		}
		return amount;
	}

	/**
	 * Checks how many of the given ItemWrapper is contained in this instance.
	 * Note that this will only check for this specific ItemWrapper and not do
	 * wildcard based similarity checks like for ItemStacks. If the given
	 * ItemWrapper is not explicitly tracked 0 will be returned
	 * 
	 * @param iw
	 *            ItemWrapper to look up
	 * @return Amount of the given ItemWrapper in this instance
	 */
	public int getAmount(ItemWrapper iw) {
		Integer i = items.get(iw);
		if (i == null) {
			return 0;
		}
		return i;
	}

	/**
	 * @return How many items are stored in this map total
	 */
	public int getTotalItemAmount() {
		return totalItems;
	}

	/**
	 * @return How many unique itemwrappers are stored in this map
	 */
	public int getTotalUniqueItemAmount() {
		return items.keySet().size();
	}

	/**
	 * @return An entry set of a copy of the underlying map
	 */
	@SuppressWarnings("unchecked")
	public Set<Entry<ItemWrapper, Integer>> getEntrySet() {
		return ((Map<ItemWrapper, Integer>) items.clone()).entrySet();
	}

	/**
	 * Gets the most specific ItemWrapper contained in this instance, for which
	 * the given ItemStack fulfills it's criterias. Most specific means the set
	 * of all ItemStacks, which fulfill the criterias of that wrapper is the
	 * smallest out of all those for which the given ItemStack fullfills all
	 * criterias
	 * 
	 * @param is
	 *            ItemStack to search wrapper for
	 * @return Most specific wrapper found or null if no wrapper accepts the
	 *         given ItemStack
	 */
	public ItemWrapper getMostSpecificWrapper(ItemStack is) {
		ItemWrapper mostSpecific = null;
		for (Entry<ItemWrapper, Integer> entry : getEntrySet()) {
			if (entry.getKey().fulfillsCriterias(is)) {
				if (mostSpecific == null) {
					mostSpecific = entry.getKey();
				} else {
					if (mostSpecific.isMoreSpecific(entry.getKey())) {
						mostSpecific = entry.getKey();
					}
				}
			}
		}
		return mostSpecific;
	}

	/**
	 * @return Whether this instance is empty
	 */
	public boolean isEmpty() {
		return items.isEmpty();
	}

	/**
	 * Gets all ItemWrapper contained in this instance for which the given given
	 * ItemStack fulfills it's criterias
	 * 
	 * @param is
	 *            ItemStack to search wrappers for
	 * @return All ItemWrapper accepting the given ItemStack
	 */
	public List<ItemWrapper> getAcceptingItemWrapper(ItemStack is) {
		List<ItemWrapper> wrappers = new LinkedList<ItemWrapper>();
		for (Entry<ItemWrapper, Integer> entry : getEntrySet()) {
			if (entry.getKey().fulfillsCriterias(is)) {
				wrappers.add(entry.getKey());
			}
		}
		return wrappers;
	}

	private Map<Integer, Map<ItemStack, List<ItemWrapper>>> calculateIntersections(ItemMap comp) {
		Map<Integer, Map<ItemStack, List<ItemWrapper>>> mapping = new TreeMap<Integer, Map<ItemStack, List<ItemWrapper>>>();
		for (Entry<ItemWrapper, Integer> entry : comp.getEntrySet()) {
			ItemStack is = entry.getKey().getItem();
			List<ItemWrapper> wrappers = getAcceptingItemWrapper(is);
			if (wrappers.size() == 0) {
				// fail fast
				return null;
			}
			int availableWrappers = wrappers.size();
			Map<ItemStack, List<ItemWrapper>> amountMapping = mapping.get(availableWrappers);
			if (amountMapping == null) {
				amountMapping = new HashMap<ItemStack, List<ItemWrapper>>();
				mapping.put(availableWrappers, amountMapping);
			}
			amountMapping.put(is, wrappers);
		}
		return mapping;
	}

	/**
	 * Checks whether an inventory contains exactly what's described in this
	 * ItemMap
	 *
	 * @param i
	 *            Inventory to compare
	 * @return True if the inventory is identical with this instance, false if
	 *         not
	 */
	public boolean containedExactlyIn(Inventory i) {
		ItemMap invMap = new ItemMap(i);
		ItemMap clone = this.clone();
		if (invMap.getTotalItemAmount() != clone.getTotalItemAmount()) {
			// this check should catch most cases already
		}
		if (invMap.getTotalItemAmount() == 0) {
			// both are empty?
			return true;
		}
		Map<Integer, Map<ItemStack, List<ItemWrapper>>> diff = calculateIntersections(invMap);
		for (int k = 1; !diff.isEmpty(); k++) {
			Map<ItemStack, List<ItemWrapper>> mapping = diff.get(k);
			if (mapping == null) {
				continue;
			}
			diff.remove(k);
			for (Entry<ItemStack, List<ItemWrapper>> entry : mapping.entrySet()) {
				int amount = invMap.getAmount(entry.getKey());
				for (ItemWrapper iw : entry.getValue()) {
					int wrapperCount = clone.getAmount(iw);
					if (wrapperCount <= 0) {
						continue;
					}
					int amountToRemove = Math.min(wrapperCount, amount);
					clone.removeItemWrapper(iw, amountToRemove);
					invMap.removeItemWrapper(new ItemWrapper(entry.getKey()), amountToRemove);
					amount -= amountToRemove;
				}
				if (amount != 0) {
					// not all could be assigned
					return false;
				}
			}
		}
		// if the inventory contained exactly what was requested, both maps
		// should be completly emptied
		return clone.isEmpty() && invMap.isEmpty();
	}

	/**
	 * Checks whether this instance is completly contained in the given
	 * inventory, which means every stack in this instance is also in the given
	 * inventory and the amount in the given inventory is either the same or
	 * bigger as in this instance
	 *
	 * @param i
	 *            inventory to check
	 * @return true if this instance is completly contained in the given
	 *         inventory, false if not
	 */
	public boolean isContainedIn(Inventory i) {
		ItemMap invMap = new ItemMap(i);
		ItemMap clone = this.clone();
		if (invMap.getTotalItemAmount() < clone.getTotalItemAmount()) {
			return false;
		}
		if (isEmpty()) {
			return true;
		}
		Map<Integer, Map<ItemStack, List<ItemWrapper>>> diff = calculateIntersections(invMap);
		for (int k = 1; !diff.isEmpty(); k++) {
			Map<ItemStack, List<ItemWrapper>> mapping = diff.get(k);
			if (mapping == null) {
				continue;
			}
			diff.remove(k);
			for (Entry<ItemStack, List<ItemWrapper>> entry : mapping.entrySet()) {
				int amount = invMap.getAmount(entry.getKey());
				for (ItemWrapper iw : entry.getValue()) {
					int wrapperCount = clone.getAmount(iw);
					if (wrapperCount <= 0) {
						continue;
					}
					int amountToRemove = Math.min(wrapperCount, amount);
					clone.removeItemWrapper(iw, amountToRemove);
					invMap.removeItemWrapper(new ItemWrapper(entry.getKey()), amountToRemove);
					amount -= amountToRemove;
				}
				if (clone.isEmpty()) {
					return true;
				}
			}
		}
		return clone.isEmpty();
	}

	public String toString() {
		String res = "";
		for (ItemStack is : getItemStackRepresentation()) {
			res += is.toString() + ";";
		}
		return res;
	}

	/**
	 * Checks how often this ItemMap is contained in the given one or how often
	 * this ItemMap could be removed from the given one before creating negative
	 * stacks
	 *
	 * @param i
	 *            ItemMap to check
	 * @return How often this map is contained in the given ItemMap or
	 *         Integer.MAX_VALUE if this instance is empty
	 */
	public int getMultiplesContainedIn(ItemMap invMap) {
		if (isEmpty()) {
			// dont divide by 0 kids
			return Integer.MAX_VALUE;
		}
		ItemMap clone;
		int count = 0;
		Map<Integer, Map<ItemStack, List<ItemWrapper>>> orig = calculateIntersections(invMap);
		while (true) {
			Map<Integer, Map<ItemStack, List<ItemWrapper>>> diff = new TreeMap<Integer, Map<ItemStack, List<ItemWrapper>>>(
					orig);
			clone = this.clone();
			for (int k = 1; !diff.isEmpty(); k++) {
				Map<ItemStack, List<ItemWrapper>> mapping = diff.get(k);
				if (mapping == null) {
					continue;
				}
				diff.remove(k);
				for (Entry<ItemStack, List<ItemWrapper>> entry : mapping.entrySet()) {
					int amount = invMap.getAmount(entry.getKey());
					if (amount == 0) {
						continue;
					}
					for (ItemWrapper iw : entry.getValue()) {
						int wrapperCount = clone.getAmount(iw);
						if (wrapperCount <= 0) {
							continue;
						}
						int amountToRemove = Math.min(wrapperCount, amount);
						clone.removeItemWrapper(iw, amountToRemove);
						invMap.removeItemWrapper(new ItemWrapper(entry.getKey()), amountToRemove);
						amount -= amountToRemove;
					}
				}
			}
			if (clone.isEmpty()) {
				count++;
			} else {
				return count;
			}
		}
	}

	/**
	 * Checks how often this ItemMap is contained in the given inventory or how
	 * often this ItemMap could be removed from the given inventory before
	 * creating negative stacks
	 *
	 * @param i
	 *            Inventory to check
	 * @return How often this map is contained in the given one or
	 *         Integer.MAX_VALUE if this instance is empty
	 */
	public int getMultiplesContainedIn(Inventory i) {
		return getMultiplesContainedIn(new ItemMap(i));
	}

	/**
	 * Multiplies the whole content of this instance by the given multiplier.
	 * Values for individual ItemWrappers will be rounded down to integers
	 *
	 * @param multiplier
	 *            Multiplier to scale the amount of the contained items with
	 */
	public void multiplyContent(double multiplier) {
		totalItems = 0;
		HashMap<ItemWrapper, Integer> newItems = new HashMap<ItemWrapper, Integer>();
		for (Entry<ItemWrapper, Integer> entry : getEntrySet()) {
			int amount = (int) (entry.getValue() * multiplier);
			newItems.put(entry.getKey(), amount);
			totalItems += amount;
		}
		items = newItems;
	}

	/**
	 * Turns this item map into a list of ItemStacks, with amounts that do not
	 * surpass the maximum allowed stack size for each ItemStack
	 *
	 * @return List of stacksize conform ItemStacks
	 */
	public LinkedList<ItemStack> getItemStackRepresentation() {
		LinkedList<ItemStack> result = new LinkedList<>();
		for (Entry<ItemWrapper, Integer> entry : getEntrySet()) {
			ItemStack is = entry.getKey().getItem();
			Integer amount = entry.getValue();
			while (amount != 0) {
				int addAmount = Math.min(amount, is.getMaxStackSize());
				is.setAmount(addAmount);
				result.add(is);
				amount -= addAmount;
			}
		}
		return result;
	}

	/**
	 * Removes all items in the given ItemMap from this instance. This does
	 * respect wildcards as specified in this instance, but not the ones in the
	 * given ItemMap. If not enough materials are completly cover the given
	 * ItemMap, nothing is removed and false is returned
	 * 
	 * @param im
	 *            ItemMap whichs content should be removed from this instance
	 * @return True if everything was removed, false if not
	 */
	public boolean reduceBy(ItemMap minusMap) {
		if (minusMap.isEmpty()) {
			return true;
		}
		if (minusMap.getMultiplesContainedIn(this) <= 0) {
			return false;
		}
		ItemMap minusClone = minusMap.clone();
		Map<Integer, Map<ItemStack, List<ItemWrapper>>> intersections = calculateIntersections(minusMap);
		for (int k = 1; !intersections.isEmpty(); k++) {
			Map<ItemStack, List<ItemWrapper>> mapping = intersections.get(k);
			if (mapping == null) {
				continue;
			}
			intersections.remove(k);
			for (Entry<ItemStack, List<ItemWrapper>> entry : mapping.entrySet()) {
				int amount = minusClone.getAmount(entry.getKey());
				for (ItemWrapper iw : entry.getValue()) {
					if (amount == 0) {
						continue;
					}
					int wrapperCount = this.getAmount(iw);
					if (wrapperCount <= 0) {
						continue;
					}
					int amountToRemove = Math.min(wrapperCount, amount);
					this.removeItemWrapper(iw, amountToRemove);
					minusClone.removeItemWrapper(new ItemWrapper(entry.getKey()), amountToRemove);
					amount -= amountToRemove;
				}
				if (amount != 0) {
					log.warning("Invalid matching trying to reduce map: " + toString() + " reduced by "
							+ minusMap.toString());
					return false;
				}
			}
		}
		if (minusClone.isEmpty()) {
			log.warning("Invalid matching trying to reduce map, end map not empty: " + toString() + " reduced by "
					+ minusMap.toString());
			return false;
		}
		return true;
	}

	/**
	 * Clones this map
	 */
	public ItemMap clone() {
		ItemMap clone = new ItemMap();
		for (Entry<ItemWrapper, Integer> entry : getEntrySet()) {
			clone.addItemWrapper(entry.getKey(), entry.getValue());
		}
		return clone;
	}

	/**
	 * Checks whether this instance would completly fit into the given inventory
	 *
	 * @param i
	 *            Inventory to check
	 * @return True if this ItemMap's item representation would completly fit in
	 *         the inventory, false if not
	 */
	public boolean fitsIn(Inventory i) {
		int size;
		if (i instanceof PlayerInventory) {
			size = 36; // mojang pls
		} else {
			size = i.getSize();
		}
		ItemMap invCopy = new ItemMap();
		for (ItemStack is : i.getStorageContents()) {
			invCopy.addItemStack(is);
		}
		ItemMap instanceCopy = this.clone();
		instanceCopy.merge(invCopy);
		return instanceCopy.getItemStackRepresentation().size() <= size;
	}

	/**
	 * Instead of converting into many stacks of maximum size, this creates a
	 * stack with an amount of one for each entry and adds the total item amount
	 * and stack count as lore, which is needed to display larger ItemMaps in
	 * inventories
	 *
	 * @return UI representation of large ItemMap
	 */
	public List<ItemStack> getLoredItemCountRepresentation() {
		List<ItemStack> items = new LinkedList<>();
		for (Entry<ItemWrapper, Integer> entry : getEntrySet()) {
			ItemStack is = entry.getKey().getGUIRepresentation();
			ISUtils.addLore(is, ChatColor.GOLD + "Total item count: " + entry.getValue());
			StringBuilder out = new StringBuilder();
			out.append(ChatColor.AQUA);
			out.append(String.valueOf(entry.getValue()));
			out.append(" item");
			out.append(entry.getValue() == 1 ? "" : "s");
			out.append(" total, which equals ");
			if (entry.getValue() > is.getType().getMaxStackSize()) {
				int stacks = entry.getValue() / is.getType().getMaxStackSize();
				int extra = entry.getValue() % is.getType().getMaxStackSize();
				if (stacks != 0) {
					out.append(stacks + " stack" + (stacks == 1 ? "" : "s"));
				}
				if (extra != 0) {
					out.append(" and " + extra);
					out.append(" item" + (extra == 1 ? "" : "s"));
				}
			}
			items.add(is);
		}
		return items;
	}

	/**
	 * Attempts to remove the content of this ItemMap from the given inventory.
	 * If it fails to find all the required items it will stop and return null.
	 * If it does succeed, an ItemMap containing all removed items will be
	 * returned. This is useful, because the actual items removed may very well
	 * not be completly identical to the ones in this instance due to wild
	 * cards. To allow for example item transfers, without resetting certain
	 * aspects of the transferred items due to wildcards, the result of this
	 * method can be used
	 *
	 * @param i
	 *            Inventory to remove from
	 * @return ItemMap containing the item stacks removed or null if nothing or only a part of the
	 *         items was removed
	 */
	public ItemMap removeSafelyFrom(Inventory i) {
		ItemMap invMap = new ItemMap(i);
		ItemMap clone = this.clone();
		ItemMap removedItems = new ItemMap();
		if (!isContainedIn(i)) {
			return null;
		}
		if (invMap.getTotalItemAmount() < clone.getTotalItemAmount()) {
			return null;
		}
		if (isEmpty()) {
			return removedItems;
		}
		Map<Integer, Map<ItemStack, List<ItemWrapper>>> diff = calculateIntersections(invMap);
		for (int k = 1; !diff.isEmpty(); k++) {
			Map<ItemStack, List<ItemWrapper>> mapping = diff.get(k);
			if (mapping == null) {
				continue;
			}
			diff.remove(k);
			for (Entry<ItemStack, List<ItemWrapper>> entry : mapping.entrySet()) {
				int amount = invMap.getAmount(entry.getKey());
				for (ItemWrapper iw : entry.getValue()) {
					int wrapperCount = clone.getAmount(iw);
					if (wrapperCount <= 0) {
						continue;
					}
					int amountToRemove = Math.min(wrapperCount, amount);
					clone.removeItemWrapper(iw, amountToRemove);
					invMap.removeItemWrapper(new ItemWrapper(entry.getKey()), amountToRemove);
					ItemStack cloneStack = entry.getKey().clone();
					cloneStack.setAmount(amountToRemove);
					removedItems.addItemStack(cloneStack);
					if (i.removeItem(cloneStack).values().size() > 0) {
						// could not be removed on the minecraft layer, so we
						// cancel
						return null;
					}
					amount -= amountToRemove;
				}
			}
		}
		return clone.isEmpty() ? removedItems : null;
	}

	public boolean equals(Object o) {
		if (o instanceof ItemMap) {
			ItemMap im = (ItemMap) o;
			if (im.getTotalItemAmount() == getTotalItemAmount()) {
				return im.getEntrySet().equals(getEntrySet());
			}
		}
		return false;
	}

	/**
	 * Utility to not mess with stacks directly taken from inventories
	 *
	 * @param is
	 *            Template ItemStack
	 * @return Cloned ItemStack with its amount set to 1
	 */
	public static ItemStack createMapConformCopy(ItemStack is) {
		ItemStack copy = is.clone();
		copy.setAmount(1);
		net.minecraft.server.v1_10_R1.ItemStack s = CraftItemStack.asNMSCopy(copy);
		if (s == null) {
			log.info("Attempted to create map conform copy of " + copy.toString()
					+ ", but couldn't because this item can't be held in inventories since Minecraft 1.8");
			return null;
		}
		s.setRepairCost(0);
		copy = CraftItemStack.asBukkitCopy(s);
		return copy;
	}

	private static ItemWrapper createMapConformItemWrapper(ItemStack is) {
		if (is == null) {
			return null;
		}
		ItemStack conform = createMapConformCopy(is);
		if (conform == null) {
			return null;
		}
		return new ItemWrapper(conform);
	}

	/**
	 * Utility to add NBT tags to an item and produce a custom stack size
	 *
	 * @param is
	 *            Template Bukkit ItemStack
	 * @param amt
	 *            Output Stack Size
	 * @param map
	 *            Java Maps and Lists representing NBT data
	 * @return Cloned ItemStack with amount set to amt and NBT set to map.
	 */
	public static ItemStack enrichWithNBT(ItemStack is, int amt, Map<String, Object> map) {
		log.info("Received request to enrich " + is.toString());
		ItemStack copy = is.clone();
		amt = (amt < 1 ? 1 : amt > is.getMaxStackSize() ? is.getMaxStackSize() : amt);
		copy.setAmount(amt);
		net.minecraft.server.v1_10_R1.ItemStack s = CraftItemStack.asNMSCopy(copy);
		if (s == null) {
			log.severe("Failed to create enriched copy of " + copy.toString());
			return null;
		}

		NBTTagCompound nbt = s.getTag();
		if (nbt == null) {
			nbt = new NBTTagCompound();
		}

		TagManager.mapToNBT(nbt, map);
		s.setTag(nbt);
		copy = CraftItemStack.asBukkitCopy(s);
		return copy;
	}

	public static NBTTagCompound mapToNBT(NBTTagCompound base, Map<String, Object> map) {
		return TagManager.mapToNBT(base, map);
	}

	public static NBTTagList listToNBT(NBTTagList base, List<Object> list) {
		return TagManager.listToNBT(base, list);
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> serial = new HashMap<String, Object>();
		serial.put("items", items);
		serial.put("totalItems", totalItems);
		return serial;
	}
}
