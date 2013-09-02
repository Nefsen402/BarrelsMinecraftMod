package need4speed402.mods.barrels;

import net.minecraft.entity.DataWatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

/**
 * This is a custom itemFrameEntity that ignores the data watcher and uses a local ItemStack
 * 
 * @author Alex
 *
 */
public class EntityItemFrame extends net.minecraft.entity.item.EntityItemFrame{
	ItemStack item;
	
	public EntityItemFrame(net.minecraft.entity.item.EntityItemFrame frame) {
		super(frame.worldObj, frame.xPosition, frame.yPosition, frame.zPosition, frame.hangingDirection);
		
		lastTickPosX = prevPosX = posX = frame.posX;
		lastTickPosY = prevPosY = posY = frame.posY;
		lastTickPosZ = prevPosZ = posZ = frame.posZ;
	}
	
	public EntityItemFrame(World world, int x, int y, int z, int rotation) {
		super(world, x, y, z, rotation);
		float x1 = x + Direction.offsetX[rotation];
		float y1 = (float) y + 0.5F;
		float z1 = z + Direction.offsetZ[rotation];
		
		if (rotation == 0){
			x1 += 0.5F;
			z1 += 0.0625F;
		}else if (rotation == 1){
			x1 += 0.9375F;
			z1 += 0.5F;
		}else if (rotation == 2){
			x1 += 0.5F;
			z1 += 0.9375F;
		}else{
			x1 += 0.0625F;
			z1 += 0.5F;
		}
		
		lastTickPosX = prevPosX = posX = x1;
		lastTickPosY = prevPosY = posY = y1;
		lastTickPosZ = prevPosZ = posZ = z1;
	}
	
	@Override
	public ItemStack getDisplayedItem() {
		return item;
	}
	
	@Override
	public void setDisplayedItem(ItemStack stack) {
		item = stack;
	}
	
	@Override
	public DataWatcher getDataWatcher() {
		return null;
	}
	
	@Override
	public int getRotation() {
		return 0;
	}
	
	@Override
	public void setItemRotation(int rotation) {}
	
	@Override
	protected void entityInit() {}
}