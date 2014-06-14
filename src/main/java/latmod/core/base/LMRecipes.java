package latmod.core.base;
import java.util.Map;

import latmod.core.*;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;

public class LMRecipes
{
	public final boolean storeRecipes;
	public FastList<IRecipe> craftingRecipes;
	public FastMap<ItemStack, ItemStack> furnaceRecipes;
	
	public LMRecipes(boolean b)
	{
		storeRecipes = b;
		craftingRecipes = new FastList<IRecipe>();
		furnaceRecipes = new FastMap<ItemStack, ItemStack>();
	}
	
	public ItemStack size(ItemStack is, int s)
	{ ItemStack is1 = is.copy(); is1.stackSize = s; return is1; }
	
	public void clearRecipes()
	{
		CraftingManager.getInstance().getRecipeList().removeAll(craftingRecipes);
		craftingRecipes.clear();
		
		Map m = FurnaceRecipes.smelting().getSmeltingList();
		for(ItemStack is : furnaceRecipes.keys) m.remove(is);
		furnaceRecipes.clear();
	}

	public void addRecipe(ItemStack out, Object... in)
	{
		IRecipe r = LatCore.addRecipe(out, in);
		if(storeRecipes) craftingRecipes.add(r);
	}
	
	public void addShapelessRecipe(ItemStack out, Object... in)
	{
		IRecipe r = LatCore.addShapelessRecipe(out, in);
		if(storeRecipes) craftingRecipes.add(r);
	}
	
	public void addItemBlockRecipe(ItemStack item, ItemStack block, boolean back)
	{
		ItemStack out9 = item.copy(); out9.stackSize = 9;
		addShapelessRecipe(out9, block);
		if(back) addRecipe(block, "EEE", "EEE", "EEE", Character.valueOf('E'), item);
	}
	
	public void addOre(String s, ItemStack is)
	{ LatCore.addOreDictionary(s, is); }
	
	public void addSmelting(ItemStack in, ItemStack out, float xp)
	{
		if(storeRecipes) furnaceRecipes.put(in, out);
		LatCore.addSmeltingRecipe(out, in, xp);
	}
	
	public void addSmelting(ItemStack in, ItemStack out)
	{ addSmelting(in, out, 0F); }
}