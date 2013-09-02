package need4speed402.mods.barrels;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ItemBarrel extends ItemBlock{

	public ItemBarrel(int id) {
		super(id);
		this.setHasSubtypes(true);
		this.setUnlocalizedName("barrel");
	}
	
	/**
	 * gets the metadata of the block
	 * 
	 * @param the metadata of the block
	 * 
	 * @return the metadata placed in the world
	 */
	@Override
	public int getMetadata(int metadata) {
		return metadata;
	}
	
	/**
	 * gets the name of the barrel that is not translated. Used to
	 * translate later
	 * 
	 * @param the item to get the unlocolized name from
	 * 
	 * @return the unlocolized name
	 */
	@Override
	public String getUnlocalizedName(ItemStack item) {
		return this.getUnlocalizedName() + "T" + Integer.toString(item.getItemDamage() + 1);
	}
	
	/**
	 * adds info the the item like "Holds 64 stacks."
	 */
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List info, boolean flag) {
		int compacity;
		
		switch (stack.getItemDamage()){
			case 0:
				compacity = Barrels.instance.T1BarrelMaxStorage;
				break;
			case 1:
				compacity = Barrels.instance.T2BarrelMaxStorage;
				break;
			case 2:
				compacity = Barrels.instance.T3BarrelMaxStorage;
				break;
			default:
				compacity = -1;	
		}
		
		String language = Minecraft.getMinecraft().gameSettings.language;
		
		if (!Barrels.instance.prefix.containsKey(language) || !Barrels.instance.suffix.containsKey(language)){
			language = "en_US";
		}
		
		info.add(Barrels.instance.prefix.get(language) + " " + Integer.toString(compacity) + " " + Barrels.instance.suffix.get(language));
	}
}