package factorization.shared;

import factorization.api.Coord;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;

public class FzNetDispatch {
    
    public static FMLProxyPacket generate(byte[] data) {
        return new FMLProxyPacket(Unpooled.wrappedBuffer(data), FzNetEventHandler.channelName);
    }
    
    public static FMLProxyPacket generate(ByteBufOutputStream buf) {
        return new FMLProxyPacket(buf.buffer(), FzNetEventHandler.channelName);
    }
    
    public static FMLProxyPacket generate(ByteArrayOutputStream baos) {
        return new FMLProxyPacket(Unpooled.wrappedBuffer(baos.toByteArray()), FzNetEventHandler.channelName);
    }
    
    public static void addPacket(FMLProxyPacket packet, EntityPlayer player) {
        if (player.worldObj.isRemote) {
            FzNetEventHandler.channel.sendToServer(packet);
        } else {
            FzNetEventHandler.channel.sendTo(packet, (EntityPlayerMP) player);
        }
    }
    
    public static void addPacketFrom(Packet packet, Chunk chunk) {
        final WorldServer world = (WorldServer) chunk.worldObj;
        if (world.isRemote) return;
        final PlayerManager pm = world.getPlayerManager();
        final int near = 10;
        final int far = 16;
        final int sufficientlyClose = (near*16)*(near*16);
        final int superlativelyFar = (far*16)*(far*16);
        final int chunkBlockX = chunk.xPosition*16 + 8;
        final int chunkBlockZ = chunk.zPosition*16 + 8;
        for (EntityPlayerMP player : (Iterable<EntityPlayerMP>) world.playerEntities) {
            double dx = player.posX - chunkBlockX;
            double dz = player.posZ - chunkBlockZ;
            double dist = dx*dx + dz*dz;
            if (dist > superlativelyFar) continue;
            if (dist < sufficientlyClose || pm.isPlayerWatchingChunk(player, chunk.xPosition, chunk.zPosition)) {
                player.playerNetServerHandler.sendPacket(packet);
            }
        }
    }
    
    public static void addPacketFrom(Packet packet, World world, double x, double z) {
        int chunkX = MathHelper.floor_double(x / 16.0D);
        int chunkZ = MathHelper.floor_double(z / 16.0D);
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        addPacketFrom(packet, chunk);
    }
    
    public static void addPacketFrom(Packet packet, Entity ent) {
        addPacketFrom(packet, ent.worldObj, ent.posX, ent.posZ);
    }
    
    public static void addPacketFrom(Packet packet, TileEntity ent) {
        World w = ent.getWorldObj();
        addPacketFrom(packet, w.getChunkFromBlockCoords(ent.xCoord, ent.zCoord));
    }
    
    public static void addPacketFrom(Packet packet, Coord c) {
        addPacketFrom(packet, c.getChunk());
    }
    
}