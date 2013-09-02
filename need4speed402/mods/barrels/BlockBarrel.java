package need4speed402.mods.barrels;

import java.rmi.UnexpectedException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import need4speed402.mods.barrels.client.BarrelStepSound;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockBarrel extends BlockContainer {
	
	public static final int STARTING_TIME = 20;
	
	/** random used for item dropping */
	private Random random = new Random();
	
	/** the textures for the barrels in a list */
	@SideOnly(Side.CLIENT)
	private Icon[] textures;
	
	/**
	 * Configures the block
	 * 
	 * @param id
	 */
	public BlockBarrel(int id) {
		super(id, Material.wood);
		this.setCreativeTab(CreativeTabs.tabDecorations);
		this.setHardness(2.0F);
		this.setUnlocalizedName("barrel");
		this.setBurnProperties(this.blockID, 1, 2);
	}
	
	/**
	 * Gets the block icon from the side and the metadata
	 * 
	 * @param the side the block wants the icon from
	 * @param the metadata
	 * 
	 * @return the icon to be rendered
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public Icon getIcon(int side, int metadata) {
		if (side == 0 || side == 1){
			return this.textures[metadata];
		}else{
			return this.textures[metadata + 3];
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Icon getBlockTexture(IBlockAccess world, int x, int y, int z, int side) {
		if (side == 0 || side == 1){
			return getIcon(side, world.getBlockMetadata(x, y, z));
		}
		
		TileEntityBarrel barrel = this.getTileEntity(world, x, y, z);
		
		if (barrel.getItem() != null && barrel.getFrames()[Direction.facingToDirection[side]] == null && (!Barrels.instance.onlyRenderOneSide || barrel.getSide() == Direction.facingToDirection[side])){
			return textures[barrel.getBlockMetadata() + 6];			
		}else{
			return textures[barrel.getBlockMetadata() + 3];
		}
	}
	
	/**
	 * registers the icons
	 * 
	 * @param the IconRegister
	 * 
	 * @return void
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister register){
		textures = new Icon[9];
		
		textures[0] = register.registerIcon("barrels:T1Top");
		textures[1] = register.registerIcon("barrels:T2Top");
		textures[2] = register.registerIcon("barrels:T3Top");
		
		textures[3] = register.registerIcon("barrels:T1Side");
		textures[4] = register.registerIcon("barrels:T2Side");
		textures[5] = register.registerIcon("barrels:T3Side");
		
		textures[6] = register.registerIcon("barrels:T1RenderSide");
		textures[7] = register.registerIcon("barrels:T2RenderSide");
		textures[8] = register.registerIcon("barrels:T3RenderSide");
	}
	
	private TileEntityBarrel getTileEntity(IBlockAccess world, int x, int y, int z){
		if (world.getBlockId(x, y, z) == this.blockID){
			TileEntity entity = world.getBlockTileEntity(x, y, z);
			
			if (entity instanceof TileEntityBarrel){
				return (TileEntityBarrel) entity;
			}
			
			//attempt to fix the problem where possible
			if (world instanceof World){
				entity = new TileEntityBarrel();
				entity.setWorldObj((World) world);
				entity.xCoord = x;
				entity.yCoord = y;
				entity.zCoord = z;
				
				((World) world).setBlockTileEntity(x, y, z, entity);
				
				return (TileEntityBarrel) entity;
			}
		}
		
		//i like crashing the game when its this fucked up
		throw new IllegalAccessError("There was a fatal problem thats is not related the Barrels");
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean addBlockHitEffects(World world, MovingObjectPosition target, EffectRenderer effectRenderer) {
		TileEntityBarrel barrel = this.getTileEntity(world, target.blockX, target.blockY, target.blockZ);
		
		return barrel.getLastClicked() > 0;
	}
	
	@Override
	public float getBlockHardness(World world, int x, int y, int z) {
		TileEntityBarrel barrel = this.getTileEntity(world, x, y, z);
		
		barrel.resetLastClicked();
		
		if (barrel.getLastClicked() > 0){
			if (world.isRemote){
				((BarrelStepSound) stepSound).ignoreNextCall(true);
			}
			
			//-1 meaning unbreakable
			return -1;
		}else{
			return super.getBlockHardness(world, x, y, z);
		}
	}
	
	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		TileEntityBarrel barrel = this.getTileEntity(world, x, y, z);
		
		int side = 0;
		
		for (int i = 0; i < 4; i++){
			Block block = Block.blocksList[world.getBlockId(x + Direction.offsetX[i], y, z + Direction.offsetZ[i])];
			if (block == null){
				side = i;
				
				break;
			}else if (!block.isOpaqueCube()){
				side = i;
			}
		}
		
		barrel.setSide(side);
	}
	
	/**
	 * Called when a block gets placed in the world
	 * Used to determine witch side the barrel was placed on
	 * 
	 * @param the world the block was placed in
	 * @param the x coordinate the block was placed at
	 * @param the y coordinate the block was placed at
	 * @param the z coordinate the block was placed at
	 * @param the entity who placed the block
	 * @param the block placed
	 * 
	 * @return void
	 */
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack item) {
		TileEntityBarrel barrel = this.getTileEntity(world, x, y, z);
		
		barrel.setSide(Direction.rotateOpposite[Math.round(player.rotationYaw / 90) & 3]);
	}
	
	/**
	 * Returns true if this block can interact with a
	 * comparator
	 * 
	 * @return if this block can interact with a
	 * comparator
	 */
	@Override
	public boolean hasComparatorInputOverride() {
		return true;
	}
	
	/**
	 * this method is used to get the strength of a redstone signal
	 * a redstone comparator gives off.
	 * 
	 * @param the world where to comparator is checking
	 * @param the x coordinate being checked
	 * @param the y coordinate being checked
	 * @param the z coordinate being checked
	 * @param the side where the comparator is trying to get the results
	 * 
	 * @return redstone strength
	 */
	@Override
	public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
		int powerInput = (world.getBlockMetadata(x + Direction.offsetX[side], y, z + Direction.offsetZ[side]) & 4) == 0 ? 0 : world.getBlockPowerInput(x, y, z);
		
		TileEntityBarrel barrel = this.getTileEntity(world, x, y, z);
		
		if (barrel.getItem() != null){
			int result = (int) Math.round((double) barrel.getItem().stackSize / (double) barrel.getInventorySize() * 15.0D);
			
			if (result == 0) result = 1;
			if (result == 15 && barrel.getItem().stackSize != barrel.getInventorySize()) result = 14;
			
			return Math.max(powerInput, result);
		}
		
		return powerInput;
	}
	
	/**
	 * Called when the block is right-clicked
	 * 
	 * @param the World object the block was clicked
	 * @param the x coordinate the block was placed at
	 * @param the y coordinate the block was placed at
	 * @param the z coordinate the block was placed at
	 * @param the EntityPlayer who clicked the block
	 * @param the side of the block hit
	 * @param the place clicked relative to the x position of the block
	 * @param the place clicked relative to the y position of the block
	 * @param the place clicked relative to the z position of the block
	 * 
	 * @return if the block the the player is holding should be placed
	 */
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float blockX, float blockY, float blockZ) {
		TileEntityBarrel barrel = this.getTileEntity(world, x, y, z);
		
		int currentItem = player.inventory.currentItem;
		boolean changed = false;
		ItemStack playerItem = player.inventory.getStackInSlot(currentItem) == null ? null : player.inventory.getStackInSlot(currentItem).copy();
		
		if (barrel.isTimerActive()){
			barrel.resetTimer();
			ItemStack item = barrel.getItem();
			
			if (item != null){
				if (barrel.getSelectedSlot() == currentItem && barrel.getMode() == 0) {
					if ((item.getItemDamage() > barrel.getBlockMetadata()) || (playerItem != null && playerItem.getItemDamage() > barrel.getBlockMetadata())){
						int oldMeta = barrel.getBlockMetadata();
						int newMeta = -1;
						
						if (playerItem == null) {
							newMeta = item.getItemDamage();
							if (item.stackSize == 1){
								barrel.setItem(null);
							}else{
								item.stackSize -= 1;
								barrel.setItem(item.copy());
							}
						} else if (playerItem.stackSize == 1) {
							newMeta = playerItem.getItemDamage();
						}
						
						if (newMeta != -1) {
							world.setBlockMetadataWithNotify(x, y, z, newMeta, 1);
							
							if (world.isRemote){
								world.markBlockForRenderUpdate(x, y, z);
							}else{
								PacketHandler.getInstance().sendClientsBlockChange(barrel, player);
							}
							
							barrel.blockMetadata = newMeta;
							playerItem = new ItemStack(this, 1, oldMeta);
							changed = true;
						}
					}
				}else if (barrel.getSelectedSlot() == currentItem && barrel.getMode() == 1 && (playerItem == null || barrel.equals(playerItem))){
					for (int slot = 0; slot < player.inventory.mainInventory.length; slot++) {
						player.inventory.setInventorySlotContents(slot, barrel.addItem(player.inventory.getStackInSlot(slot)));
					}
					
					changed = true;
				}else if (playerItem != null){
					if (playerItem.itemID == this.blockID && playerItem.getItemDamage() > barrel.getBlockMetadata()) {
						barrel.setClick(currentItem, 0);
					}else if (item == null || barrel.equals(playerItem)) {
						barrel.setClick(currentItem, 1);
					}
					
					playerItem = barrel.addItem(playerItem);
					changed = true;
				}
			}
		}else if (playerItem != null){
			if (playerItem.itemID == this.blockID && playerItem.getItemDamage() > barrel.getBlockMetadata()) {
				barrel.setClick(currentItem, 0);
				
				playerItem = barrel.addItem(playerItem);
				changed = true;
			}else if (barrel.getItem() == null || barrel.equals(playerItem)) {
				barrel.setClick(currentItem, 1);
				
				playerItem = barrel.addItem(playerItem);
				changed = true;
			}
		}
		
		if (changed){
			player.inventory.setInventorySlotContents(currentItem, playerItem);
			player.inventory.onInventoryChanged();
			
			if (!world.isRemote){
				player.inventoryContainer.detectAndSendChanges();
			}
		}
		
		return true;
	}

	/**
	 * called when a barrel is left-clicked
	 * 
	 * @param the World object the block was clicked
	 * @param the x coordinate the block was placed at
	 * @param the y coordinate the block was placed at
	 * @param the z coordinate the block was placed at
	 * @param the EntityPlayer who clicked the block
	 * 
	 * @return void
	 */
	@Override
	public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
		TileEntityBarrel barrel = this.getTileEntity(world, x, y, z);
		
		ItemStack item = barrel.getItem();
		ItemStack playerItem = player.inventory.getStackInSlot(player.inventory.currentItem);
		
		if (item != null && barrel.isAbrupted()){
			if (barrel.isTimerActive() && barrel.getSelectedSlot() != player.inventory.currentItem && playerItem == null && barrel.getMode() == 2){
				barrel.resetTimer();
				
				barrel.setItem(spawnItem(player, item));
			}else{
				if (playerItem == null){
					barrel.setClick(player.inventory.currentItem, 2);
				}else{
					barrel.resetTimer();
				}
				
				if (player.isSneaking()) {
					if (item.stackSize == 1){
						barrel.setItem(null);
					}else{
						item.stackSize--;
						barrel.setItem(item.copy());
					}
					
					item.stackSize = 1;
					this.spawnItem (world, player.posX, player.posY, player.posZ, item, player);
				}else{
					int maxStackSize = item.getMaxStackSize();
					
					if (barrel.getItem().stackSize > maxStackSize) {
						item.stackSize -= maxStackSize;
						barrel.setItem(item.copy());
						
						item.stackSize = maxStackSize;
						this.spawnItem (world, player.posX, player.posY, player.posZ, item, player);
					} else if (barrel.getItem().stackSize != 1 && Barrels.instance.leaveOneItem) {
						item.stackSize--;
						this.spawnItem (world, player.posX, player.posY, player.posZ, item, player);
						
						item.stackSize = 1;
						barrel.setItem(item.copy());
					}else{
						this.spawnItem (world, player.posX, player.posY, player.posZ, item, player);
						barrel.setItem(null);
					}
				}
				
				if (barrel.getItem() == null){
					barrel.resetTimer();
				}
			}
			
			player.inventory.onInventoryChanged();
			barrel.setLastClicked(STARTING_TIME);
			
			if (!world.isRemote){
				player.inventoryContainer.detectAndSendChanges();
			}
		}else{
			barrel.resetTimer();
		}
	}
	
	/**
	 * called when a player breaks the block.
	 * 
	 * @param the world the block has been broken in
	 * @param the player who has broken the block
	 * @param the x coordinate of the block
	 * @param the y coordinate of the block
	 * @param the z coordinate of the block
	 * @param the metadata of the block broken
	 * 
	 * @return void
	 */
	@Override
	public void breakBlock(World world, int x, int y, int z, int metadata, int blockid) {
		TileEntityBarrel tile;
		{
			TileEntity entity = world.getBlockTileEntity(x, y, z);
			if (entity == null || !(entity instanceof TileEntityBarrel)) return;
			tile = (TileEntityBarrel) entity;
		}
		if (!world.isRemote){
			
			this.spawnItem(world, x, y, z, tile.getItem(), null);
			world.func_96440_m(x, y, z, 0);
			
			if (world.getGameRules().getGameRuleBooleanValue("doTileDrops")){
				int frameCount = 0;
				for (int i = 0; i < tile.getFrames().length; i++){
					if (tile.getFrames()[i] != null){
						frameCount++;
					}
				}
				
				if (frameCount != 0){
					this.spawnItem(world, x, y, z, new ItemStack(Item.itemFrame, frameCount, 0), null);
				}
			}
		}
		
		world.removeBlockTileEntity(x, y, z);
	}
	
	
	
	/**
	 * The possible orientations the block can be in
	 */
	@Override
	public ForgeDirection[] getValidRotations(World world, int x, int y, int z) {
		return new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST };
	}
	
	/**
	 * called to rotate the block Used by things like buildcraft wrenches to orient the block so it faces the side hit by the wrench
	 * 
	 * @param the world the block is in
	 * @param the x coordinate the block is in
	 * @param the y coordinate the block is in
	 * @param the z coordinate the block is in
	 * @param the new orientation
	 * 
	 * @return if the rotation was successful
	 */
	@Override
	public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis) {
		//for some reason some mods don't give a shit about getValidRotations so I need to check it myself
		if (Arrays.binarySearch(this.getValidRotations(world, x, y, z), axis) == -1){
			return false;
		}
		
		TileEntityBarrel barrel = this.getTileEntity(world, x, y, z);
		
		if (barrel.getSide() == Direction.facingToDirection[axis.ordinal()]){
			return false;
		}
		
		barrel.setSide(Direction.facingToDirection[axis.ordinal()]);
		
		if (!world.isRemote){
			PacketHandler.getInstance().sendClientsBlockChange(barrel);
		}else{
			world.markBlockForRenderUpdate(x, y, z);
		}
		
		return true;
	}
	
	public ItemStack spawnItem (EntityPlayer player, ItemStack item){
		int currentSlot = player.inventory.currentItem;
		int inventorySize = player.inventory.mainInventory.length;
		
		int pass = 0;
		int slot = 0;
		
		while (item != null) {
			if (pass == 0){
				slot = currentSlot;
			}else if (slot == currentSlot){
				slot++;
			}
			
			ItemStack playerItem = player.inventory.getStackInSlot(slot);
			
			if (playerItem == null || playerItem.stackSize < 1){
				if (pass == 0 || pass == 2){
					if (item.stackSize > item.getMaxStackSize()){
						playerItem = item.copy();
						playerItem.stackSize = item.getMaxStackSize();
						item.stackSize -= playerItem.stackSize;
					}else{
						playerItem = item.copy();
						item = null;
					}
					
					playerItem.animationsToGo = 5;
				}
			}else if ((pass == 0 || pass == 1) && TileEntityBarrel.equals(playerItem, item)){
				
				playerItem = playerItem.copy();
				if (item.stackSize + playerItem.stackSize > item.getMaxStackSize()){
					int len = Math.min(item.stackSize, item.getMaxStackSize() - playerItem.stackSize);
					playerItem.stackSize += len;
					item.stackSize -= len;
				}else{
					playerItem.stackSize += item.stackSize;
					item = null;
				}
				
				playerItem.animationsToGo = 5;
			}
			
			player.inventory.setInventorySlotContents(slot, playerItem);
			
			if (pass == 0){
				pass = 1;
				slot = 0;
				continue;
			}
			
			if (slot >= inventorySize - 1){
				if (pass > 2){
					break;
				}else{
					slot = -1;
				}
				pass++;
			}
			slot++;
		}
		player.inventory.onInventoryChanged();
		
		return item;
	}
	
	/**
	 * spawns a ItemStack in the world
	 * NOTE: uses special algorithm to prevent an exploit.
	 * 
	 * @param world object to spawn the item in
	 * @param the x coordinate to spawn
	 * @param the y coordinate to spawn
	 * @param the z coordinate to spawn
	 * @param the ItemStack to spawn
	 * @param boolean to determine if the item should fly in a random direction
	 * 
	 * @return void
	 */
	public void spawnItem (World world, double x, double y, double z, ItemStack item, EntityPlayer player){
		if (item != null && !world.isRemote) {
			item = item.copy();
			
			if (player != null){
				item = spawnItem(player, item);
			}
			
			//if the items variable is not null, it will spawn them in the world
			if (item != null && !world.isRemote){
				int stackSize = (int) Math.ceil(item.stackSize / item.getMaxStackSize());
				int stacksToSpawn = (stackSize < 64) ? stackSize : 64;
				int itemsPerStack = (stackSize == 0) ? 0 : (int) (Math.ceil(item.stackSize / stacksToSpawn));
				int excess = (stackSize == 0) ? item.stackSize : item.stackSize % stacksToSpawn;
				
				item.stackSize = itemsPerStack;
				for (int spawn = 0; spawn < stacksToSpawn; spawn++){
					EntityItem entity = null;
					if (player == null){
						entity = new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, item.copy());
						entity.motionX = (this.random.nextInt(99) - 49) * 0.0015;
						entity.motionZ = (this.random.nextInt(99) - 49) * 0.0015;
						entity.delayBeforeCanPickup = 10;
					}else{
						entity = new EntityItem(world, x, y + 0.5, z, item.copy());
						entity.motionX = 0;
						entity.motionZ = 0;
					}
					world.spawnEntityInWorld(entity);
				}
				
				if (excess != 0){
					item.stackSize = excess;
					EntityItem entity = null;
					if (player == null){
						entity = new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, item.copy());
						entity.motionX = (this.random.nextInt(99) - 49) * 0.0015;
						entity.motionZ = (this.random.nextInt(99) - 49) * 0.0015;
						entity.delayBeforeCanPickup = 10;
					}else{
						entity = new EntityItem(world, x, y + 0.5, z, item.copy());
						entity.motionX = 0;
						entity.motionZ = 0;
					}
					world.spawnEntityInWorld(entity);
				}
			}
		}
	}
	
	/**
	 * Creates a tile entity associated with this block
	 * 
	 * @param the world the block wants to create a tile entity in
	 * 
	 * @return the new tile entity
	 */
	@Override
	public TileEntity createNewTileEntity(World world) {
		return new TileEntityBarrel();
	}
	
	/**
	 * the metadata of this block when broken
	 * 
	 * @param the metadata of the block
	 * 
	 * @return the metadata placed in the world
	 */
	@Override
	public int damageDropped(int metadata) {
		return metadata;
	}
	
	/**
	 * Called when the player picked a block in creative mode (default hey is middle mouse button)
	 * 
	 * @retirn the item stack picked
	 */
	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
		boolean pickedItemFrame = false;
		TileEntityBarrel barrel = this.getTileEntity(world, x, y, z);
		
		if (Direction.facingToDirection[target.sideHit] != -1 && barrel.getFrames()[Direction.facingToDirection[target.sideHit]] != null){
			Vec3 vector = target.hitVec;
			
			double xHit = vector.xCoord - Math.floor(vector.xCoord);
			double yHit = vector.yCoord - Math.floor(vector.yCoord);
			double zHit = vector.zCoord - Math.floor(vector.zCoord);
			
			pickedItemFrame = yHit > (1D / 16D) && yHit < (1D / 16D) * 13D && (xHit == 0 ? zHit : xHit)
					> (1D / 16D) * 2D && (xHit == 0 ? zHit : xHit) < (1D / 16D) * 14D;
		}
		
		if (pickedItemFrame){
			return new ItemStack(Item.itemFrame, 1, 0);
		}else{
			return new ItemStack(this.blockID, 1, world.getBlockMetadata(x, y, z));
		}
	}
	
	/**
	 * called to add the sub blocks of the block into the creative menu
	 * used by blocks with metadata
	 * 
	 * @param the blockID of the block being added
	 * @param the creative tab the block is getting added in
	 * @param the list the blocks are getting added to
	 * 
	 * @return void
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(int blockID, CreativeTabs tab, List list) {
		if (Barrels.instance.T1BarrelMaxStorage > 0)
			list.add(Barrels.instance.T1barrel);
		if (Barrels.instance.T2BarrelMaxStorage > 0)
			list.add(Barrels.instance.T2barrel);
		if (Barrels.instance.T3BarrelMaxStorage > 0)
			list.add(Barrels.instance.T3barrel);
	}
}