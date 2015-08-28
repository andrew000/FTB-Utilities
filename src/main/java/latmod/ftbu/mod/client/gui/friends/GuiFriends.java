package latmod.ftbu.mod.client.gui.friends;

import static net.minecraft.util.EnumChatFormatting.GREEN;
import latmod.ftbu.core.client.*;
import latmod.ftbu.core.gui.*;
import latmod.ftbu.core.net.*;
import latmod.ftbu.core.util.*;
import latmod.ftbu.core.world.*;
import latmod.ftbu.mod.FTBU;
import latmod.ftbu.mod.client.FTBUClient;
import latmod.ftbu.mod.client.minimap.Waypoints;
import latmod.ftbu.mod.player.ClientNotifications;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.*;

@SideOnly(Side.CLIENT)
public class GuiFriends extends GuiLM implements IClientActionGui
{
	public static final ResourceLocation texPlayers = FTBU.mod.getLocation("textures/gui/players.png");
	
	public static final TextureCoords icon_status[] =
	{
		new TextureCoords(texPlayers, 18 * 0, 144, 18, 18),
		new TextureCoords(texPlayers, 18 * 1, 144, 18, 18),
		new TextureCoords(texPlayers, 18 * 2, 144, 18, 18),
	};
	
	public static final TextureCoords tex_slider = new TextureCoords(texPlayers, 0, 162, 16, 13);
	
	public final GuiScreen parentScreen;
	public final LMPlayerClient owner;
	public final FastList<Player> players;
	
	public final TextBoxLM searchBox;
	public final ButtonLM buttonSave, buttonSort, buttonArmor;
	public final ButtonPlayer pbOwner;
	public final ButtonPlayer[] pbPlayers;
	public final FastList<ButtonNotification> notificationButtons = new FastList<ButtonNotification>();
	public final SliderLM scrollbar;
	
	static LMClientPlayer selectedPlayer = null;
	static final FastList<ButtonAction> actionButtons = new FastList<ButtonAction>();
	static LMPlayerClient staticOwner;
	private static LMPComparator comparator = null;
	static boolean notificationsGuiOpen = false;
	public int notificationsWidth = 0;
	
	public GuiFriends(GuiScreen gui)
	{
		super(null, texPlayers);
		parentScreen = gui;
		hideNEI = true;
		
		owner = LMWorldClient.inst.getPlayer(container.player);
		players = new FastList<Player>();
		staticOwner = owner;
		
		xSize = 240;
		ySize = 144;
		
		searchBox = new TextBoxLM(this, 103, 5, 94, 18)
		{
			public void textChanged()
			{
				refreshPlayers();
			}
		};
		
		searchBox.charLimit = 15;
		
		buttonSave = new ButtonLM(this, 218, 6, 16, 16)
		{
			public void onButtonPressed(int b)
			{
				playClickSound();
				if(parentScreen != null)
					mc.displayGuiScreen(parentScreen);
				else
					mc.thePlayer.closeScreen();
			}
		};
		
		buttonSave.title = GREEN + FTBULang.button_close;
		
		buttonSort = new ButtonLM(this, 199, 6, 16, 16)
		{
			public void onButtonPressed(int b)
			{
				if(b == 0)
					comparator = LMPComparator.map.values.get((comparator.listID + 1) % LMPComparator.map.size());
				else
				{
					int i = comparator.listID - 1;
					if(i < 0) i = LMPComparator.map.size() - 1;
					comparator = LMPComparator.map.values.get(i);
				}
				
				refreshPlayers();
				playClickSound();
			}
			
			public void addMouseOverText(FastList<String> l)
			{
				l.add(title);
				if(isShiftKeyDown())
				{
					for(LMPComparator c : LMPComparator.map.values)
						l.add(((c == comparator) ? EnumChatFormatting.BLUE : EnumChatFormatting.GRAY) + c.translatedName);
				}
				else l.add(EnumChatFormatting.BLUE + comparator.translatedName);
			}
			
			public boolean isEnabled()
			{ return !GuiFriends.notificationsGuiOpen; }
		};
		
		buttonSort.title = FTBU.mod.translateClient("button.lmp_comparator");
		
		buttonArmor = new ButtonLM(this, 64, 7, 16, 16)
		{
			public void onButtonPressed(int b)
			{
				playClickSound();
				FTBUClient.hideArmorFG.onClicked();
				ClientConfig.Registry.save();
			}
			
			public boolean isEnabled()
			{ return !GuiFriends.notificationsGuiOpen; }
		};
		
		scrollbar = new SliderLM(this, 218, 26, tex_slider.width, 111, tex_slider.height)
		{
			public boolean canMouseScroll()
			{ return !notificationsGuiOpen; }
		};
		
		scrollbar.isVertical = true;
		scrollbar.displayMin = scrollbar.displayMax = 0;
		
		// Player buttons //
		
		pbOwner = new ButtonPlayer(this, -1, 84, 5);
		pbOwner.setPlayer(new Player(this, owner));
		
		pbPlayers = new ButtonPlayer[7 * 6];
		
		for(int i = 0; i < pbPlayers.length; i++)
			pbPlayers[i] = new ButtonPlayer(this, i, 84 + (i % 7) * 19, 25 + (i / 7) * 19);
		
		if(selectedPlayer == null) selectedPlayer = new LMClientPlayer(owner);
		
		LMPComparator.init();
		if(comparator == null)
			comparator = LMPComparator.map.values.get(0);
		
		refreshPlayers();
	}
	
	public void addWidgets(FastList<WidgetLM> l)
	{
		l.add(searchBox);
		l.add(buttonSave);
		l.add(buttonSort);
		l.add(buttonArmor);
		l.add(scrollbar);
		
		l.add(pbOwner);
		l.addAll(pbPlayers);
		
		actionButtons.clear();
		
		if(selectedPlayer.playerLM.equalsPlayer(owner))
		{
			actionButtons.add(new ButtonAction(this, PlayerAction.settings, FTBULang.client_config));
			actionButtons.add(new ButtonAction(this, PlayerAction.notifications, FTBULang.Friends.notifications));
			actionButtons.add(new ButtonAction(this, PlayerAction.waypoints, Waypoints.clientConfig.getIDS()));
			actionButtons.add(new ButtonAction(this, PlayerAction.minimap, FTBULang.Friends.claimed_chunks));
			//actionButtons.add(new ActionButton(this, PlayerAction.notes, "Notes"));
		}
		else
		{
			boolean isFriend = owner.isFriendRaw(selectedPlayer.playerLM);
			actionButtons.add(new ButtonAction(this, PlayerAction.friend_toggle, isFriend ? FTBULang.Friends.button_rem_friend : FTBULang.Friends.button_add_friend));
			
			if(!isFriend && GuiFriends.selectedPlayer.playerLM.isFriendRaw(owner))
				actionButtons.add(new ButtonAction(this, PlayerAction.friend_deny, FTBULang.Friends.button_deny_friend));
		}
		
		l.addAll(actionButtons);
		
		if(notificationsGuiOpen)
		{
			notificationsWidth = MathHelperLM.max(100, guiLeft - 10, getMaxNTextLength(), fontRendererObj.getStringWidth(FTBULang.Friends.notifications) + 30).intValue();
			notificationButtons.clear();
			ClientNotifications.perm.sort(null);
			
			for(int i = 0; i < ClientNotifications.perm.size(); i++)
			{
				ButtonNotification b = new ButtonNotification(this, ClientNotifications.perm.get(i));
				if(b.index * 26 + 16 <= height) notificationButtons.add(b);
			}
			
			l.addAll(notificationButtons);
			
			ButtonLM b = new ButtonLM(this, -getPosX(0) + 2, -getPosY(0) + 2, notificationsWidth, 14)
			{
				public void onButtonPressed(int b)
				{
					notificationsGuiOpen = false;
					refreshWidgets();
				}
			};
			
			b.title = FTBULang.button_close;
			l.add(b);
		}
	}
	
	private int getMaxNTextLength()
	{
		int s = 0;
		
		for(ClientNotifications.PermNotification n : ClientNotifications.perm)
		{
			int l = fontRendererObj.getStringWidth(n.notification.title.getFormattedText());
			if(n.notification.getDesc() != null)
				l = Math.max(l, fontRendererObj.getStringWidth(n.notification.getDesc().getFormattedText()));
			
			if(n.notification.getItem() != null) l += 20;
			l += 6;
			
			if(l > s) s = l;
		}
		
		return s;
	}
	
	public void drawBackground()
	{
		if(players.size() < pbPlayers.length)
			scrollbar.value = 0F;
		else if(scrollbar.update())
			refreshPlayers();
		
		super.drawBackground();
		
		pbOwner.render();
		for(int i = 0; i < pbPlayers.length; i++)
			pbPlayers[i].render();
		
		buttonSave.render(Icons.accept);
		buttonSort.render(Icons.sort);
		
		buttonArmor.render(Icons.jacket);
		if(FTBUClient.hideArmorFG.getB()) buttonArmor.render(Icons.close);
		
		GL11.glColor4f(1F, 1F, 1F, 1F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		drawRect(guiLeft + 2, guiTop - 18, guiLeft + 2 + GuiFriends.actionButtons.size() * 18, guiTop, 0x66666666);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glColor4f(1F, 1F, 1F, 1F);
		GL11.glEnable(GL11.GL_BLEND);
		
		for(ButtonAction a : actionButtons)
			a.render();
		
		scrollbar.renderSlider(tex_slider);
		
		if(FTBUClient.hideArmorFG.getB())
		{
			for(int i = 0; i < 4; i++)
				selectedPlayer.inventory.armorInventory[i] = null;
		}
		else
		{
			EntityPlayer ep1 = selectedPlayer.playerLM.getPlayer();
			
			if(ep1 != null)
			{
				selectedPlayer.inventory.mainInventory = ep1.inventory.mainInventory.clone();
				selectedPlayer.inventory.armorInventory = ep1.inventory.armorInventory.clone();
				selectedPlayer.inventory.currentItem = ep1.inventory.currentItem;
			}
			else
			{
				for(int i = 0; i < 4; i++)
					selectedPlayer.inventory.armorInventory[i] = selectedPlayer.playerLM.lastArmor[i];
				selectedPlayer.inventory.mainInventory[0] = selectedPlayer.playerLM.lastArmor[4];
				selectedPlayer.inventory.currentItem = 0;
			}
		}
		
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
		int playerX = guiLeft + 44;
		int playerY = guiTop + 127;
		GuiInventory.func_147046_a(playerX, playerY, 55, playerX - mouseX, playerY - 90 - mouseY, selectedPlayer);
		GL11.glColor4f(1F, 1F, 1F, 1F);
		GL11.glPopAttrib();
		GL11.glPopMatrix();
		
		if(notificationsGuiOpen)
		{
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			
			drawRect(0, 0, notificationsWidth + 4, height, 0x33666666);
			drawRect(2, 2, notificationsWidth + 2, 16, 0xFF666666);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			fontRendererObj.drawString(FTBULang.Friends.notifications + " [" + ClientNotifications.perm.size() + "]", 6, 5, 0xFFFFFFFF);
			for(ButtonNotification b : notificationButtons) b.render();
		}
	}
	
	public void drawText(FastList<String> l)
	{
		searchBox.render(107, 10, 0xFFA7A7A7);
		
		drawString(fontRendererObj, selectedPlayer.playerLM.getName(), guiLeft + 5, guiTop + ySize + 1, 0xFFFFFFFF);
		
		super.drawText(l);
	}
	
	public void initLMGui()
	{
		LMNetHelper.sendToServer(new MessageLMPlayerRequestInfo(0));
	}
	
	public void onClientDataChanged()
	{
		refreshPlayers();
	}
	
	public void refreshPlayers()
	{
		refreshWidgets();
		
		players.clear();
		
		FastList<LMPlayerClient> list0 = new FastList<LMPlayerClient>();
		list0.addAll(LMWorldClient.inst.players);
		list0.remove(owner);
		list0.sort(comparator);
		
		for(LMPlayerClient p : list0)
			players.add(new Player(this, p));
		
		if(!searchBox.text.isEmpty())
		{
			FastList<Player> l = new FastList<Player>();
			
			String s = searchBox.text.trim().toLowerCase();
			for(int i = 0; i < players.size(); i++)
			{
				Player p = players.get(i);
				if(p.player.getName().toLowerCase().contains(s)) l.add(players.get(i));
			}
			
			players.clear();
			players.addAll(l);
		}
		
		for(int i = 0; i < pbPlayers.length; i++)
		{
			int j = i;
			
			if(players.size() >= pbPlayers.length)
			{
				int k = ((players.size() % 7) == 0) ? 0 : 1;
				j += (int)(scrollbar.value * ((players.size() - pbPlayers.length) / 7) + k) * 7;
			}
			
			if(j < 0 || j >= players.size()) j =-1;
			
			pbPlayers[i].setPlayer((j == -1) ? null : players.get(j));
		}
	}
}