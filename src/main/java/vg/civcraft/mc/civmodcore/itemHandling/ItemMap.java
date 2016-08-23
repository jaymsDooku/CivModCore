package vg.civcraft.mc.civmodcore.itemHandling;

import static vg.civcraft.mc.civmodcore.CivModCorePlugin.log;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.v1_10_R1.NBTBase;
import net.minecraft.server.v1_10_R1.NBTTagByte;
import net.minecraft.server.v1_10_R1.NBTTagByteArray;
import net.minecraft.server.v1_10_R1.NBTTagCompound;
import net.minecraft.server.v1_10_R1.NBTTagDouble;
import net.minecraft.server.v1_10_R1.NBTTagFloat;
import net.minecraft.server.v1_10_R1.NBTTagInt;
import net.minecraft.server.v1_10_R1.NBTTagIntArray;
import net.minecraft.server.v1_10_R1.NBTTagList;
import net.minecraft.server.v1_10_R1.NBTTagLong;
import net.minecraft.server.v1_10_R1.NBTTagShort;
import net.minecraft.server.v1_10_R1.NBTTagString;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.MemorySection;
import org.bukkit.craftbukkit.v1_10_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
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
 * when compared to a map containing negative values. Additionally this
 * implementation allows durability "wild cards", if you specify -1 as
 * durability it will count as any given durability. When working with multiple
 * ItemMaps this will only work if all methods are executed on the instance
 * containing items with a durability of -1.
 */
public class ItemMap {

	private HashMap<ItemWrapper, Integer> items;

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
	 * @param inv Inventory to base the item map on
	 */
	public ItemMap(Inventory inv) {
		totalItems = 0;
		update(inv);
	}

	/**
	 * Constructor to create an ItemMap based on a single ItemStack
	 *
	 * @param is ItemStack to start with
	 */
	public ItemMap(ItemStack is) {
		items = new HashMap<>();
		totalItems = 0;
		addItemStack(is);
	}

	/**
	 * Constructor to create an item map based on a collection of ItemStacks
	 *
	 * @param stacks Stacks to add to the map
	 */
	public ItemMap(Collection<ItemStack> stacks) {
		items = new HashMap<>();
		addAll(stacks);
	}
	
	/**
	 * Constructor to create an item map based on an array of ItemStacks
	 * 
	 * @param item Stacks to add to the map
	 */
	public ItemMap(ItemStack [] item) {
		items = new HashMap<>();
		totalItems = 0;
		for(ItemStack is : item) {
			addItemStack(is);
		}
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
	 * @param wrapper Wrapper to remove
	 * @param amount Amount to remove
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
		}
		else {
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
	 * @param input ItemStack to insert
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
	 * @param input ItemStack to remove
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
	 * @param input ItemStack to remove
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
	 * @param stacks Stacks to add
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
	 * @param im ItemMap to merge
	 */
	public void merge(ItemMap im) {
		for (Entry<ItemWrapper, Integer> entry : im.getEntrySet()) {
			addItemWrapper(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Updates this ItemMap to make its content identical with the given inventory. Any preexisting items
	 * will be removed. After this method is called, further changes to the inventory will NOT
	 * update this instance
	 * 
	 * @param inv Inventory to base instance on
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
	 * @param entries Entries to add
	 */
	public void addEntrySet(Set<Entry<ItemStack, Integer>> entries) {
		for (Entry<ItemStack, Integer> entry : entries) {
			addItemAmount(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Utility method, which has the amount of items to add as parameter.
	 *
	 * @param input  ItemStack to sort into the map
	 * @param amount Amount associated with the given ItemStack
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
	 * Gets how many many wrappers, which would accept the given ItemStacks,
	 * are in this instance
	 *
	 * @param is Exact ItemStack to search for
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
	 * Gets the most specific ItemWrapper contained in this instance, for which the given ItemStack fulfills it's criterias. 
	 * Most specific means the set of all ItemStacks, which fulfill the criterias of that wrapper is the smallest out of all
	 * those for which the given ItemStack fullfills all criterias
	 * 
	 * @param is ItemStack to search wrapper for
	 * @return Most specific wrapper found or null if no wrapper accepts the given ItemStack
	 */
	public ItemWrapper getMostSpecificWrapper(ItemStack is) {
		ItemWrapper mostSpecific = null;
		for(Entry <ItemWrapper, Integer> entry: getEntrySet()) {
			if (entry.getKey().fulfillsCriterias(is)) {
				if (mostSpecific == null) {
					mostSpecific = entry.getKey();
				}
				else {
					if (mostSpecific.isMoreSpecific(entry.getKey())) {
						mostSpecific = entry.getKey();
					}
				}
			}
		}
		return mostSpecific;
	}

	/**
	 * Checks whether an inventory contains exactly what's described in this
	 * ItemMap
	 *
	 * @param i Inventory to compare
	 * @return True if the inventory is identical with this instance, false if
	 * not
	 */
	public boolean containedExactlyIn(Inventory i) {
		ItemMap invMap = new ItemMap(i);
		ItemMap clone = this.clone();
		for (Entry<ItemWrapper, Integer> entry : invMap.getEntrySet()) {
			int amountToRemove = entry.getValue();
			while (amountToRemove != 0) {
				ItemWrapper mostSpecific = clone.getMostSpecificWrapper(entry.getKey().getItem());
				if (mostSpecific == null) {
					return false;
				}
				amountToRemove = clone.removeItemWrapper(mostSpecific, entry.getValue());
			}
		}
		return clone.totalItems == 0;
	}

	/**
	 * Checks whether this instance is completly contained in the given
	 * inventory, which means every stack in this instance is also in the given
	 * inventory and the amount in the given inventory is either the same or
	 * bigger as in this instance
	 *
	 * @param i inventory to check
	 * @return true if this instance is completly contained in the given
	 * inventory, false if not
	 */
	public boolean isContainedIn(Inventory i) {
		ItemMap invMap = new ItemMap(i);
		ItemMap clone = this.clone();
		for (Entry<ItemWrapper, Integer> entry : invMap.getEntrySet()) {
			int amountToRemove = entry.getValue();
			while (amountToRemove != 0) {
				ItemWrapper mostSpecific = clone.getMostSpecificWrapper(entry.getKey().getItem());
				if (mostSpecific == null) {
					return false;
				}
				amountToRemove = clone.removeItemWrapper(mostSpecific, entry.getValue());
			}
		}
		return clone.totalItems >= 0;
	}

	public String toString() {
		String res = "";
		for (ItemStack is : getItemStackRepresentation()) {
			res += is.toString() + ";";
		}
		return res;
	}

	/**
	 * Checks how often this ItemMap is contained in the given ItemMap or how
	 * often this ItemMap could be removed from the given one before creating
	 * negative stacks
	 *
	 * @param i ItemMap to check
	 * @return How often this map is contained in the given one or
	 * Integer.MAX_VALUE if this instance is empty
	 */
	public int getMultiplesContainedIn(Inventory i) {
		if (totalItems == 0) {
			//dont divide by 0 kids
			return Integer.MAX_VALUE;
		}
		ItemMap invMap = new ItemMap(i);
		ItemMap clone = this.clone();
		int count = 0;
		while (true) {
			//keep looping until empty
			for (Entry<ItemWrapper, Integer> entry : clone.getEntrySet()) {
				int amountToRemove = entry.getValue();
				for(Entry <ItemWrapper, Integer> invEntry : invMap.getEntrySet()) {
					if (entry.getKey().fulfillsCriterias(invEntry.getKey().getItem())) {
						amountToRemove = invMap.removeItemWrapper(invEntry.getKey(), amountToRemove);
						if (amountToRemove == 0) {
							break;
						}
					}
				}
				if (amountToRemove != 0) {
					return count;
				}
			}
			count++;
		}
	}

	/**
	 * Multiplies the whole content of this instance by the given multiplier
	 *
	 * @param multiplier Multiplier to scale the amount of the contained items with
	 */
	public void multiplyContent(double multiplier) {
		totalItems = 0;
		for (Entry<ItemWrapper, Integer> entry : getEntrySet()) {
			items.put(entry.getKey(), (int) (entry.getValue() * multiplier));
			totalItems += (int) (entry.getValue() * multiplier);
		}
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
	 * @param i Inventory to check
	 * @return True if this ItemMap's item representation would completly fit in
	 * the inventory, false if not
	 */
	public boolean fitsIn(Inventory i) {
		int size;
		if (i instanceof PlayerInventory) {
			size = 36;
		}
		else {
			size = i.getSize();
		}
		ItemMap invCopy = new ItemMap();
		for(ItemStack is : i.getStorageContents()) {
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
			ISUtils.addLore(is,
					ChatColor.GOLD + "Total item count: " + entry.getValue());
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
	 * If it fails to find all the required items it will stop and return false
	 *
	 * @param i Inventory to remove from
	 * @return True if everything was successfully removed, false if not
	 */
	public boolean removeSafelyFrom(Inventory i) {
		ItemMap cloneMap = this.clone();
		int itemsRemoved = 1;
		while (itemsRemoved > 0 && cloneMap.getTotalItemAmount() != 0) {
			itemsRemoved = 0;
			for (ItemStack stack : i.getContents()) {
				if (i == null) {
					continue;
				}
				ItemWrapper mostSpecific = cloneMap.getMostSpecificWrapper(stack);
				if (mostSpecific == null) {
					continue;
				}
				int amount = items.get(mostSpecific);
				int toRemove = Math.min(amount, stack.getAmount());
				itemsRemoved += toRemove;
				cloneMap.removeItemWrapper(mostSpecific, toRemove);
				ItemStack cloneStack = stack.clone();
				cloneStack.setAmount(toRemove);
				if (!i.removeItem(cloneStack).isEmpty()) {
					return false;
				}
				if (cloneMap.getTotalItemAmount() == 0) {
					//we are done
					return true;
				}
			}
		}
		return cloneMap.getTotalItemAmount() == 0;
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
	 * @param is Template ItemStack
	 * @return Cloned ItemStack with its amount set to 1
	 */
	private static ItemStack createMapConformCopy(ItemStack is) {
		ItemStack copy = is.clone();
		copy.setAmount(1);
		net.minecraft.server.v1_10_R1.ItemStack s = CraftItemStack
				.asNMSCopy(copy);
		if (s == null) {
			log().info("Attempted to create map conform copy of " + copy.toString()
					+ ", but couldn't because this item can't be held in inventories since Minecraft 1.8");
			return null;
		}
		s.setRepairCost(0);
		copy = CraftItemStack.asBukkitCopy(s);
		return copy;
	}

	/**
	 * Utility to add NBT tags to an item and produce a custom stack size
	 *
	 * @param is  Template Bukkit ItemStack
	 * @param amt Output Stack Size
	 * @param map Java Maps and Lists representing NBT data
	 * @return Cloned ItemStack with amount set to amt and NBT set to map.
	 */
	public static ItemStack enrichWithNBT(ItemStack is, int amt, Map<String, Object> map) {
		log().info("Received request to enrich " + is.toString());
		ItemStack copy = is.clone();
		amt = (amt < 1 ? 1 : amt > is.getMaxStackSize() ? is.getMaxStackSize() : amt);
		copy.setAmount(amt);
		net.minecraft.server.v1_10_R1.ItemStack s = CraftItemStack.asNMSCopy(copy);
		if (s == null) {
			log().severe("Failed to create enriched copy of " + copy.toString());
			return null;
		}

		NBTTagCompound nbt = s.getTag();
		if (nbt == null) {
			nbt = new NBTTagCompound();
		}

		mapToNBT(nbt, map);
		s.setTag(nbt);
		copy = CraftItemStack.asBukkitCopy(s);
		return copy;
	}
	
	public static ItemWrapper createMapConformItemWrapper(ItemStack is) {
		if (is == null) {
			return null;
		}
		ItemStack conform = createMapConformCopy(is);
		if (conform == null) {
			return null;
		}
		return new ItemWrapper(conform);
	}

	public static NBTTagCompound mapToNBT(NBTTagCompound base, Map<String, Object> map) {
		log().info("Representing map --> NBTTagCompound");
		if (map == null || base == null) return base;
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			Object object = entry.getValue();
			if (object instanceof Map) {
				log().info("Adding map at key " + entry.getKey());
				base.set(entry.getKey(), mapToNBT(new NBTTagCompound(), (Map<String, Object>) object));
			} else if (object instanceof MemorySection) {
				log().info("Adding map from MemorySection at key " + entry.getKey());
				base.set(entry.getKey(), mapToNBT(new NBTTagCompound(), ((MemorySection) object).getValues(true)));
			} else if (object instanceof List) {
				log().info("Adding list at key " + entry.getKey());
				base.set(entry.getKey(), listToNBT(new NBTTagList(), (List<Object>) object));
			} else if (object instanceof String) {
				log().info("Adding String " + object + " at key " + entry.getKey());
				base.setString(entry.getKey(), (String) object);
			} else if (object instanceof Double) {
				log().info("Adding Double " + object + " at key " + entry.getKey());
				base.setDouble(entry.getKey(), (Double) object);
			} else if (object instanceof Float) {
				log().info("Adding Float " + object + " at key " + entry.getKey());
				base.setFloat(entry.getKey(), (Float) object);
			} else if (object instanceof Boolean) {
				log().info("Adding Boolean " + object + " at key " + entry.getKey());
				base.setBoolean(entry.getKey(), (Boolean) object);
			} else if (object instanceof Byte) {
				log().info("Adding Byte " + object + " at key " + entry.getKey());
				base.setByte(entry.getKey(), (Byte) object);
			} else if (object instanceof Short) {
				log().info("Adding Byte " + object + " at key " + entry.getKey());
				base.setShort(entry.getKey(), (Short) object);
			} else if (object instanceof Integer) {
				log().info("Adding Integer " + object + " at key " + entry.getKey());
				base.setInt(entry.getKey(), (Integer) object);
			} else if (object instanceof Long) {
				log().info("Adding Long " + object + " at key " + entry.getKey());
				base.setLong(entry.getKey(), (Long) object);
			} else if (object instanceof byte[]) {
				log().info("Adding bytearray at key " + entry.getKey());
				base.setByteArray(entry.getKey(), (byte[]) object);
			} else if (object instanceof int[]) {
				log().info("Adding intarray at key " + entry.getKey());
				base.setIntArray(entry.getKey(), (int[]) object);
			} else if (object instanceof UUID) {
				log().info("Adding UUID " + object + " at key " + entry.getKey());
				base.a(entry.getKey(), (UUID) object);
			} else if (object instanceof NBTBase) {
				log().info("Adding nbtobject at key " + entry.getKey());
				base.set(entry.getKey(), (NBTBase) object);
			} else {
				log().warning("Unrecognized entry in map-->NBT: " + object.toString());
			}
		}
		return base;
	}

	public static NBTTagList listToNBT(NBTTagList base, List<Object> list) {
		log().info("Representing list --> NBTTagList");
		if (list == null || base == null) return base;
		for (Object object : list) {
			if (object instanceof Map) {
				log().info("Adding map to list");
				base.add(mapToNBT(new NBTTagCompound(), (Map<String, Object>) object));
			} else if (object instanceof MemorySection) {
				log().info("Adding map from MemorySection to list");
				base.add(mapToNBT(new NBTTagCompound(), ((MemorySection) object).getValues(true)));
			} else if (object instanceof List) {
				log().info("Adding list to list");
				base.add(listToNBT(new NBTTagList(), (List<Object>) object));
			} else if (object instanceof String) {
				log().info("Adding string " + object + " to list");
				base.add(new NBTTagString((String) object));
			} else if (object instanceof Double) {
				log().info("Adding double " + object + " to list");
				base.add(new NBTTagDouble((Double) object));
			} else if (object instanceof Float) {
				log().info("Adding float " + object + " to list");
				base.add(new NBTTagFloat((Float) object));
			} else if (object instanceof Byte) {
				log().info("Adding byte " + object + " to list");
				base.add(new NBTTagByte((Byte) object));
			} else if (object instanceof Short) {
				log().info("Adding short " + object + " to list");
				base.add(new NBTTagShort((Short) object));
			} else if (object instanceof Integer) {
				log().info("Adding integer " + object + " to list");
				base.add(new NBTTagInt((Integer) object));
			} else if (object instanceof Long) {
				log().info("Adding long " + object + " to list");
				base.add(new NBTTagLong((Long) object));
			} else if (object instanceof byte[]) {
				log().info("Adding byte array to list");
				base.add(new NBTTagByteArray((byte[]) object));
			} else if (object instanceof int[]) {
				log().info("Adding int array to list");
				base.add(new NBTTagIntArray((int[]) object));
			} else if (object instanceof NBTBase) {
				log().info("Adding nbt object to list");
				base.add((NBTBase) object);
			} else {
				log().warning("Unrecognized entry in list-->NBT: " + base.toString());
			}
		}
		return base;
	}

}
