package need4speed402.mods.barrels.proxy;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import need4speed402.mods.barrels.Barrels;
import need4speed402.mods.barrels.TileEntityBarrel;
import need4speed402.mods.barrels.client.BarrelRender;
import need4speed402.mods.barrels.client.BarrelStepSound;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.resources.Language;
import net.minecraft.client.resources.LanguageManager;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ProxyClient extends Proxy{
	
	@Override
	public void preInit() {
		TileEntityRenderer.instance.specialRendererMap.put(TileEntityBarrel.class, BarrelRender.getInstance());
		BarrelRender.getInstance().setTileEntityRenderer(TileEntityRenderer.instance);
	}
	
	
	/**
	 * Loads all the languages that the mod has in it's jar file.
	 */
	@Override
	public void init(){
		try{
			ArrayList<String> languages = new ArrayList<String>();
			ArrayList<InputStream> languageFiles = new ArrayList<InputStream>();
			
			try{
				File file = new File(decode(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()));
				
				if (file.getPath().endsWith(".class")){
					//Executed in a folder environment
					file = new File(file.getPath().substring(0, file.getPath().lastIndexOf(this.getClass().getPackage().getName().replace(".", File.separator))));
				}
				
				if (file.isDirectory()){
					file = new File(file, "lang/barrels");
					
					for (File f : file.listFiles()){
						languages.add(f.getName().substring(0, f.getName().lastIndexOf('.')));
						languageFiles.add(new BufferedInputStream(new FileInputStream(f)));
					}
				}else{
					JarInputStream in = new JarInputStream(new BufferedInputStream(new FileInputStream(file)));
					
					JarEntry entry;
					while ((entry = in.getNextJarEntry()) != null){
						if (entry.getName().startsWith("lang/barrels/") && !entry.getName().endsWith("/")){
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							
							byte[] buffer = new byte[1024];
							int len;
							while ((len = in.read(buffer)) > 0){
								out.write(buffer, 0, len);
							}
							
							languages.add(entry.getName().substring(entry.getName().lastIndexOf('/') + 1, entry.getName().lastIndexOf('.')));
							languageFiles.add(new ByteArrayInputStream(out.toByteArray()));
						}
					}
					
					in.close();
				}
			}catch (Exception e){
				LanguageManager manager = Minecraft.getMinecraft().func_135016_M();
				/* 
				 * failed to get all the language files. Probably failed to get the path of the jar file or folder
				 * tries to load from minecraf's existing library.
				 * this will only add languages that minecraft has registered itself.
				 * 
				 * the only reason why I don't do this in the first place is because the other way is faster
				 */
				
				Iterator i = manager.func_135040_d().iterator();
				
				while (i.hasNext()){
					Language lang = (Language) i.next();
					
					InputStream stream = this.getClass().getClassLoader().getResourceAsStream("lang/barrels/" + lang.func_135034_a() + ".lang");
					
					if (stream != null){
						languages.add(lang.func_135034_a());
						languageFiles.add(stream);
					}
				}
			}
			
			for (int i = 0; i < Math.min(languages.size(), languageFiles.size()); i++){
				Properties prop = new Properties();
				prop.load(languageFiles.get(i));
				
				if (Barrels.instance.T1BarrelMaxStorage != -1) LanguageRegistry.instance().addNameForObject(Barrels.instance.T1barrel, languages.get(i), prop.getProperty("T1barrel"));
				if (Barrels.instance.T2BarrelMaxStorage != -1) LanguageRegistry.instance().addNameForObject(Barrels.instance.T2barrel, languages.get(i), prop.getProperty("T2barrel"));
				if (Barrels.instance.T3BarrelMaxStorage != -1) LanguageRegistry.instance().addNameForObject(Barrels.instance.T3barrel, languages.get(i), prop.getProperty("T3barrel"));
				
				Barrels.instance.prefix.put(languages.get(i), prop.getProperty("prefix"));
				Barrels.instance.suffix.put(languages.get(i), prop.getProperty("suffix"));
				
				languageFiles.get(i).close();
			}
		}catch (Exception e){
			throw new RuntimeException("Failed to init names", e);
		}
	}
	
	@Override
	public void postInit(){
		Barrels.instance.barrel.setStepSound(new BarrelStepSound(Block.soundWoodFootstep));
	}
	
	/**
	 * decodes the string returned from
	 * this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()
	 * encoded with UTF-8. This fixes special characters like spaces.
	 * 
	 * @param encoded string
	 * @return decoded string
	 */
	private static String decode(String s){
		StringBuilder sb = new StringBuilder(s.length());
		
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '%'){
				try{
					sb.append(new String( new byte[] { Byte.parseByte(s.substring(i + 1, i + 3), 16) }, 0, 1, "UTF-8"));
					i += 2;
				}catch (Exception e){
					sb.append(c);
				}
			}else{
				sb.append(c);
			}
		}

		return (sb.toString());
	}
}