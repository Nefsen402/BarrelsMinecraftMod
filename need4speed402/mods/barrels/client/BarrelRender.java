package need4speed402.mods.barrels.client;

import static net.minecraftforge.client.IItemRenderer.ItemRenderType.ENTITY;
import static net.minecraftforge.client.IItemRenderer.ItemRendererHelper.ENTITY_ROTATION;
import need4speed402.mods.barrels.Barrels;
import need4speed402.mods.barrels.TileEntityBarrel;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureCompass;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Icon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BarrelRender extends TileEntitySpecialRenderer{
	private static final BarrelRender instance = new BarrelRender();

	private final TextureManager manager = Minecraft.getMinecraft().renderEngine;
	private final GameSettings settings = Minecraft.getMinecraft().gameSettings;
	private final Tessellator tessellator = Tessellator.instance;
	
	private final Render frameRender = ((Render) RenderManager.instance.entityRenderMap.get(EntityItemFrame.class));
	
	private final ResourceLocation itemGlint = new ResourceLocation("textures/misc/enchanted_item_glint.png");
	private final ResourceLocation mapBackround = new ResourceLocation("textures/map/map_background.png");
	
	private final DynamicTexture mapTexture = new DynamicTexture(128, 128);
	
	/** Variable used for rendering blocks displayed on the barrel */
	private final RenderBlocks blockRender = new RenderBlocks();
	
	public final int MAX_STRING_WIDTH = 90;
	public final String INFINITY = "\u221E";
	
	
	public static BarrelRender getInstance() {
		return instance;
	}
	
	//there should only be one BarrelRender instance
	private BarrelRender (){}
	
	/**
	 * renders the overlay
	 * 
	 * @param the text being rendered
	 * @param the side being rendered
	 * @param the x coordinate being rendered at
	 * @param the y coordinate being rendered at
	 * @param the z coordinate being rendered at
	 * 
	 * @return void
	 */
	private void renderText(String text, int side, double x, double y, double z) {
		if (text != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x + 0.5F, y + 0.91F, z + 0.5F);
			GL11.glRotatef(-90 * Direction.rotateOpposite[side], 0, 1, 0);
			GL11.glTranslatef(0, 0, -0.505F);
			
			int stringWidth = this.getFontRenderer().getStringWidth(text);
			if (stringWidth >= MAX_STRING_WIDTH){
				//text is about to go off the barrel
				text = INFINITY;
				stringWidth = this.getFontRenderer().getStringWidth(text);
				
				GL11.glTranslatef(0, 0.21F, 0);
				GL11.glScalef(0.04F, 0.04F, 1);
			}else{
				GL11.glScalef(0.01F, 0.01F, 1);
			}
			
			GL11.glRotatef(180, 0, 0, 1);
	
			this.getFontRenderer().drawString(text, -stringWidth / 2, 2, 0xFFFFFFFF);
			GL11.glPopMatrix();
		}
	}
	
	private void renderName(String text, int side, double x, double y, double z) {
		if (text != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x + 0.5F, y + 0.91F, z + 0.5F);
			GL11.glRotatef(-90 * Direction.rotateOpposite[side], 0, 1, 0);
			GL11.glTranslatef(0, 0, -0.505F);
			
			int stringWidth = this.getFontRenderer().getStringWidth(text);
			
			float scale = Math.min(1F / (float) (stringWidth + 10), 0.01F);
			GL11.glScalef(scale, scale, 1);
			
			GL11.glRotatef(180, 0, 0, 1);
	
			this.getFontRenderer().drawString(text, -stringWidth / 2, 2, 0xFFFFFFFF);
			GL11.glPopMatrix();
		}
	}
	
	/**
	 * renders the item
	 * 
	 * @param the ItemStack to be rendered
	 * @param the render manager used to render
	 * @param the side being rendered
	 * @param the x coordinate being rendered at
	 * @param the y coordinate being rendered at
	 * @param the z coordinate being rendered at
	 */
	public void renderItem(TileEntityBarrel tile, ItemStack stack, int side, double x, double y, double z) {
		GL11.glPushMatrix();
		Barrels.instance.renderIn3D = true;
		GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
		GL11.glRotatef(-90 * Direction.rotateOpposite[side], 0, 1, 0);
		GL11.glTranslatef(0, -1F / 16F, -0.5F);
		
		{
			float scale = 1F / 3.2F;
			GL11.glScalef(scale, scale, scale);
		}
		
		ItemRenderType type = Barrels.instance.renderIn3D ? ItemRenderType.ENTITY : ItemRenderType.INVENTORY;
		ItemRendererHelper helper = Barrels.instance.renderIn3D ? ItemRendererHelper.BLOCK_3D : ItemRendererHelper.INVENTORY_BLOCK;
		
		IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(stack, type);
		Item item = stack.getItem();
		
		{
			int color = item.getColorFromItemStack(stack, 0);
			float a = 1;//(float) (color >> 24 & 0xFF) / 255F;
			float r = (float) (color >> 16 & 0xFF) / 255F;
			float g = (float) (color >>  8 & 0xFF) / 255F;
			float b = (float) (color	   & 0xFF) / 255F;
			GL11.glColor4f(r, g, b, a);
		}
		
		if (customRenderer != null){
			manager.func_110577_a(TextureMap.field_110575_b);
			
			GL11.glTranslatef(0, 0, -0.005F);
			GL11.glScalef(2F, 2F, 2F);
			
			if (Barrels.instance.renderIn3D && !customRenderer.shouldUseRenderHelper(type, stack, ItemRendererHelper.ENTITY_BOBBING)){
				GL11.glTranslatef(0, -0.4F, 0);
			}
			
			
			if (customRenderer.shouldUseRenderHelper(type, stack, helper)){
				if(Barrels.instance.renderIn3D){
					GL11.glRotatef(90, 0, 1, 0);
				}else{
					GL11.glScalef(1, 1, 0.001F);
					
					GL11.glRotatef(180, 0, 0, 1);
					GL11.glRotatef(210, 1, 0, 0);
					GL11.glRotatef(-45, 0, 1, 0);
				}
			}
			
			RenderItem.renderInFrame = Barrels.instance.renderIn3D;
			blockRender.useInventoryTint = true;
			
			if (Barrels.instance.renderIn3D){
				EntityItem entity = new EntityItem(tile.worldObj, 0, 0, 0, stack);
				entity.hoverStart = 0;
				entity.age = 0;
				
				customRenderer.renderItem(type, stack, blockRender, entity);
			}else{
				customRenderer.renderItem(type, stack, blockRender);
			}
			
			RenderItem.renderInFrame = false;
			
			GL11.glDisable(GL11.GL_LIGHTING);
		}else if (stack.getItemSpriteNumber() == 0 && item instanceof ItemBlock && RenderBlocks.renderItemIn3d(Block.blocksList[item.itemID].getRenderType())){
			manager.func_110577_a(TextureMap.field_110575_b);
			
			GL11.glScalef(1.3F, 1.3F, 1.3F);
			
			//stops flickering commonly found in 16x16 texture packs
			GL11.glTranslatef(0, 0, -0.01F);
			
			if(Barrels.instance.renderIn3D){
				GL11.glRotatef(90, 0, 1, 0);
			}else{
				GL11.glScalef(1, 1, 0.001F);
				
				GL11.glRotatef(180, 0, 0, 1);
				GL11.glRotatef(210, 1, 0, 0);
				GL11.glRotatef(-45, 0, 1, 0);
			}
			
			blockRender.renderBlockAsItem(Block.blocksList[stack.itemID], stack.getItemDamage(), 1);
		}else{
			GL11.glEnable(GL12.GL_RESCALE_NORMAL);
			GL11.glScalef(1, 1, 0.125F);
			
			boolean reloadTexture = true;
			Icon def = stack.getIconIndex();
			
			if (def == null){
				def = ((TextureMap) manager.func_110581_b(TextureMap.field_110576_c)).func_110572_b("missingno");
			}
			
			for (int pass = 0; pass < (item.requiresMultipleRenderPasses() ? item.getRenderPasses(stack.getItemDamage()) : 1); pass++){
				if (reloadTexture){
					manager.func_110577_a(stack.getItem() == Item.map ? mapBackround : manager.func_130087_a(stack.getItemSpriteNumber()));
				}
				
				if (pass > 0){
					int color = item.getColorFromItemStack(stack, pass);
					float a = 1;//(float) (color >> 24 & 0xFF) / 255F;
					float r = (float) (color >> 16 & 255) / 255.0F;
					float g = (float) (color >> 8 & 255) / 255.0F;
					float b = (float) (color & 255) / 255.0F;
					GL11.glColor4f(r, g, b, a);
				}
				
				Icon icon = null;
				if (item.requiresMultipleRenderPasses()){
					icon = item.getIcon(stack, pass);
				}
				
				if (icon == null){
					icon = def;
				}
				
				if (stack.getItem() == Item.map){
					this.renderItem(0, 1, 0, 1, GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH), GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT));
					
					GL11.glScalef(0.9F, 0.9F, 1);
					
					mapTexture.func_110564_a();
					
					float offset = 0.6F;
					
					tessellator.startDrawingQuads();
					tessellator.addVertexWithUV(-1,  1, -offset, 1, 0);
					tessellator.addVertexWithUV( 1,  1, -offset, 0, 0);
					tessellator.addVertexWithUV( 1, -1, -offset, 0, 1);
					tessellator.addVertexWithUV(-1, -1, -offset, 1, 1);
					tessellator.draw();
				}else if (stack.getItem() == Item.compass){
					TextureAtlasSprite texture = ((TextureMap) manager.func_110581_b(TextureMap.field_110576_c)).func_110572_b(Item.compass.getIconIndex(stack).getIconName());
					
					if (texture != null && texture instanceof TextureCompass){
						TextureCompass compass = (TextureCompass) texture;
						double angle = compass.currentAngle;
						double delta = compass.angleDelta;
						compass.currentAngle = 0.0D;
						compass.angleDelta = 0.0D;
						compass.updateCompass(tile.worldObj, tile.xCoord, tile.xCoord, side * 90 + 220, false, true);
						
						renderItem(icon.getMinU(), icon.getMaxU(), icon.getMinV(), icon.getMaxV(), icon.getOriginX(), icon.getOriginY());
						
						compass.currentAngle = angle;
						compass.angleDelta = delta;
						compass.updateAnimation();
					}else{
						renderItem(icon.getMinU(), icon.getMaxU(), icon.getMinV(), icon.getMaxV(), icon.getOriginX(), icon.getOriginY());
					}
					
				}else{
					this.renderItem(icon.getMinU(), icon.getMaxU(), icon.getMinV(), icon.getMaxV(), icon.getOriginX(), icon.getOriginY());
				}
				
				//Renders glimmer effect on some items
				if (reloadTexture = stack.hasEffect(pass)){
					GL11.glPushMatrix();
					manager.func_110577_a(itemGlint);
					
					GL11.glDepthFunc(GL11.GL_EQUAL);
					GL11.glEnable(GL11.GL_BLEND);
					
					GL11.glColor3f(0.5F, 0.25F, 0.8F);
					
					GL11.glMatrixMode(GL11.GL_TEXTURE);
					GL11.glDepthMask(false);
					GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
					
					for (int dir = 0; dir < 2; dir++){
						GL11.glPushMatrix();
						float phrase = (float) (System.currentTimeMillis() / 2 % (3000 + dir * 1873)) / (3000 + dir * 1873) * 256;
						float uoffset = dir == 1 ? -1 : 4;
						
						/*
						 * I want to tune this up later so here is a referance that will help me:
						 * 
						 * u1 	(phrase + 20 * offset) * 0.00390625
						 * u2	(phrase + 20 + 20 * offset) * 0.00390625
						 * u3	(phrase + 20) * 0.00390625
						 * u4	phrase * 0.00390625
						 * 
						 * v1	phrase * 0.00390625
						 * v2	phrase * 0.00390625
						 * v3	0
						 * v4	0
						 */
						
						float minU;
						float maxU;
						float maxV;
						float minV;
						
						if (dir == 0){
							minU = (float) (phrase + 20 * uoffset) * 0.00390625F;
							maxU = (float) (phrase + 20 + 20 * uoffset) * 0.00390625F;
							maxV = 1;
							minV = 0;
						}else{
							GL11.glRotatef(90, 0, 0, 1);
							
							minV = (float) (phrase + 20 * uoffset) * 0.00390625F;
							maxV = (float) (phrase + 20 + 20 * uoffset) * 0.00390625F;
							maxU = 1;
							minU = 0;
						}
						
						renderItem(minU, maxU, minV, maxV, icon.getOriginX(), icon.getOriginY());
						GL11.glPopMatrix();
					}
					
					GL11.glDepthMask(true);
					GL11.glMatrixMode(GL11.GL_MODELVIEW);
					GL11.glDisable(GL11.GL_BLEND);
					GL11.glDepthFunc(GL11.GL_LEQUAL);
					
					GL11.glPopMatrix();
				}
			}
			
			GL11.glDisable(GL12.GL_RESCALE_NORMAL);
			
			//renders item damage bar
			if (stack.isItemDamaged()){
				GL11.glDisable(GL11.GL_TEXTURE_2D);
				int damage = (int) Math.round(13.0D - (double) stack.getItemDamageForDisplay() * 13.0D / (double) stack.getMaxDamage());
				int color = (int) Math.round(255.0D - (double) stack.getItemDamageForDisplay() * 255.0D / (double) stack.getMaxDamage());
				
				tessellator.startDrawingQuads();
				{
					float offset = 0.001F;
					
					if (damage > 0){
						this.renderQuad(-1F / 8F * (damage - 6) - (damage < 13 ? 0 : offset), -1F / 8F * 6, 1F / 8F * damage + (damage < 13 ? offset : offset * 2), 1F / 8F + offset, offset + 0.15F, 255 - color << 16 | color << 8);
					}
					
					if (damage < 13){
						if (damage < 12){
							this.renderQuad(-1F / 8F * 6, -1F / 8F * 6, 1F / 8F * (13 - damage - 1), 1F / 8F + offset, offset + 0.15F, (255 - color) / 4 << 16 | 16128);
						}
						
						this.renderQuad(-1F / 8F * 7 - offset, -1F / 8F * 6, 1F / 8F + offset, 1F / 8F + offset, offset + 0.15F, 0);
					}
					this.renderQuad(-1F / 8F * 7 - offset, -1F / 8F * 7 - offset, 1F / 8F * 13 + offset * 2, 1F / 8F + offset, offset + 0.15F, 0);
				}
				tessellator.draw();
				
				GL11.glEnable(GL11.GL_TEXTURE_2D);
			}
		}
		
		GL11.glPopMatrix();
	}
	
	/**
	 * Renderes a icon.
	 * 
	 * @param the icon that should be rendered 
	 * 
	 * @return void
	 */
	private void renderItem(float minU, float maxU, float minV, float maxV, int originX, int originY){
		if (settings.fancyGraphics){
			float offset = 0.5F;
			
			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-1,  1, -offset, maxU, minV);
			tessellator.addVertexWithUV( 1,  1, -offset, minU, minV);
			tessellator.addVertexWithUV( 1, -1, -offset, minU, maxV);
			tessellator.addVertexWithUV(-1, -1, -offset, maxU, maxV);
			tessellator.draw();
			float width = (maxU - minU) / (float) originX;
			float height = (maxV - minV) / (float) originY;
			
			//indents to prevent artifacting
			float indent = 0.0001F;
			tessellator.startDrawingQuads();
			for (int i = 0; i < originX; i++){
				tessellator.addVertexWithUV((float) i / (float) originX * 2 - 1,  1,       0, (maxU - width * (float) i) - indent, minV);
				tessellator.addVertexWithUV((float) i / (float) originX * 2 - 1,  1, -offset, (maxU - width * (float) i) - indent, minV);
				tessellator.addVertexWithUV((float) i / (float) originX * 2 - 1, -1, -offset, (maxU - width * (float) i) - indent, maxV);
				tessellator.addVertexWithUV((float) i / (float) originX * 2 - 1, -1,       0, (maxU - width * (float) i) - indent, maxV);
			}
			tessellator.draw();
			
			tessellator.startDrawingQuads();
			for (int i = 1; i <= originX; i++){
				tessellator.addVertexWithUV((float) i / (float) originX * 2 - 1,  1, -offset, (maxU - width * (float) i) + indent, minV);
				tessellator.addVertexWithUV((float) i / (float) originX * 2 - 1,  1,       0, (maxU - width * (float) i) + indent, minV);
				tessellator.addVertexWithUV((float) i / (float) originX * 2 - 1, -1,       0, (maxU - width * (float) i) + indent, maxV);
				tessellator.addVertexWithUV((float) i / (float) originX * 2 - 1, -1, -offset, (maxU - width * (float) i) + indent, maxV);
			}
			tessellator.draw();
			
			tessellator.startDrawingQuads();
			for (int i = 0; i < originY; i++){
				tessellator.addVertexWithUV(-1, (float) i / (float) originY * 2 - 1, -offset, maxU, (maxV - height * (float) i) - indent);
				tessellator.addVertexWithUV( 1, (float) i / (float) originY * 2 - 1, -offset, minU, (maxV - height * (float) i) - indent);
				tessellator.addVertexWithUV( 1, (float) i / (float) originY * 2 - 1,       0, minU, (maxV - height * (float) i) - indent);
				tessellator.addVertexWithUV(-1, (float) i / (float) originY * 2 - 1,       0, maxU, (maxV - height * (float) i) - indent);
			}
			tessellator.draw();
			
			tessellator.startDrawingQuads();
			for (int i = 1; i <= originY; i++){
				tessellator.addVertexWithUV(-1, (float) i / (float) originY * 2 - 1,       0, maxU, (maxV - height * (float) i) + indent);
				tessellator.addVertexWithUV( 1, (float) i / (float) originY * 2 - 1,       0, minU, (maxV - height * (float) i) + indent);
				tessellator.addVertexWithUV( 1, (float) i / (float) originY * 2 - 1, -offset, minU, (maxV - height * (float) i) + indent);
				tessellator.addVertexWithUV(-1, (float) i / (float) originY * 2 - 1, -offset, maxU, (maxV - height * (float) i) + indent);
			}
			tessellator.draw();
		}else{
			float offset = -0.1F;
			
			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV(-1,  1, offset, maxU, minV);
			tessellator.addVertexWithUV( 1,  1, offset, minU, minV);
			tessellator.addVertexWithUV( 1, -1, offset, minU, maxV);
			tessellator.addVertexWithUV(-1, -1, offset, maxU, maxV);
			tessellator.draw();
		}
	}
	
	/**
	 * called when a player switches to a different dimension
	 * 
	 * @param the world the player switched to
	 * 
	 * @return void
	 */
	@Override
	public void onWorldChange(World world) {
		this.blockRender.blockAccess = world;
	}
	
	/**
	 * renders a quad (a colored square)
	 * 
	 * @param the x coordinate being rendered at
	 * @param the y coordinate being rendered at
	 * @param the width of the square being drawn
	 * @param the height of the square being drawn
	 * @param the color of the square being drawn
	 */
	private void renderQuad(float x, float y, float width, float height, float offset, int color){
		tessellator.setColorOpaque(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF);
		if (settings.fancyGraphics){
			offset += 0.5F;
			
			tessellator.addVertex(        x,          y, -offset);
			tessellator.addVertex(        x, y + height, -offset);
			tessellator.addVertex(x + width, y + height, -offset);
			tessellator.addVertex(x + width,          y, -offset);
			
			tessellator.addVertex(        x, y + height, -offset);
			tessellator.addVertex(        x, y + height,       0);
			tessellator.addVertex(x + width, y + height,       0);
			tessellator.addVertex(x + width, y + height, -offset);
			
			tessellator.addVertex(x + width, y, -offset);
			tessellator.addVertex(x + width, y,       0);
			tessellator.addVertex(        x, y,	      0);
			tessellator.addVertex(        x, y, -offset);
			
			tessellator.addVertex(x + width, y + height, -offset);
			tessellator.addVertex(x + width, y + height,       0);
			tessellator.addVertex(x + width,          y,       0);
			tessellator.addVertex(x + width,          y, -offset);
			
			tessellator.addVertex(x,          y, -offset);
			tessellator.addVertex(x,          y,       0);
			tessellator.addVertex(x, y + height,       0);
			tessellator.addVertex(x, y + height, -offset);
		}else{
			tessellator.addVertex(        x,          y, -offset);
			tessellator.addVertex(        x, y + height, -offset);
			tessellator.addVertex(x + width, y + height, -offset);
			tessellator.addVertex(x + width,          y, -offset);
		}
	}
	
	/**
	 * Called every frame (not tick) for rendering
	 * 
	 * @param the tile entity being rendered
	 * @param the x coordinate to render at
	 * @param the y coordinate to render at
	 * @param the z coordinate to render at
	 * @param the counter to the next frame
	 * 
	 * @return void
	 */
	@Override
	public void renderTileEntityAt(TileEntity entity, double x, double y, double z, float counter) {
		TileEntityBarrel tile = (TileEntityBarrel) entity;
		ItemStack item = tile.getItem();
		
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
		
		if (item != null){
			item = item.copy();
			item.stackSize = 1;
			
			if (item.getItem() instanceof ItemMap){
				MapData data = Item.map.getMapData(item, tile.worldObj);
				
				//sets the textureManager for the map to ready it for rendering
				if (data != null){
					for (int i = 0; i < data.colors.length; i++){
						byte color = data.colors[i];
		
						if (color / 4 == 0){
							mapTexture.func_110565_c()[i] = ((i * 2) / 128 & 1) * 8 + 16 << 24;
						}else{
							short miltiplyer = 0;
							switch (color & 3){
							case 0:
								miltiplyer = 180;
								break;
							case 2:
								miltiplyer = 255;
								break;
							default:
								miltiplyer = 220;
							}
		
							int mapColor = MapColor.mapColorArray[color / 4].colorValue;
							int r = (mapColor >> 16 & 0xFF) * miltiplyer / 0xFF;
							int g = (mapColor >>  8 & 0xFF) * miltiplyer / 0xFF;
							int b = (mapColor       & 0xFF) * miltiplyer / 0xFF;
		
							if (settings.anaglyph){
								r = (r * 30 + g * 59 + b * 11) / 100;
								g = (r * 30 + g * 70         ) / 100;
								b = (r * 30 + b * 70         ) / 100;
							}
							
							//writes final color to pixel buffer
							mapTexture.func_110565_c()[i] = 0xFF000000 | r << 16 | g << 8 | b;
						}
					}
				}
			}
		}
		
		if (Barrels.instance.onlyRenderOneSide){
			int side = tile.getSide();
			if (canRender(tile, side)){
				renderEntityOnSide(tile, item, side, x, y, z);
			}
			
			for (side = 0; side < 4; side++){
				if (side != tile.getSide() && canRender(tile, side) && tile.getFrames()[side] != null){
					renderEntityOnSide(tile, item, side, x, y, z);
				}
			}
		}else{
			for (byte side = 0; side < 4; side++){
				if (canRender(tile, side)){
					renderEntityOnSide(tile, item, side, x, y, z);
				}
			}
		}
		
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_LIGHTING);
	}
	
	private boolean canRender(TileEntityBarrel tile, int side){
		return tile.getBlockType().shouldSideBeRendered(tile.worldObj, tile.xCoord + Direction.offsetX[side], tile.yCoord, tile.zCoord + Direction.offsetZ[side], tile.getBlockMetadata());
	}
	
	private void renderEntityOnSide(TileEntityBarrel tile, ItemStack item, int side, double x, double y, double z){
		this.setLight(tile, side);
		
		boolean render = item != null;
		
		if (tile.getFrames()[side] != null){
			if (render){
				this.renderText(tile.getOverlay(), side, x, y, z);
			}
			
			GL11.glPushMatrix();
			GL11.glTranslated(x + 0.5F, y + 1F / 16F * 7F, z + 0.5F);
			
			GL11.glRotatef(-90 * (side - 1), 0, 1, 0);
			GL11.glTranslatef(-0.5F - 1F / 16F, 0, 0);
			GL11.glRotatef(90 * (side - 1), 0, 1, 0);
			
			GL11.glEnable(GL11.GL_LIGHTING);
			
			frameRender.doRender(tile.getFrames()[side], 0, 0, 0, 0, 0);
			
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glPopMatrix();
		}else if (render){
			this.renderText(tile.getOverlay(), side, x, y, z);
			this.renderItem(tile, item, side, x, y, z);
			
			if (Barrels.instance.renderName){
				this.renderName(item.getDisplayName(), side, x, y - 0.8F, z);
			}
		}
	}
	
	/**
	 * sets the light conditions
	 * @param the tile entity being renderd
	 * @param the side being rendered
	 * 
	 * @return void
	 */
	private void setLight(TileEntity tile, int side) {
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 
				tile.worldObj.getSkyBlockTypeBrightness(EnumSkyBlock.Block, tile.xCoord + Direction.offsetX[side], tile.yCoord, tile.zCoord + Direction.offsetZ[side]) * 15,
				tile.worldObj.getSkyBlockTypeBrightness(EnumSkyBlock.Sky, tile.xCoord + Direction.offsetX[side], tile.yCoord, tile.zCoord +Direction.offsetZ[side]) * 15);
	}
}