package need4speed402.mods.barrels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import need4speed402.mods.barrels.proxy.Proxy;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;

@NetworkMod(clientSideRequired = true, serverSideRequired = false, channels = { PacketHandler.NETWORK_CHANNEL }, packetHandler = PacketHandler.class )
@Mod(modid = "barrels", name = "The Barrels Mod", version = "3.0")
public class Barrels {
	@Instance("barrels")
	public static Barrels instance;
	
	/** the proxy */
	@SidedProxy(serverSide = "need4speed402.mods.barrels.proxy.ProxyServer", clientSide = "need4speed402.mods.barrels.proxy.ProxyClient")
	public static Proxy proxy;

	public BlockBarrel barrel;
	public ItemStack T1barrel;
	public ItemStack T2barrel;
	public ItemStack T3barrel;
	
	/** max storage of the barrels; measured in stacks */
	public int T1BarrelMaxStorage                 =                64;
	public int T2BarrelMaxStorage                 =              1024;
	public int T3BarrelMaxStorage                 =              4096;

	/** crafting recipe codes */
	public String T1barrelCrafting                = "shaped;logWood;logWood;logWood;item,ingotIron;nothing;item,ingotIron;logWood;logWood;logWood";
	public String T2barrelCrafting                = "shaped;item,blazeRod;block,obsidian;item,blazeRod;item,enderPearl;barrel,T1barrel;item,enderPearl;item,blazeRod;block,obsidian;item,blazeRod";
	public String T3barrelCrafting                = "shaped;block,obsidian;item,blazeRod;block,obsidian;item,diamond;barrel,T2barrel;item,diamond;block,obsidian;item,blazeRod;block,obsidian";
	public final HashMap<String, String> prefix = new HashMap<String, String>(), suffix = new HashMap<String, String>();
	
	public boolean onlyRenderOneSide              =             false;
	public boolean renderIn3D                     =              true;
	public boolean renderName                     =             false;
	
	public boolean leaveOneItem                   =             false;
	
	public final byte[] interaction               =       new byte[6];
	
	/**
	 * used for reading/writing/creating the configuration file and to register
	 * the barrel render
	 * 
	 * @param the event of this initialization phrase
	 * 
	 * @return void
	 */
	@EventHandler
	public void preLoad(FMLPreInitializationEvent event) {
		instance = this;
		
		Configuration cfg = new Configuration(event.getSuggestedConfigurationFile());

		try {
			{
				int id = cfg.get(Configuration.CATEGORY_BLOCK, "Barrel", 4000, "The block ID for barrels.").getInt();
				
				if (id > Block.blocksList.length){ 
					throw new IndexOutOfBoundsException ("Block id too big! Must be below " + Block.blocksList.length + "!");
				}else if (id <= 0){
					throw new IndexOutOfBoundsException ("Block id too small! Must be above 0!");
				}else if (Block.blocksList[id] != null){
					int found = 0;
					
					for (int i = (id == 0 ? 1 : id) ; i < Block.blocksList.length; i++){
						if (Block.blocksList[i] == null){
							found = i;
						}
					}
					
					if (found == 0){
						for (int i = id - 1; i > 0; i--){
							if (Block.blocksList[i] == null){
								found = i;
							}
						}
					}
					
					if (found != 0){
					throw new IllegalArgumentException("The default block-id is already occupied.\nI found a free one but there is not gurantee that this will acually work: "
							+ Integer.toString(found) + "\nYou will need to change the block-id in the configuration file to this one manually");
					}else{
						throw new IllegalArgumentException("The default blockid is already occupied");
					}
				}
				
				barrel = new BlockBarrel(id);
			}

			this.T1barrel = new ItemStack(barrel, 1, 0);
			this.T2barrel = new ItemStack(barrel, 1, 1);
			this.T3barrel = new ItemStack(barrel, 1, 2);

			this.T1BarrelMaxStorage = cfg.get("BarrelStorage", "T1BarrelMaxStorage", this.T1BarrelMaxStorage,
					"The maximum stacks of items that a barrel can hold." +
					"\n Set any value to -1 and that type of barrel will be disabled.").getInt();

			T2BarrelMaxStorage = cfg.get("BarrelStorage", "T2BarrelMaxStorage", T2BarrelMaxStorage).getInt();
			T3BarrelMaxStorage = cfg.get("BarrelStorage", "T3BarrelMaxStorage", T3BarrelMaxStorage).getInt();
			
			{
				String catagory = "render";
				this.onlyRenderOneSide = true; //cfg.get(catagory, "onlyRenderOneSide", this.onlyRenderOneSide).getBoolean(this.onlyRenderOneSide);
				this.renderIn3D = cfg.get(catagory, "renderIn3D", this.renderIn3D).getBoolean(this.renderIn3D);
				this.renderName = cfg.get(catagory, "renderName", this.renderName).getBoolean(this.renderName);
			}
			
			{
				String[] modes = cfg.get("interaction", "interaction", "in/out;in/out;unused;unused;unused;unused", 
						"How the barrels interact with outside sources (buildcraft pipes)" +
						"\n Usage: can be in four states: in, out, in/out, unused" +
						"\n Formatting: <up>;<down>;<back>;<forward>;<right>;<left>").getString().split(";");
				
				for (int index = 0; index < modes.length; index++){
					byte mode = 0;
					if (modes[index].equals("in")) mode = 1;
					else if (modes[index].equals("out")) mode = 2;
					else if (modes[index].equals("in/out")) mode = 3;
					
					this.interaction[index] = mode;
				}
			}
			this.leaveOneItem = cfg.get("interaction", "leaveOneItem", this.leaveOneItem).getBoolean(this.leaveOneItem);
			
			this.T1barrelCrafting = cfg.get("crafting", "T1barrelCrafting", T1barrelCrafting, 
					"This allowes you to chage the crafting recipeis." +
					"\n Look on the forum for tutorials on setting this up.").getString();
			this.T2barrelCrafting = cfg.get("crafting", "T2barrelCrafting", T2barrelCrafting).getString();
			this.T3barrelCrafting = cfg.get("crafting", "T3barrelCrafting", T3barrelCrafting).getString();

		} catch (Exception e) {
			throw new RuntimeException("There has been a problem with the initialization of Barrels!", e);
		} finally {
			cfg.save();
		}
		
		proxy.preInit();
	}
	
	/**
	 * loads language files and registers the barrels
	 * 
	 * @param the event of this initialization phrase
	 * 
	 * @return void
	 */
	@EventHandler
	public void load(FMLInitializationEvent event) {
		TileEntity.addMapping(TileEntityBarrel.class, "tileBarrels");
		GameRegistry.registerBlock(barrel, ItemBarrel.class, "block.barrel", "barrels");
		
		proxy.init();
	}
	
	/**
	 * Loads recipes
	 * 
	 * @param the event of this initialization phrase
	 * 
	 * @return void
	 */
	@EventHandler
	public void postLoad(FMLPostInitializationEvent event) throws Exception{
		if (this.T1BarrelMaxStorage > 0)
			this.addRecipe(this.T1barrel, this.T1barrelCrafting);
		if (this.T2BarrelMaxStorage > 0)
			this.addRecipe(this.T2barrel, this.T2barrelCrafting);
		if (this.T3BarrelMaxStorage > 0)
			this.addRecipe(this.T3barrel, this.T3barrelCrafting);
		
		proxy.postInit();
	}

	/**
	 * adds a recipe using a special code structure.
	 * 
	 * it first determines how big the recipe is, then
	 * adds each part of the recipe into a ArrayList.
	 * 
	 * @param result
	 * @param code
	 */
	public void addRecipe(ItemStack result, String code) {
		final String nothingSymbol = "nothing"; //I wish I wasen't so stupid when I was writing this because now I would make the nothing symbol nothing ("")
		
		try {
			if (code == null || code.length() == 0)
				return;
			String[] element = code.split(";");
			if (element.length != 10) throw new IllegalArgumentException("Insuficent information");
			ArrayList<Object> list = new ArrayList<Object>();

			byte startY = 0;
			byte endY = 3;

			byte startX = 0;
			byte endX = 3;

			{
				boolean started = true;
				boolean ended = true;

				for (byte i = 0; i < 3; i++) {
					if (started && element[i * 3 + 1].equals(nothingSymbol)
							&& element[i * 3 + 2].equals(nothingSymbol)
							&& element[i * 3 + 3].equals(nothingSymbol)) {
						startY++;
					} else
						started = false;

					if (ended && element[9 - i * 3].equals(nothingSymbol)
							&& element[8 - i * 3].equals(nothingSymbol)
							&& element[7 - i * 3].equals(nothingSymbol)) {
						endY--;
					} else
						ended = false;
				}
			}

			{
				boolean started = true;
				boolean ended = true;
				
				for (int i = 0; i < 3; i++) {
					if (started && element[i + 1].equals(nothingSymbol)
							&& element[i + 4].equals(nothingSymbol)
							&& element[i + 7].equals(nothingSymbol)) {
						startX++;
					} else
						started = false;

					if (ended && element[3 - i].equals(nothingSymbol)
							&& element[6 - i].equals(nothingSymbol)
							&& element[9 - i].equals(nothingSymbol)) {
						endX--;
					} else
						ended = false;
				}
			}
			boolean isShapeless = element[0].equals("shapeless");
			for (int i = startY; i < endY; i++) {
				if (!isShapeless)
					list.add(i - startY, "");
				for (int ii = startX; ii < endX; ii++) {
					int index = i * 3 + ii + 1;
					boolean isNothing = element[index].equals(nothingSymbol);
					if (!isShapeless) {
						String string = null;
						if (isNothing) {
							string = " ";
						} else {
							string = Integer.toString(index);
						}
						if (i - startY == 0) {
							list.set(0, list.get(0) + string);
						} else if (i - startY == 1) {
							list.set(1, list.get(1) + string);
						} else if (i - startY == 2) {
							list.set(2, list.get(2) + string);
						}
					}
					if (isNothing)
						continue;

					if (!isShapeless){
						list.add(Character.valueOf(Integer.toString(index).charAt(0)));
					}

					Object crafting = null;

					if (element[index].contains(",")) {
						String[] subList = element[index].split(",");
						if (subList[0].equals("block")) {
							for (Block block : Block.blocksList) {
								if (block != null && block.getUnlocalizedName() != null && block.getUnlocalizedName().equals("tile." + subList[1])) {
									crafting = new ItemStack(block, 1, (subList.length == 3) ? Integer.parseInt(subList[2]) : 0);
									break;
								}
							}
						} else if (subList[0].equals("item")) {
							for (Item item : Item.itemsList) {
								if (item != null && item.getUnlocalizedName() != null && item.getUnlocalizedName().equals("item." + subList[1])) {
									crafting = new ItemStack(item, 1, (subList.length == 3) ? Integer.parseInt(subList[2]) : 0);
									break;
								}
							}
						} else if (subList[0].equals("barrel")) {
							crafting = this.getClass().getField(subList[1]).get(this);
						} else if (subList[0].equals("blockid") || subList[0].equals("itemid")) {
							crafting = new ItemStack(Integer.parseInt(subList[1]), 1, subList.length == 3 ? Integer.parseInt(subList[2]) : 0);
						}else{
							crafting = Class.forName(subList[0]).getField(subList[1]).get(null);
							if (!(crafting instanceof ItemStack) && !(crafting instanceof String)) {
								if (crafting instanceof Block) {
									crafting = new ItemStack((Block) crafting, 1, (subList.length == 3) ? Integer.parseInt(subList[2]) : 0);
								} else if (crafting instanceof Item) {
									crafting = new ItemStack((Item) crafting, 1, (subList.length == 3) ? Integer.parseInt(subList[2]) : 0);
								} else {
									throw new IllegalArgumentException("Wrong type");
								}
							}
						}
					} else {
						crafting = element[index];
					}

					list.add(crafting);
				}
			}

			if (element[0].equals("shaped")) {
				GameRegistry.addRecipe(new ShapedOreRecipe(result, list.toArray()));
			} else if (isShapeless) {
				GameRegistry.addRecipe(new ShapelessOreRecipe(result, list.toArray()));
			} else {
				throw new IllegalArgumentException("Type of crafting has not been specified.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.toString() + ": There was a problem while adding a recipe. Plase make sure your configuratons are correct.");
		}
	}
}