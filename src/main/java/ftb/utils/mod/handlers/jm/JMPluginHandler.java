package ftb.utils.mod.handlers.jm;

import ftb.lib.TextureCoords;
import ftb.lib.api.*;
import ftb.utils.mod.*;
import ftb.utils.mod.client.gui.claims.GuiClaimChunks;
import ftb.utils.world.*;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.*;
import journeymap.client.api.model.MapImage;
import latmod.lib.*;
import net.minecraft.util.*;
import net.minecraft.world.ChunkCoordIntPair;

import java.util.Map;

/**
 * Created by LatvianModder on 07.02.2016.
 */
public class JMPluginHandler implements IJMPluginHandler
{
	public final IClientAPI clientAPI;
	
	public JMPluginHandler(IClientAPI api)
	{
		clientAPI = api;
	}
	
	public void refresh(int dim)
	{
		clientAPI.removeAll(FTBUFinals.MOD_ID);
		
		if(FTBUWorldDataSP.exists() && !FTBUWorldDataSP.get().chunks.isEmpty() && clientAPI.playerAccepts(FTBUFinals.MOD_ID, DisplayType.Polygon))
		{
			for(ChunkCoordIntPair pos : FTBUWorldDataSP.get().chunks.keySet())
			{
				Map.Entry<TextureCoords, ChunkType> e1 = GuiClaimChunks.getForChunk(pos);
				
				if(e1 != null)
				{
					try
					{
						TextureCoords tc = e1.getKey();
						ChunkType type = e1.getValue();
						
						int color = LMColorUtils.getRGBA(type.getAreaColor(ForgeWorldSP.inst.clientPlayer), 0);
						
						BlockPos start = new BlockPos(MathHelperLM.unchunk(pos.chunkXPos), 0, MathHelperLM.unchunk(pos.chunkZPos));
						BlockPos end = new BlockPos(MathHelperLM.unchunk(pos.chunkXPos + 1), 0, MathHelperLM.unchunk(pos.chunkZPos + 1));
						
						//GuiLM.drawTexturedRectD(mainPanel.posX + x * 16, mainPanel.posY + y * 16, zLevel, 16, 16, tc.minU, tc.minV, tc.maxU, tc.maxV);
						
						//public MapImage(ResourceLocation imageLocation, int textureX, int textureY, int textureWidth, int textureHeight, int color, float opacity)
						MapImage image = new MapImage(tc.texture, tc.posXI(), tc.posYI(), (int) tc.width, (int) tc.height, color, 1F);
						image.setDisplayWidth(tc.textureW);
						image.setDisplayHeight(tc.textureH);
						ImageOverlay chunkOverlay = new ImageOverlay(FTBUFinals.MOD_ID, "claimed_" + pos.chunkXPos + "_" + pos.chunkZPos, start, end, image);
						
						StringBuilder sb = new StringBuilder(type.getIDS());
						
						if(type.asClaimed() != null)
						{
							sb.append('\n');
							sb.append(EnumChatFormatting.GREEN.toString());
							
							ForgePlayer player = type.asClaimed().chunk.getOwner();
							sb.append((ForgeWorldSP.inst.clientPlayer.isFriend(player) ? EnumChatFormatting.GREEN : EnumChatFormatting.BLUE) + player.getProfile().getName());
							
							if(type.asClaimed().chunk.isChunkloaded)
							{
								sb.append('\n');
								sb.append(FTBU.mod.translate("chunktype.chunkloaded"));
							}
						}
						
						chunkOverlay.setOverlayGroupName("Claimed_Chunks").setTitle(sb.toString());
						clientAPI.show(chunkOverlay);
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
				}
			}
		}
	}
}