package agency.highlysuspect.appendages.parts;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.function.BiConsumer;

public enum BodyPart {
	HEAD, TORSO, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG,
	;
	
	public ModelPart associatedModelPart(PlayerEntityModel<?> model) {
		switch(this) {
			case HEAD: return model.head;
			case TORSO: return model.torso;
			case RIGHT_ARM: return model.rightArm;
			case LEFT_ARM: return model.leftArm;
			case RIGHT_LEG: return model.rightLeg;
			case LEFT_LEG: return model.leftLeg;
			default: throw new IllegalStateException("BodyPart with no model part assigned");
		}
	}
	
	private static final Multimap<BodyPart, MountPoint> mountsByPart = HashMultimap.create();
	
	static {
		//N.B. Cuboid coordinates are in "minecraft pixels" (so, the head is 8 cuboid units tall)
		//When rendering the actual cuboid, they are divided by 16.
		//So when I translate along the cuboid's rendered position, I also need to divide by 16.
		for(BodyPart part : values()) {
			mountsByPart.put(part, new MountPoint(part, "origin", (cuboid, stack) -> {
				//average the x, y, and z coordinates to go to the center of the cuboid
				stack.translate((cuboid.minX + cuboid.maxX) / 32f, (cuboid.minY + cuboid.maxY) / 32f, (cuboid.minZ + cuboid.maxZ) / 32f);
			}));
			mountsByPart.put(part, new MountPoint(part, "top", (cuboid, stack) -> {
				//average the x and z coordinates, but use the top y coordinate
				stack.translate((cuboid.minX + cuboid.maxX) / 32f, cuboid.minY / 16f, (cuboid.minZ + cuboid.maxZ) / 32f);
			}));
			mountsByPart.put(part, new MountPoint(part, "bottom", (cuboid, stack) -> {
				//average the x and z coordinates, but use the bottom y coordinate
				stack.translate((cuboid.minX + cuboid.maxX) / 32f, cuboid.maxY / 16f, (cuboid.minZ + cuboid.maxZ) / 32f);
				stack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(180));
			}));
			
			MountPoint left, right;
			mountsByPart.put(part, left = new MountPoint(part, "left", (cuboid, stack) -> {
				//average the y and z coordinates, but use the left x coordinate
				stack.translate(cuboid.maxX / 16f, (cuboid.minY + cuboid.maxY) / 32f, (cuboid.minZ + cuboid.maxZ) / 32f);
				stack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(90));
			}));
			mountsByPart.put(part, right = new MountPoint(part, "right", (cuboid, stack) -> {
				//average the y and z coordinates, but use the right x coordinate
				stack.translate(cuboid.minX / 16f, (cuboid.minY + cuboid.maxY) / 32f, (cuboid.minZ + cuboid.maxZ) / 32f);
				stack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(-90));
			}));
			left.mirrored = right;
			right.mirrored = left;
			
			mountsByPart.put(part, new MountPoint(part, "front", (cuboid, stack) -> {
				//average the x and y coordinates, but use the front z coordinate
				stack.translate((cuboid.minX + cuboid.maxX) / 32f, (cuboid.minY + cuboid.maxY) / 32f, cuboid.minZ / 16f);
				stack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(90));
			}));
			mountsByPart.put(part, new MountPoint(part, "back", (cuboid, stack) -> {
				//average the x and y coordinates, but use the back z coordinate
				stack.translate((cuboid.minX + cuboid.maxX) / 32f, (cuboid.minY + cuboid.maxY) / 32f, cuboid.maxZ / 16f);
				stack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-90));
			}));
		}
	}
	
	public Collection<MountPoint> getAvailableMountPoints() {
		return mountsByPart.get(this);
	}
	
	public MountPoint getMountPointByName(String name) {
		return getAvailableMountPoints().stream().filter(point -> point.name.equals(name)).findFirst().get();
	}
	
	public static class MountPoint {
		private MountPoint(BodyPart bodyPart, String name, BiConsumer<ModelPart.Cuboid, MatrixStack> setupFunc) {
			this.bodyPart = bodyPart;
			this.name = name;
			this.setupFunc = setupFunc;
		}
		
		private final BodyPart bodyPart;
		private final String name;
		private final BiConsumer<ModelPart.Cuboid, MatrixStack> setupFunc;
		private MountPoint mirrored = this;
		
		public BodyPart getBodyPart() {
			return bodyPart;
		}
		
		public String getName() {
			return name;
		}
		
		public void applyTransform(ModelPart.Cuboid cuboid, MatrixStack stack) {
			setupFunc.accept(cuboid, stack);
		}
		
		public MountPoint getMirrored() {
			return mirrored;
		}
	}
}
