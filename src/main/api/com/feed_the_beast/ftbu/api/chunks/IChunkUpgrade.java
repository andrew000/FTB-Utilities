package com.feed_the_beast.ftbu.api.chunks;

import com.feed_the_beast.ftbl.lib.LangKey;
import net.minecraft.util.IStringSerializable;

/**
 * Created by LatvianModder on 01.03.2017.
 */
public interface IChunkUpgrade extends IStringSerializable
{
    //TODO: Move this to a registry someday
    int getId();

    LangKey getLangKey();
}