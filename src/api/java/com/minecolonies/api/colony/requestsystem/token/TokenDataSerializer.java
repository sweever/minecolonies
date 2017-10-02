package com.minecolonies.api.colony.requestsystem.token;

import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.io.IOException;

/**
 * ------------ Class not Documented ------------
 */
public class TokenDataSerializer implements DataSerializer<IToken>
{
    @Override
    public void write(final PacketBuffer buf, final IToken value)
    {
        ByteBufUtils.writeTag(buf, StandardFactoryController.getInstance().serialize(value));
    }

    @Override
    public IToken read(final PacketBuffer buf) throws IOException
    {
        return StandardFactoryController.getInstance().deserialize(ByteBufUtils.readTag(buf));
    }

    @Override
    public DataParameter<IToken> createKey(final int id)
    {
        return new DataParameter<>(id, this);
    }
}
