package need4speed402.mods.barrels;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import need4speed402.mods.barrels.client.BarrelStepSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Direction;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Inventory handling for barrels is very interesting because Minecraft's ISidedInventory
 * implementation does not support items stacking more then the 'stack limit'
 * that means I need to do a hack witch is why there are 2 slots in barrels.
 * 
 * Slot 0 is the output slot witch stacksize is equal to the items inside the barrel minus the items in the input slot
 * 
 * Slot 1 is the input slot and it is normally 0 but will grow in stacksize if the inventory is reaching its compacity
 * 
 * when a mod searches for a slot that they can put in a item, they will find the input slot (or the output slot if 
 * the inventory's item count hasen't reached a full 'stack' yet) and put the item in it but really its just a dummy slot
 * that adds its items into the output slot every 'flush cycle' (every time the updateEntity() method gets called)
 * The special thing that this inventory supports that it's competitor does not is accept multiple stacks in one flush
 * cycle because when one inventory is done using the barrel and calls the setInventorySlotContents() method that dummy
 * slot will automatically get flushed or when a new inventory starts using this one, the item will get flushed when it calls
 * getStackInSlot().
 * 
 * The reason why these 'dynamic' flushes only flush its own slot is because it prevents issues with concurrency
 * but also it does cause some problems so I'm still debating what's better
 * 
 * @author Alex Orzechowski
 *
 */
public class TileEntityBarrel extends TileEntity implements ISidedInventory{

	/** Item stored in barrel */
	private ItemStack item;

	/** The text overlay for rendering */
	@SideOnly(Side.CLIENT)
	private String overlay;

	/** a timer used to determine if the player double clicked */
	private byte doubleClickTimer;
	private byte selectedSlot, mode;
	private int lastClicked, lastClickedExpected = -1;
	
	/** stacks that get set by getStackInSlot(), used to detect changes done to variables directly */
	private final ItemStack[] currentStack = new ItemStack[getSizeInventory()];
	private final int[] lastStackSize = new int[getSizeInventory()];
	
	/** the side the tile entity is oriented */
	private int side = 0;
	
	/** boolean list for all the side the barrel render is going to render on true to render this side, false to not */
	private final EntityItemFrame[] frames = new EntityItemFrame[4];
	
	/** update is set to true when a update packet needs to get sent. The requiresCheck is true if lastStack and currentStack may have changes */
	private boolean update = false, requiresCheck = false;
	
	/**
	 * returns a list of all the frames that are on the barrel. they can be null
	 * 
	 * @return all the frames that are on the barrel
	 */
	public EntityItemFrame[] getFrames() {
		return frames;
	}
	
	public int getLastClicked() {
		return lastClicked;
	}
	
	public boolean isAbrupted() {
		return lastClickedExpected == -1;
	}
	
	public void resetLastClicked() {
		lastClickedExpected = 1;
	}
	
	public void setLastClicked(int lastClicked) {
		this.lastClicked = lastClicked;
	}
	
	/**
	 * gets the side the tile entity is oriented
	 * 
	 * @return the side the tile entity is oriented
	 */
	public int getSide() {
		return side;
	}
	
	/**
	 * sets the orientation of this tile entity
	 * 
	 * @param the side the orientation of this tile entity is set to
	 * 
	 * @return void
	 */
	public void setSide(int side) {
		this.side = side;
	}
	
	/**
	 * gets the item stored in the barrel
	 * this method will flush the changes from the buffers (currentStack & lastStack)
	 * into a ItemStack copy
	 * 
	 * for a complete access, use this method.
	 * for normal use, use getStackInSlot()
	 * 
	 * NOTE: Do not change values directly. The barrel will not
	 * update properly if done. Use setItem() after this.
	 * 
	 * @see getStackInSlot()
	 * @see setItem()
	 * @return void
	 */
	public ItemStack getItem() {
		ItemStack item = this.item == null ? null : this.item.copy();
		
		if (requiresCheck){
			for (int slot = 0; slot < getSizeInventory(); slot++){
				if (currentStack[slot] != null && currentStack[slot].stackSize != lastStackSize[slot]){
					if (item == null){
						item = currentStack[slot];
					}
					
					item.stackSize += currentStack[slot].stackSize - lastStackSize[slot];
				}
			}
		}
		return item;
	}
	
	public ItemStack getRawItem (){
		return item;
	}

	/**
	 * sets the item in this tile entity
	 * 
	 * you might want to create a copy of the ItemStack you are passing
	 * because the item in the barrel is directly set to the one passed
	 * 
	 * @return the item this barrel is getting set to
	 */
	public void setItem(ItemStack item) {
		for (int slot = 0; slot < getSizeInventory(); slot++){
			currentStack[slot] = null;
		}
		
		this.item = item;
		
		requiresUpdate();
		requiresCheck = false;
	}
	
	/**
	 * gets called when tile is validated.
	 * It request the server for the items inside the barrel 
	 * (because the server loads the map, not the client)
	 * 
	 * @return void
	 */
	@Override
	public void validate(){
		super.validate();
		
		if (worldObj.isRemote){
			PacketHandler.getInstance().sendServerRequestForItem(this.xCoord, this.yCoord, this.zCoord);
		}
	}
	
	/**
	 * gets the maximum inventory size in individual items
	 * (not in stacks)
	 * 
	 * NOTE: this is not the getSizeInventory method
	 * 
	 * that vanilla uses
	 * @return the inventory size
	 */
	public int getInventorySize() {
		return this.getStackLimit() * this.item.getMaxStackSize();
	}
	
	/**
	 * Gets the max number of stacks the barrel can hold
	 * used as a utility function
	 * 
	 * @return the maximum number of stacks this tile entity can hold
	 */
	public int getStackLimit() {
		switch (this.getBlockMetadata()){
			case 0:
				return Barrels.instance.T1BarrelMaxStorage;
			case 1:
				return Barrels.instance.T2BarrelMaxStorage;
			case 2:
				return Barrels.instance.T3BarrelMaxStorage;
			default:
				return 0;
		}
	}
	
	/**
	 * gets the inventory size
	 * 
	 * @return the inventory size
	 */
	@Override
	public int getSizeInventory() {
		return 2;
	}
	
	/**
	 * gets called every tick.
	 * 
	 * Used for decreasing double-click timer counter
	 * 
	 * The most important thing this method does is it flushes the item buffer
	 * to the item variable within this class.
	 * 
	 * When a mod does something like getStackInSlot().stackSize += 1
	 * the item buffer will detect that and flush it because the item buffer always points to
	 * the variable passed by getStackInSlot. These variables also help with issues when mods use
	 * decrStackSize() in conjunction with setStackInSlot() (vanilla hoppers)
	 * 
	 * @return void
	 */
	@Override
	public void updateEntity() {
		if (this.isTimerActive()) {
			this.doubleClickTimer--;
		}
		
		if (lastClickedExpected >= 0){
			lastClickedExpected--;
			
			if (lastClicked > 0){
				lastClicked--;
			}
			
			if (lastClickedExpected == -1){
				lastClicked = 0;
				
				if (this.worldObj.isRemote){
					((BarrelStepSound) this.getBlockType().stepSound).ignoreNextCall(false);
				}
			}
		}
		
		if (requiresCheck){
			flush();
		}
		
		if (!worldObj.isRemote){
			for (int i = 0; i < frames.length; i++){
				int x = this.xCoord + Direction.offsetX[i];
				int y = this.yCoord;
				int z = this.zCoord + Direction.offsetZ[i];
				
				List list = worldObj.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItemFrame.class, AxisAlignedBB.getAABBPool().getAABB(x, y, z, x + 1, y + 1, z + 1));
				
				if (list != null && list.size() > 0){
					for (int size = 0; size < list.size(); size++){
						net.minecraft.entity.item.EntityItemFrame frame = (net.minecraft.entity.item.EntityItemFrame) list.get(size);
						if (frame.hangingDirection == i){
							if (frames[i] != null){
								Barrels.instance.barrel.spawnItem(worldObj, x, y, z, new ItemStack(Item.itemFrame, 2, 0), null);
								frames[i] = null;
							}else{
								frames[i] = new EntityItemFrame(frame);
								
								{
									ItemStack item = null;
									if (this.item != null){
										item = this.item.copy();
										item.stackSize = 1;
									}
									
									frames[i].setDisplayedItem(item);
								}
							}
							frame.setDead();
							
							if (update){
								update(true);
							}else{
								PacketHandler.getInstance().sendClientsFrameChange(this);
							}
						}
					}
				}
			}
		}
		
		if (update){
			update(false);
		}
	}
	
	private void flush() {
		for (int slot = 0; slot < getSizeInventory(); slot++){
			if (currentStack[slot] != null){
				if (currentStack[slot].stackSize != lastStackSize[slot]){
					if (item == null){
						item = currentStack[slot];
					}
					
					if (lastStackSize[slot] - currentStack[slot].stackSize >= item.stackSize){
						item = null;
					}else{
						item.stackSize += currentStack[slot].stackSize - lastStackSize[slot];
					}
					
					localUpdate();
					update = true;
				}
				
				currentStack[slot] = null;
			}
		}
		
		requiresCheck = false;
	}
	
	/**
	 * updates client/server
	 * 
	 * if this is the server, the new item will get sent to all
	 *    the clients connected to the server in the same dimension
	 *    
	 * if this is the client, it will recalculate the overlay
	 *    text
	 *    
	 * @return void
	 */
	private void update(boolean sendFrames){
		if (this.worldObj.isRemote){
			localUpdate();
		}else{
			PacketHandler.getInstance().updateClients(this, sendFrames);
		}
		
		this.worldObj.func_96440_m(this.xCoord, this.yCoord, this.zCoord, this.getBlockType().blockID);
		
		update = false;
	}
	
	@SideOnly(Side.CLIENT)
	public void localUpdate (){
		ItemStack item = this.getItem();
		
		if (item == null){
			overlay = null;
		}else{
			StringBuilder overlay = new StringBuilder();
			
			int maxSize = item.getMaxStackSize();
			int stackCount = item.stackSize / maxSize;
			
			if (maxSize == 1){
				overlay.append(stackCount);
			}else{
				int excess = item.stackSize % maxSize;
				
				if (stackCount > 0){
					if (stackCount > 1) overlay.append(stackCount).append(" x "); 
					overlay.append(maxSize);
					if (excess > 0) overlay.append(" + ");
				}
				
				if (excess > 0) overlay.append(excess);
			}
			
			this.overlay = overlay.toString();
		}
		
		for (int i = 0; i < frames.length; i++){
			if (frames[i] != null){
				ItemStack item1 = null;
				if (item != null){
					item1 = this.item.copy();
					item1.stackSize = 1;
				}
				
				frames[i].setDisplayedItem(item1);
			}
		}
		
		worldObj.markBlockForRenderUpdate(xCoord, yCoord, zCoord);
	}
	
	public void requiresUpdate (){
		update = true;
		
		if (worldObj != null && this.worldObj.isRemote){
			localUpdate();
		}
	}

	/**
	 * writes the barrel's data to the map when unloading calculates the excess items in a dummy item before saving
	 * 
	 * @tag item - the item id of the item inside the barrel -1 when the barrel is empty in older versions so make sure you check for -1
	 * @tag size - the number of items inside the barrel
	 * @tag metadata - the metadata stores inside the barrel
	 * @tag tag - the NBTTaggCompound when a item inside the barrel has NBT
	 * 
	 * @param the TagCompound being written to
	 * 
	 * @return void
	 */
	@Override
	public void writeToNBT(NBTTagCompound NBT) {
		super.writeToNBT(NBT);
		
		ItemStack item = this.getItem();
		
		if (item != null){
			NBT.setInteger("item", item.itemID);
			NBT.setInteger("size", item.stackSize);
			NBT.setInteger("metadata", item.getItemDamage());
			
			if (item.getTagCompound() != null){
				NBT.setCompoundTag("tag", item.getTagCompound());
			}
			
			if (item.getItem() instanceof ItemMap){
				NBT.setInteger("dim", worldObj.provider.dimensionId);
				
				NBTTagCompound cool = new NBTTagCompound();
				Item.map.getMapData(item, worldObj).writeToNBT(cool);
				NBT.setCompoundTag("map", cool);
			}
		}
		
		{
			byte packed = 0;
			
			for (int i = 0; i < frames.length; i++){
				if (frames[i] != null){
					packed |= 1 << i;
				}
			}
			
			if (packed != 0){
				NBT.setInteger("dim", worldObj.provider.dimensionId);
				NBT.setByte("frame", packed);
			}
		}
		
		NBT.setByte("side", (byte) getSide());
	}

	/**
	 * reads from the map data on load
	 * 
	 * @tag item - the item id of the item inside the barrel -1 when the barrel is empty in older versions so make sure you check for -1
	 * @tag size - the number of items inside the barrel
	 * @tag metadata - the metadata stores inside the barrel
	 * @tag tag - the NBTTagCompound when a item inside the barrel has NBT
	 * 
	 * @param the TagCompound being read from
	 * 
	 * @return void
	 */
	@Override
	public void readFromNBT(NBTTagCompound NBT) {
		super.readFromNBT(NBT);
		
		if (worldObj == null && NBT.hasKey("dim")){
			worldObj = DimensionManager.getWorld(DimensionManager.getProviderType(NBT.getInteger("dim")));
		}
		
		if (NBT.hasKey("item")) {
			int itemID = NBT.getInteger("item");
			if (itemID == -1 || Item.itemsList[itemID] == null){
				this.setItem(null);
			}else{
				this.setItem(new ItemStack(itemID, NBT.getInteger("size"), NBT.getInteger("metadata")));
			}
			
			if (NBT.hasKey("tag") && item != null){
				this.item.setTagCompound(NBT.getCompoundTag("tag"));
			}
			
			if (NBT.hasKey("map") && item != null && item.getItem() instanceof ItemMap){
				Item.map.getMapData(item, worldObj);
			}
		}else{
			setItem(null);
		}
		
		if (NBT.hasKey("frame")){
			byte packed = NBT.getByte("frame");
			
			for (int i = 0; i < frames.length; i++){
				if ((packed & (1 << i)) != 0){
					//I know worldObj is null at this stage (REALLY bad game design) but maybe Mojang will get their shit together...
					frames[i] = new EntityItemFrame(worldObj, xCoord, yCoord, zCoord, i);
				}
			}
		}
		
		this.setSide(NBT.getByte("side"));
	}

	/**
	 * starts the double-click timer
	 * plus sets the slot the player was holding when last clicked
	 * 
	 * @param the slot the player is selecting
	 * 
	 * @return void
	 */
	public void setClick(int selectedSlot, int mode) {
		this.selectedSlot = (byte) selectedSlot;
		this.mode = (byte) mode;
		doubleClickTimer = 10;
	}

	/**
	 * returns the selected slot from when the last setClick was called
	 * 
	 * @return the selected slot
	 */
	public int getSelectedSlot() {
		return selectedSlot;
	}
	
	public int getMode (){
		return mode;
	}

	/**
	 * resets the doubleClick timer
	 * 
	 * @return void
	 */
	public void resetTimer() {
		doubleClickTimer = -1;
	}

	/**
	 * returns if the timer used to determine if the player has clicked the
	 * barrel before is active
	 * 
	 * @return true if the timer is active
	 * false otherwise
	 */
	public boolean isTimerActive() {
		return doubleClickTimer > -1;
	}

	/**
	 * Used for the barrel render, it gets the overlay of the barrel for render
	 * 
	 * @return the overlay
	 */
	@SideOnly(Side.CLIENT)
	public String getOverlay() {
		return overlay;
	}
	
	/**
	 * gets the size of a stack in a slot
	 * 
	 * @param the slot to get the item out of
	 * 
	 * @return the amount of items in this slot
	 */
	private int getStackSizeInSlot(int slot) {
		ItemStack item = this.getItem();
		
		if (item != null){
			switch (slot % 2){
				case 0:
					return item.stackSize - Math.max(item.stackSize - this.getInventorySize() + item.getMaxStackSize(), 0);
				case 1:
					return Math.max(item.stackSize - this.getInventorySize() + item.getMaxStackSize(), 0);
			}
		}
		
		return 0;
	}
	
	/**
	 * Gets the ItemStack in a slot
	 * 
	 * the item buffer is set to what the method passes so that this barrel has a continuous
	 * pointer if the buffer was populated by a ItemStack before, it will be flushed and
	 * a new pointer will be created. As my goal of this new system is to make it behave
	 * as closely to a regular chest as possible, a new pointer is not actually created,
	 * it's stacksize is reset instead.
	 * 
	 * @param the slot
	 * 
	 * @param the ItemStack in the slot
	 */
	@Override
	public ItemStack getStackInSlot(int slot) {
		if (slot < 0 || slot >= getSizeInventory()){
			throw new IndexOutOfBoundsException();
		}
		
		ItemStack item = this.getItem();
		
		if (item != null){
			if (this.item == null){
				flush();
			}
			
			int slotSize = getStackSizeInSlot(slot);
			
			if (currentStack[slot] != null){
				if (currentStack[slot].stackSize != lastStackSize[slot]){
					this.item.stackSize += currentStack[slot].stackSize - lastStackSize[slot];
					requiresUpdate();
					
					currentStack[slot].stackSize = lastStackSize[slot] = slotSize;
				}else if (lastStackSize[slot] != slotSize){
					currentStack[slot].stackSize = lastStackSize[slot] = slotSize;
				}
				
				requiresCheck = true;
				return currentStack[slot];
			}else{
				item.stackSize = slotSize;
				
				currentStack[slot] = item;
				lastStackSize[slot] = item.stackSize;
				
				requiresCheck = true;
				return item;
			}
		}else{
			if (this.item != null){
				flush();
				
				return getStackInSlot(slot);
			}
			
			currentStack[slot] = null;
		}
		
		return null;
	} 
	
	/**
	 * Sets the contents of the inventory of the specified slot.
	 * 
	 * this is where the magic happens. This method adds/subtracts
	 * the amount of items relative to the pointer. If there is no pointer,
	 * it will make a new one. This does things differently then the other methods
	 * instead of flushing the buffer when it can be done, the pointers are reset
	 * and no changes are done. There is an issue, if you reset the pointers while
	 * not creating new ones, mods that have a reference to this object will have
	 * their variables reset. That's why I'm checking this with using the '=='
	 * operator. This will not reset any other pointers.
	 * 
	 * 
	 * @param the slot the item is getting set to
	 * @param the new stack
	 * 
	 * @return void
	 */
	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		if (slot < 0 || slot >= getSizeInventory()){
			throw new IndexOutOfBoundsException();
		}
		
		ItemStack item = this.getItem();
		
		if (item == null){
			if (stack != null){
				if (stack != currentStack[slot]){
					flush();
					this.item = stack.copy();
					
					currentStack[slot] = stack;
					lastStackSize[slot] = stack.stackSize;
					
					requiresUpdate();
				}
			}else{
				currentStack[slot] = null;
			}
		}else{
			if (this.item == null){
				flush();
			}
			
			int fakeStack;
			
			if (currentStack[slot] != null){
				if (currentStack[slot] != stack){
					currentStack[slot].stackSize = lastStackSize[slot];
				}
				
				fakeStack = lastStackSize[slot];
			}else{
				fakeStack = getStackSizeInSlot(slot);
			}
			
			if (fakeStack - (stack == null ? 0 : stack.stackSize) != 0){
				if (stack == null){
					if (this.item.stackSize > fakeStack){
						this.item.stackSize -= fakeStack;
					}else{
						this.item = null;
					}
					
					currentStack[slot] = null;
				}else{
					this.item.stackSize += stack.stackSize - fakeStack;
					
					currentStack[slot] = stack;
					lastStackSize[slot] = stack.stackSize;
				}
				
				requiresUpdate();
			}
		}
		
		if (stack != null){
			requiresCheck = true;
		}
	}


	/**
	 * adds a ItemStack to a barrel.
	 * 
	 * @param the ItemStack to be added
	 * 
	 * @return a new ItemStack. Returns null if
	 * entire stack could fit, otherwise it returns
	 * the extra items remaining
	 * 
	 * works similarly as 
	 * TileEntityBarrel.decrStackSize()
	 */
	public ItemStack addItem(ItemStack item) {
	if (item != null && item.stackSize > 0) {
			int inventorySize = this.item == null ? this.getStackLimit() * item.getMaxStackSize() : this.getInventorySize();
			ItemStack item2 = this.getItem();
			
			if (item2 == null) {
				if (item.stackSize > inventorySize){
					{
						ItemStack item1 = item.copy();
						item1.stackSize = inventorySize;
						this.setItem(item1);
					}
					
					item.stackSize -= inventorySize;
				}else{
					this.setItem(item.copy());
					item = null;
				}
			} else if (this.equals(item)) {
				if (item2.stackSize + item.stackSize > inventorySize){
					item.stackSize -= inventorySize - item2.stackSize;

					item2.stackSize = inventorySize;
					this.setItem(item2);
				}else{
					item2.stackSize += item.stackSize;
					this.setItem(item2);
					
					item = null;
				}
			}
			return item;
		}
		return null;
	}
	
	/**
	 * Decreases the amount stored in the barrel.
	 * 
	 * @see setInventorySlotContents()
	 * 
	 * @param the slot the item is getting decreased
	 * @param the value of how many items are getting decreased
	 * 
	 * @return the remaining item
	 */
	@Override
	public ItemStack decrStackSize(int slot, int value) {
		if (slot < 0 || slot >= getSizeInventory()){
			throw new IndexOutOfBoundsException();
		}
		
		if (this.item != null && value != 0) {
			update = true;
			
			if (value > this.item.stackSize){
				value = this.item.stackSize;
			}
			
			ItemStack excess = item.copy();
			
			getStackInSlot(slot).stackSize -= value;
			
			excess.stackSize = value;
			return excess;
		} else return null;
	}

	/**
	 * returns true if the player can use the inventory
	 * 
	 * @param the entity opening the inventory
	 * 
	 * @return boolean; if the player can open the inventory
	 */
	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return false;
	}

	/**
	 * gets the stack limit per slot
	 * 
	 * @return the stack limit
	 */
	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	/** 
	 * Called when the inventory is opened
	 * 
	 * @return void
	 */
	public void openChest() {
		
	}

	/** 
	 * Called when the inventory is closed
	 * 
	 * @return void
	 */
	public void closeChest() {
		
	}

	/**
	 * Used for crafting tables and perhaps other inventories.
	 * 
	 * return the ItemStack dropped when the inventory closes
	 */
	@Override
	public ItemStack getStackInSlotOnClosing(int i) {
		return null;
	}

	/**
	 * gets the inventory name
	 * 
	 * @return the inventory name
	 */
	@Override
	public String getInvName() {
		return "container.barrel";
	}
	
	private static final int[][] ROTATION_MATRIX = {
		{1, 1, 1, 1},
		{0, 0, 0, 0},
		{2, 5, 3, 4},
		{3, 4, 2, 5},
		{5, 3, 4, 2},
		{4, 2, 5, 3},
	};
	
	/**
	 * code used to determine which mode is used for a inventory side
	 * 
	 * @param the side of the inventory that the accessor is trying to access
	 * 
	 * @return the inventory mode
	 */
	public byte getModeForSide (int side){
		return this.getMode(ROTATION_MATRIX[side][getSide()]);
	}
	
	/**
	 * reads of the configuration file for the mode code number
	 * by the index of a list.
	 * 
	 * @param the index of the sides
	 * 
	 * @return the inventory mode
	 */
	private byte getMode(int index) {
		return Barrels.instance.interaction[index];
	}
	
	/**
	 * compares 2 ItemStack variables to see if they are the same
	 * take NBT into consideration
	 * does not take item stack count into consideration
	 * 
	 * @param the ItemStack to compare
	 * @return weather the two items stacks equal each other
	 */
	public boolean equals(ItemStack item) {
		return TileEntityBarrel.equals(item, this.getItem());
	}
	
	
	public static boolean equals(ItemStack item, ItemStack item2){
		return (item == item2 || (item != null && item2 != null && item.isItemEqual(item2))) &&
				(item.getTagCompound() == item2.getTagCompound() || (item.getTagCompound() != null && item2.getTagCompound() != null &&
				Arrays.equals(item.getTagCompound().getTags().toArray(), item2.getTagCompound().getTags().toArray())));
	}
	/**
	 * used to check if a item is able to enter the inventory
	 * 
	 * @param the slot the item is trying to go in
	 * @param the item that is trying to enter the inventory
	 * 
	 * @return weather the item can enter
	 */
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack item) {
		ItemStack item2 = this.getItem();
		
		return item2 == null || equals(item, item2);
	}
	
	/**
	 * Checks if the name should be translated
	 * by the translator. If true, it will not get translated,
	 * otherwise not.
	 * 
	 * Since barrels don't have a inventory, this does not apply.
	 * 
	 * @return if the inventory name is localized
	 */
	@Override
	public boolean isInvNameLocalized() {
		return true;
	}
	
	/**
	 * gets the size of the inventory of the specified side.
	 */
	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		switch (this.getModeForSide(side)){
		case 1:
			return new int[] { 0 };
		case 2:
			return new int[] { 1 };
		case 3:
			return new int[] { 0, 1 };
		}
		
		return new int[] {};
	}
	
	/**
	 * checks if an item can enter the inventory
	 * 
	 * @param the slot to check
	 * @param the item being inserted
	 * @param the face the inventory that is calling this is facing
	 * 
	 * @return if the item can be inserted
	 */
	@Override
	public boolean canInsertItem(int slot, ItemStack item, int side) {
		return slot % 2 == 1 && this.isItemValidForSlot(slot, item);
	}
	
	/**
	 * checks if an item can exit the inventory
	 * 
	 * @param the slot to check
	 * @param the item being extracted
	 * @param the face the inventory that is calling this is facing
	 * 
	 * @return if the item can be extracted
	 */
	@Override
	public boolean canExtractItem(int slot, ItemStack item, int side) {
		return slot % 2 == 0 && this.isItemValidForSlot(slot, item);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return AxisAlignedBB.getBoundingBox((float) xCoord - 0.5F, (float) yCoord  - 0.5F, (float) zCoord  - 0.5F,
				(float) xCoord + 1.5F, (float) yCoord + 1.5F, (float) zCoord + 1.5F);
	}
}