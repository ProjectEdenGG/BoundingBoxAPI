/*
 * Copyright 2016 inventivetalent. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and contributors and should not be interpreted as representing official policies,
 *  either expressed or implied, of anybody else.
 */

package org.inventivetalent.boundingbox;

import net.minecraft.world.phys.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.inventivetalent.particle.ParticleEffect;
import org.inventivetalent.reflection.minecraft.Minecraft;
import org.inventivetalent.reflection.minecraft.MinecraftVersion;
import org.inventivetalent.reflection.resolver.ClassResolver;
import org.inventivetalent.reflection.resolver.FieldResolver;
import org.inventivetalent.reflection.resolver.MethodResolver;
import org.inventivetalent.reflection.resolver.ResolverQuery;
import org.inventivetalent.reflection.resolver.minecraft.NMSClassResolver;
import org.inventivetalent.vectors.d3.Vector3DDouble;
import org.inventivetalent.vectors.d3.Vector3DInt;

import java.util.function.Consumer;

public class BoundingBoxAPI {

	static ClassResolver classResolver = new ClassResolver();
	static NMSClassResolver nmsClassResolver = new NMSClassResolver();

	static Class<?> Entity = nmsClassResolver.resolveSilent("world.entity.Entity", "Entity");
	static Class<?> World = nmsClassResolver.resolveSilent("world.level.World", "World");
	static Class<?> Block = nmsClassResolver.resolveSilent("world.level.block.Block", "Block");
	static Class<?> BlockPosition = nmsClassResolver.resolveSilent("core.BlockPosition");
	static Class<?> Chunk = nmsClassResolver.resolveSilent("world.level.chunk.Chunk", "Chunk");
	static Class<?> IBlockData = nmsClassResolver.resolveSilent("world.level.block.state.IBlockData", "IBlockData");
	static Class<?> IBlockAccess = nmsClassResolver.resolveSilent("world.level.IBlockAccess", "IBlockAccess");
	static Class<?> BlockData = nmsClassResolver.resolveSilent("world.level.block.state.BlockBase$BlockData", "BlockBase$BlockData");
	static Class<?> VoxelShape;
	static Class<?> VoxelShapeCollision;

	static FieldResolver EntityFieldResolver = new FieldResolver(Entity);
	static FieldResolver BlockFieldResolver = new FieldResolver(Block);

	static MethodResolver BlockMethodResolver = new MethodResolver(Block);
	static MethodResolver ChunkMethodResolver = new MethodResolver(Chunk);
	static MethodResolver IBlockDataMethodResolver = new MethodResolver(IBlockData);
	static MethodResolver EntityMethodResolver = new MethodResolver(Entity);
	static MethodResolver BlockDataMethodResolver;
	static MethodResolver VoxelShapeMethodResolver;
	static MethodResolver VoxelShapeCollisionMethodResolver;

	public static BoundingBox getBoundingBox(Entity entity) {
		return getAbsoluteBoundingBox(entity).expand(entity.getLocation().toVector().multiply(-1));
	}

	public static BoundingBox getAbsoluteBoundingBox(Entity entity) {
		try {
			try {
				return fromNMS(EntityFieldResolver.resolveAccessor("boundingBox").get(Minecraft.getHandle(entity)));
			} catch (NullPointerException ex) {
				return fromNMS(EntityFieldResolver.resolveAccessor("aA").get(Minecraft.getHandle(entity)));
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setBoundingBox(Entity entity, BoundingBox boundingBox) {
		try {
			try {
				EntityFieldResolver.resolveAccessor("boundingBox").set(Minecraft.getHandle(entity), toNMS(boundingBox));
			} catch (NullPointerException ex) {
				EntityFieldResolver.resolveAccessor("aA").set(Minecraft.getHandle(entity), toNMS(boundingBox));
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setSize(Entity entity, float width, float length) {
		try {
			EntityMethodResolver.resolve(new ResolverQuery("setSize", float.class, float.class)).invoke(Minecraft.getHandle(entity), width, length);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static BoundingBox getBoundingBox(Block block) {
		try {
			Location location = block.getLocation();

			Object blockPosition = BlockPosition.getConstructor(double.class, double.class, double.class).newInstance(location.getX(), location.getY(), location.getZ());
			Object iBlockData = ChunkMethodResolver.resolve(new ResolverQuery(MinecraftVersion.VERSION.newerThan(Minecraft.Version.v1_13_R1) ? "getType" : "getBlockData", BlockPosition)).invoke(Minecraft.getHandle(block.getChunk()), blockPosition);
			Object iBlockAccess = Minecraft.getHandle(location.getWorld());
			MethodResolver blockResolver = IBlockDataMethodResolver;
			if (MinecraftVersion.VERSION.newerThan(Minecraft.Version.v1_16_R1)) {
				if (BlockDataMethodResolver == null) {
					BlockDataMethodResolver = new MethodResolver(BlockData);
				}
				blockResolver = BlockDataMethodResolver;
			}
			Object nmsBlock = blockResolver.resolve("getBlock").invoke(iBlockData);

			Object axisAlignedBB;
			if (MinecraftVersion.VERSION.newerThan(Minecraft.Version.v1_13_R1)) {
				if (VoxelShape == null) {
					VoxelShape = nmsClassResolver.resolveSilent("world.phys.shapes.VoxelShape", "VoxelShape");
				}
				if (VoxelShapeMethodResolver == null) {
					VoxelShapeMethodResolver = new MethodResolver(VoxelShape);
				}
				Object voxelShape;
				if (MinecraftVersion.VERSION.newerThan(Minecraft.Version.v1_14_R1)) {
					if (VoxelShapeCollision == null) {
						VoxelShapeCollision = nmsClassResolver.resolveSilent("world.phys.shapes.VoxelShapeCollision", "VoxelShapeCollision");
					}
					if (VoxelShapeCollisionMethodResolver == null) {
						VoxelShapeCollisionMethodResolver = new MethodResolver(VoxelShapeCollision);
					}
					/// static VoxelShapeCollision a()
					Object collision = VoxelShapeCollisionMethodResolver.resolveSignature("VoxelShapeCollision a()").invoke(null);
					if (MinecraftVersion.VERSION.newerThan(Minecraft.Version.v1_16_R1)) {
						voxelShape = BlockDataMethodResolver.resolve(new ResolverQuery("a", IBlockAccess, BlockPosition, VoxelShapeCollision)).invoke(iBlockData, iBlockAccess, blockPosition, collision);
					} else {
						voxelShape = BlockMethodResolver.resolve(new ResolverQuery("a", IBlockData, IBlockAccess, BlockPosition, VoxelShapeCollision)).invoke(nmsBlock, iBlockData, iBlockAccess, blockPosition, collision);
					}
				} else {
					voxelShape = BlockMethodResolver.resolve(new ResolverQuery("a", IBlockData, IBlockAccess, BlockPosition)).invoke(nmsBlock, iBlockData, iBlockAccess, blockPosition);
				}
				axisAlignedBB = VoxelShapeMethodResolver.resolveSignature("AxisAlignedBB a()", "AxisAlignedBB getBoundingBox()").invoke(voxelShape);
			} else if (MinecraftVersion.VERSION.newerThan(Minecraft.Version.v1_9_R1)) {
				axisAlignedBB = BlockMethodResolver.resolve(new ResolverQuery("a", IBlockData, IBlockAccess, BlockPosition)).invoke(nmsBlock, iBlockData, iBlockAccess, blockPosition);
			} else {
				axisAlignedBB = BlockMethodResolver.resolve(new ResolverQuery("a", World, BlockPosition, IBlockData)).invoke(nmsBlock, iBlockAccess/*world*/, blockPosition, iBlockData);
			}
			return fromNMS(axisAlignedBB);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static BoundingBox getAbsoluteBoundingBox(Block block) {
		return getBoundingBox(block).expand(block.getLocation().toVector());
	}

	public static Runnable drawParticleOutline(final BoundingBox boundingBox, final World world, final org.bukkit.Particle particle) {
		return () -> drawParticleOutline(boundingBox, world, location -> world.spawnParticle(particle, location, 1));
	}

	public static Runnable drawParticleOutline(final BoundingBox boundingBox, final World world, final ParticleEffect particle) {
		return () -> drawParticleOutline(boundingBox, world, location -> particle.send(world.getPlayers(), location, 0, 0, 0, 0, 1));
	}

	private static void drawParticleOutline(BoundingBox boundingBox, org.bukkit.World world, Consumer<Location> spawnParticle) {
		Vector3DInt minCorner = new Vector3DInt((int) (boundingBox.getMinX() * 1000), (int) (boundingBox.getMinY() * 1000), (int) (boundingBox.getMinZ() * 1000));
		Vector3DInt maxCorner = new Vector3DInt((int) (boundingBox.getMaxX() * 1000), (int) (boundingBox.getMaxY() * 1000), (int) (boundingBox.getMaxZ() * 1000));

		int i = 0;
		for (int x = minCorner.getX(); x <= maxCorner.getX(); x += 20) {
			for (int z = minCorner.getZ(); z <= maxCorner.getZ(); z += 20) {
				for (int y = minCorner.getY(); y <= maxCorner.getY(); y += 20) {
					Vector3DDouble vector = new Vector3DDouble(x / 1000.D, y / 1000.0D, z / 1000.0D);
					int edgeIntersectionCount = 0;

					//https://bukkit.org/threads/calculating-the-edges-of-a-rectangle-cuboid-discussion.78267/#post-1142330
					if (Math.abs(x - minCorner.getX()) < 8 || Math.abs(x - maxCorner.getX()) < 8)
						edgeIntersectionCount++;

					if (Math.abs(y - minCorner.getY()) < 8 || Math.abs(y - maxCorner.getY()) < 8)
						edgeIntersectionCount++;

					if (Math.abs(z - minCorner.getZ()) < 8 || Math.abs(z - maxCorner.getZ()) < 8)
						edgeIntersectionCount++;

					if (edgeIntersectionCount >= 2) {
						if (i++ % 9 != 0)
							continue;

						spawnParticle.accept(vector.toBukkitLocation(world));
					}
				}
			}
		}
	}

	static Class<?> AxisAlignedBB = nmsClassResolver.resolveSilent("world.phys.AxisAlignedBB", "AxisAlignedBB");

	static FieldResolver AxisAlignedBBFieldResolver = new FieldResolver(AxisAlignedBB);

	public static Object toNMS(BoundingBox box) {
		try {
			return AxisAlignedBB.getConstructor(double.class, double.class, double.class, double.class, double.class, double.class)
					.newInstance(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static BoundingBox fromNMS(Object axisAlignedBB) {
		try {
			double a = (double) AxisAlignedBBFieldResolver.resolve("a", "minX").get(axisAlignedBB);
			double b = (double) AxisAlignedBBFieldResolver.resolve("b", "minY").get(axisAlignedBB);
			double c = (double) AxisAlignedBBFieldResolver.resolve("c", "minZ").get(axisAlignedBB);
			double d = (double) AxisAlignedBBFieldResolver.resolve("d", "maxX").get(axisAlignedBB);
			double e = (double) AxisAlignedBBFieldResolver.resolve("e", "maxY").get(axisAlignedBB);
			double f = (double) AxisAlignedBBFieldResolver.resolve("f", "maxZ").get(axisAlignedBB);

			return new BoundingBox(a, b, c, d, e, f);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
