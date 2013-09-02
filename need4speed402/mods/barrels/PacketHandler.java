package need4speed402.mods.barrels;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

/**
 * class that handles all the network traffic 'The Barrels Mod' generates.
 * 
 * Compression algorithm is made by Alex Orzechoweski
 * 
 * I DARE you to make it compress any better
 * 
 * If you want to accept the challenge, you probably will need to redesign the compression algorithm because
 * the current one already uses every bit available to it.
 * 
 * eh, FUCK readability, you're going to need to have to understand it like i still try to do today
 */
public class PacketHandler implements IPacketHandler{
	/** the instance of the packet handler */
	private static PacketHandler instance;
	
	/**
	 * returns the instance the packet handler
	 * 
	 * @return the instance
	 */
	public static PacketHandler getInstance() {
		return instance;
	}
	
	//minecraft-forge makes the instance
	public PacketHandler (){
		instance = this;
	}
	
	/** The network channel used for sending packets */
	public final static String NETWORK_CHANNEL = "barrels";
	
	/**
	 * Sends the client the contents of the barrel
	 * this will only send it to the players in
	 * a specific dimension
	 * 
	 * @param the item being sent
	 * @param the x position of the tileEntity getting sent
	 * @param the y position of the tileEntity getting sent
	 * @param the z position of the tileEntity getting sent
	 * @param the dimension id the packet made is being sent to
	 * 
	 * @return void
	 * 
	 * @throws IOException
	 */
	public void updateClients (TileEntityBarrel barrel, boolean sendAdditional){
		try{
			sendPacket(barrel.xCoord, barrel.zCoord, barrel.worldObj.provider.dimensionId,
			encode(barrel.xCoord, barrel.yCoord, barrel.zCoord, barrel.getRawItem(), barrel.getSide(), barrel.getFrames(), sendAdditional, null), null);
		}catch (IOException e){
			throw new RuntimeException("Someting that sould never happen happens", e);
		}
	}
	
	/**
	 * sends the contents of a barrel to the clients
	 * 
	 * @param the item being sent
	 * @param the x position of the tileEntity getting updated
	 * @param the y position of the tileEntity getting updated
	 * @param the z position of the tileEntity getting updated
	 * @param the side the tileEntity is facing
	 * @param the player who requested the contents null if the server is sending the items to everybody
	 * 
	 * @return void
	 * 
	 * @throws IOException
	 */
	public void updateClient (TileEntityBarrel barrel, Player client) throws IOException{
		try{
			if (client instanceof EntityPlayerMP){
				EntityPlayerMP player = (EntityPlayerMP) client;
				
				player.playerNetServerHandler.sendPacketToPlayer(new Packet250CustomPayload(NETWORK_CHANNEL,
				encode(barrel.xCoord, barrel.yCoord, barrel.zCoord, barrel.getRawItem(), barrel.getSide(), barrel.getFrames(), true,
				barrel.getRawItem() != null && barrel.getRawItem().getItem() instanceof ItemMap ? Item.map.getMapData(barrel.getRawItem(), barrel.worldObj) : null)));
			}else{
				throw new IllegalArgumentException("Client must be a instance of EntityPlayerMP");
			}
		}catch (IOException e){
			throw new RuntimeException("Someting that sould never happen happens", e);
		}
	}
	
	/**
	 * serializes the info given
	 * 
	 * @return the serialized info
	 * 
	 * @throws IOException
	 */
	private byte[] encode (int x, int y, int z, ItemStack item, int side, EntityItemFrame[] attachedFrames, boolean sendAll, MapData map) throws IOException{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		{
			DataOutputStream data = new DataOutputStream(out);
			
			data.writeInt(x);
			data.writeInt(y);
			data.writeInt(z);
			
			int sizeStack = 0, sizeMeta = 0;
			boolean sendAsBlock = item != null && item.itemID < 4096 && item.getItemDamage() < 16;
			boolean additionalDataSent = item != null && (((!sendAsBlock || item.getItemDamage() != 0) && sendAll) || (item.getItemDamage() != 0 && sendAll));
			
			{
				/* packed byte
				 * 
				 * ABCD EEFF
				 * 
				 * A is if additional data is being sent (item frame data or side data)
				 * 
				 * B bit is set if the item is null
				 * 
				 * C if the metadata value is 0
				 * 
				 * D bit is set if the item blockid is lower then 4096 and the metadata is lower then 16
				 * 
				 * E bits represent how many bytes the item stack size takes up in the stream
				 * 
				 * F the size of the metadata value in the stream. if the sendAsBlock flag is true or metadata is already 0, it sends the side the barrel is facing
				 */
				int packed = 0;
				
				if (additionalDataSent || item == null){
					packed |= 0x80;
				}
				
				if (item == null){
					for (int i = 0; i < attachedFrames.length; i++){
						if (attachedFrames[i] != null){
							packed |= (4 << i);
						}
					}
					
					packed |= 0x40 | side & 0x3;
				}else{
					if (item.getItemDamage() == 0){
						packed |= 0x20;
					}
					
					if (sendAsBlock){
						packed |= 0x10;
					}else{
						if (item.getItemDamage() != 0){
							if ((item.getItemDamage() & ~0xFF) == 0){
								sizeMeta = 0;
							}else if ((item.getItemDamage() & ~0xFFFF) == 0){
								sizeMeta = 1;
							}else if ((item.getItemDamage() & ~0xFFFFFF) == 0){
								sizeMeta = 2;
							}else{
								sizeMeta = 3;
							}
						}
					}
					
					{
						if ((item.stackSize & ~0xFF) == 0){
							sizeStack = 0;
						}else if ((item.stackSize & ~0xFFFF) == 0){
							sizeStack = 1;
						}else if ((item.stackSize & ~0xFFFFFF) == 0){
							sizeStack = 2;
						}else{
							sizeStack = 3;
						}
						
						packed |= (sizeStack << 2);
					}
				}
				
				if (sendAsBlock || (item != null && item.getItemDamage() == 0)){
					packed |= (side & 0x3);
				}else{
					packed |= sizeMeta;
				}
				
				data.write(packed);
			}
			
			if (item != null) {
				if (additionalDataSent){
					int packed = (side & 0x3) << 6;
					
					if (!sendAsBlock && item.itemID < 256){
						packed |= 0x20;
					}
					
					if (sendAll){
						packed |= 0x10;
						
						for (int i = 0; i < attachedFrames.length; i++){
							if (attachedFrames[i] != null){
								packed |= 1 << i;
							}
						}
					}
					
					data.write(packed);
				}
				
				if (sendAsBlock){
					data.write(item.itemID >> 4);
					
					int frames = -1;
					
					if (item.getItemDamage() == 0){
						frames = 0;
						
						for (int i = 0; i < attachedFrames.length; i++){
							if (attachedFrames[i] != null){
								frames |= 1 << i;
							}
						}
					}
					
					data.write(item.itemID << 4 | (frames != -1 ? frames : item.getItemDamage()));
				}else{
					if (additionalDataSent && item.itemID < 256){
						data.write(item.itemID);
					}else{
						data.writeShort(item.itemID);
					}
					
					if (item.getItemDamage() != 0){
						for (int i = 0; i <= sizeMeta; i++){
							data.write(item.getItemDamage() >> (i * 8));
						}
					}
				}
				
				for (int i = 0; i <= sizeStack; i++){
					data.write(item.stackSize >> (i * 8));
				}
				
				if (map != null){
					NBTTagCompound mapTag = new NBTTagCompound();
					
					map.writeToNBT(mapTag);
					
					NBTBase.writeNamedTag(mapTag, data);
				}else if (item.getTagCompound() != null){
					NBTBase.writeNamedTag(item.getTagCompound(), data);
				}
			}
		}
		
		return out.toByteArray();
	}
	
	/**
	 * a non sensitive player version of <code>sendClientsFrameChange (TileEntityBarrel, Entity)</code>
	 */
	public void sendClientsFrameChange (TileEntityBarrel barrel){
		this.sendClientsFrameChange(barrel, null);
	}
	
	/**
	 * sends a update packet to update the item frame info
	 * 
	 * barrel    - the barrel that needs updating
	 * exception - the entity to exclude while sending that packet E.G., the src player
	 * 
	 * @return void
	 */
	public void sendClientsFrameChange (TileEntityBarrel barrel, Entity player){
		try{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			{
				DataOutputStream data = new DataOutputStream(out);
				
				data.writeInt(barrel.xCoord);
				data.writeInt(barrel.yCoord);
				data.writeInt(barrel.zCoord);
				
				int frames = 0;
				for (int i = 0; i < barrel.getFrames().length; i++){
					if (barrel.getFrames()[i] != null){
						frames |= 1 << i;
					}
				}
				
				data.write(0x40 | ((frames & 0xF) << 2) | (barrel.getSide() & 0x3));
			}
			
			this.sendPacket(barrel.xCoord, barrel.zCoord, barrel.worldObj.provider.dimensionId, out.toByteArray(), player);
		}catch (IOException e){
			throw new RuntimeException("Someting that sould never happen happens", e);
		}
	}
	
	/**
	 * a non sensitive player version of <code>sendClientsBlockChange (TileEntityBarrel, Entity)</code>
	 */
	public void sendClientsBlockChange (TileEntityBarrel barrel){
		this.sendClientsBlockChange(barrel, null);
	}
	
	/**
	 * sends the metadata/facing direction of a barrel.
	 * 
	 * @param barrel    - the barrel to send the block change E.G., metadata/facing
	 * @param exception - the entity to exclude while sending that packet E.G., the src player
	 */
	public void sendClientsBlockChange (TileEntityBarrel barrel, Entity exception){
		try{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			{
				DataOutputStream data = new DataOutputStream(out);
				
				data.writeInt(barrel.xCoord);
				data.writeInt(barrel.yCoord);
				data.writeInt(barrel.zCoord);
				
				data.write(((barrel.getBlockMetadata() & 0xF) << 2) | (barrel.getSide() & 0x3));
			}
			
			this.sendPacket(barrel.xCoord, barrel.zCoord, barrel.worldObj.provider.dimensionId, out.toByteArray(), exception);
		}catch (IOException e){
			throw new RuntimeException("Someting that sould never happen happens", e);
		}
	}
	
	/**
	 * utility method, sends a packet to players around a radius where the players chunk's are loaded.
	 * @throws IOException
	 */
	private void sendPacket(int x, int z, int dim, byte[] info, Entity exception) throws IOException{
		int radius = (MinecraftServer.getServerConfigurationManager(MinecraftServer.getServer()).getViewDistance() + 1) * 16;
		Iterator<EntityPlayerMP> players = MinecraftServer.getServerConfigurationManager(MinecraftServer.getServer()).playerEntityList.iterator();
		
		while (players.hasNext()){
			EntityPlayerMP player = players.next();
			
			if (player != exception){
				if (player.dimension == dim && Math.abs(player.posX - x) <= radius && Math.abs(player.posZ - z) <= radius){
					player.playerNetServerHandler.sendPacketToPlayer(new Packet250CustomPayload(NETWORK_CHANNEL, info));
				}
			}
		}
	}
	
	/**
	 * sends a request to the server for the items
	 * in a barrel
	 * 
	 * @param x - the X coordinate of the barrel
	 * @param y - the Y coordinate of the barrel
	 * @param z - the Z coordinate of the barrel
	 * 
	 * @throws IOException soulden't throw it since ByteArrayOutputStream's don't store data outside of RAM
	 * 
	 * @return void
	 */
	public void sendServerRequestForItem (int x, int y, int z){
		try{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			{
				DataOutputStream data = new DataOutputStream(out);
				
				data.writeInt(x);
				data.writeInt(y);
				data.writeInt(z);
			}
			
			PacketDispatcher.sendPacketToServer(new Packet250CustomPayload(NETWORK_CHANNEL, out.toByteArray()));
		}catch (IOException e){
			throw new RuntimeException("Someting that sould never happen happens", e);
		}
	}
	
	/**
	 * Called when the server/client receives info
	 * 
	 * if this is the server, then the only reason the server got it is because the client
	 * requested the info of the barrel. It sends the barrel's info using the serialize
	 * 
	 * if this is the client, it has received the info from the server and decompresses it.
	 * 
	 * @param the manager used to send packets
	 * @param the packed that was sent
	 * @param the player who sent the packet
	 * 
	 * @return void
	 */
	@Override
	public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player) {
		try {
			DataInputStream data = new DataInputStream(new ByteArrayInputStream (packet.data));
			
			int x = data.readInt();
			int y = data.readInt();
			int z = data.readInt();
			World world = ((EntityPlayer) player).worldObj;
			TileEntityBarrel tile = null;
			{
				TileEntity entity = world.getBlockTileEntity(x, y, z);
				if (entity == null || !(entity instanceof TileEntityBarrel)) return;
				tile = (TileEntityBarrel) entity;
			}
			
			if (world.isRemote) {
				int packed = data.read();
				
				if (data.available() == 0 && (packed & 0x80) == 0){
					if ((packed & 0x40) != 0){
						for (int i = 0; i < tile.getFrames().length; i++){
							if ((packed & (4 << i)) != 0){
								if (tile.getFrames()[i] == null){
									tile.getFrames()[i] = new EntityItemFrame(world, x, y, z, i);
								}
							}else{
								tile.getFrames()[i] = null;
							}
						}
					}else{
						int metadata = (packed & 0x3C) >> 2;
						
						if (metadata != tile.getBlockMetadata()){
							world.setBlockMetadataWithNotify(x, y, z, metadata, 0);
						}
					}
					
					tile.setSide(packed & 0x3);
					world.markBlockForRenderUpdate(x, y, z);
				}else{
					if ((packed & 0x40) != 0){
						tile.setItem(null);
						tile.setSide(packed & 0x3);
						
						for (int i = 0; i < tile.getFrames().length; i++){
							if ((packed & (4 << i)) != 0){
								if (tile.getFrames()[i] == null){
									tile.getFrames()[i] = new EntityItemFrame(world, x, y, z, i);
								}
							}else{
								tile.getFrames()[i] = null;
							}
						}
					}else{
						int frames = -1;
						if ((packed & 0x80) != 0){
							frames = data.read();
						}
						
						int blockid = 0, metadata = 0, stackSize = 0;
						
						if ((packed & 0x10) != 0){
							blockid = data.read() << 4;
							
							int blah = data.read();
							
							blockid |= blah >> 4;
							
							if ((packed & 0x20) != 0){
								metadata = 0;
								
								for (int i = 0; i < tile.getFrames().length; i++){
									if ((blah & (1 << i)) != 0){
										tile.getFrames()[i] = new EntityItemFrame(world, x, y, z, i);
									}else{
										tile.getFrames()[i] = null;
									}
								}
							}else{
								metadata = blah & 0xF;
							}
							
							tile.setSide(packed & 0x3);
						}else{
							if (frames != -1 && (frames & 0x20) != 0){
								blockid = data.readUnsignedByte();
							}else{
								blockid = data.readUnsignedShort();
							}
							
							if ((packed & 0x20) != 0){
								metadata = 0;
								
								tile.setSide(packed & 0x3);
							}else{
								for (int i = 0; i <= (packed & 0x3); i++){
									metadata |= (data.read() << (i * 8));
								}
								
								if (frames != -1){
									tile.setSide((frames & 0xC0) >> 6);
								}
							}
						}
						
						for (int i = 0; i <= ((packed & 0xC) >> 2); i++){
							stackSize |= (data.read() << (i * 8));
						}
						
						ItemStack item = new ItemStack(blockid, stackSize, metadata);
						
						if (data.available() != 0){
							NBTTagCompound tag = (NBTTagCompound) NBTBase.readNamedTag(data);
							
							if (item.getItem() instanceof ItemMap){
								MapData map = Item.map.getMapData(item, world);
								
								if (map == null){
									String s = "map_" + item.getItemDamage();
									map = new MapData(s);
									
									map.readFromNBT(tag);
									world.setItemData(s, map);
								}
							}else{
								item.setTagCompound(tag);
							}
						}else{
							item.setTagCompound(null);
						}
						
						tile.setItem(item);
						
						if (frames != -1 && (frames & 0x10) != 0){
							for (int i = 0; i < tile.getFrames().length; i++){
								if ((frames & (1 << i)) != 0){
									if (tile.getFrames()[i] == null){
										tile.getFrames()[i] = new EntityItemFrame(world, x, y, z, i);
									}
								}else{
									tile.getFrames()[i] = null;
								}
							}
						}
					}
				}
				
				tile.localUpdate();
			}else{
				this.updateClient(tile, player);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}