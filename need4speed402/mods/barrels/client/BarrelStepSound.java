package need4speed402.mods.barrels.client;

import net.minecraft.block.StepSound;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BarrelStepSound extends StepSound{
	public BarrelStepSound(StepSound sound) {
		super(sound.stepSoundName, sound.getVolume(), sound.getPitch());
	}
	
	private boolean ignoreNextCall = false;
	
	public void ignoreNextCall (boolean flag){
		ignoreNextCall = flag;
	}
	
	@Override
	public String getStepSound() {
		if (ignoreNextCall){
			ignoreNextCall = false;
			return "";
		}else{
			return super.getStepSound();
		}
	}
}
