package com.minecolonies.coremod.network.messages;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.client.colony.IColonyView;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.coremod.colony.CitizenData;
import com.minecolonies.coremod.colony.Colony;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Add or Update a ColonyView on the client.
 */
public class ColonyViewCitizenViewMessage implements IMessage, IMessageHandler<ColonyViewCitizenViewMessage, IMessage>
{
    private IToken  colonyId;
    private int dimensionId;
    private int     citizenId;
    private ByteBuf citizenBuffer;

    /**
     * Empty constructor used when registering the message.
     */
    public ColonyViewCitizenViewMessage()
    {
        super();
    }

    /**
     * Updates a {@link com.minecolonies.coremod.colony.CitizenDataView} of the citizens.
     *
     * @param colony  Colony of the citizen
     * @param citizen Citizen data of the citizen to update view
     */
    public ColonyViewCitizenViewMessage(@NotNull final Colony colony, @NotNull final CitizenData citizen)
    {
        this.colonyId = colony.getID();
        this.citizenId = citizen.getId();
        this.citizenBuffer = Unpooled.buffer();
        citizen.serializeViewNetworkData(citizenBuffer);
    }

    @Override
    public void fromBytes(@NotNull final ByteBuf buf)
    {
        colonyId = StandardFactoryController.getInstance().readFromBuffer(buf);
        dimensionId = buf.readInt();
        citizenId = buf.readInt();
        this.citizenBuffer = Unpooled.buffer();
        buf.readBytes(citizenBuffer, buf.readableBytes());
    }

    @Override
    public void toBytes(@NotNull final ByteBuf buf)
    {
        StandardFactoryController.getInstance().writeToBuffer(buf, colonyId);
        buf.writeInt(dimensionId);
        buf.writeInt(citizenId);
        buf.writeBytes(citizenBuffer);
    }

    @Nullable
    @Override
    public IMessage onMessage(@NotNull final ColonyViewCitizenViewMessage message, final MessageContext ctx)
    {
        //Check if we are in the same dimension. Updates of a Colony that is not in our dimension are useless anyway.
        //Safety meassure since we should not be subscribed to these colonies anyway.
        if (FMLClientHandler.instance().getWorldClient().provider.getDimension() != dimensionId)
            return null;

        final IColonyView view = IAPI.Holder.getApi().getClientColonyManager().getControllerForWorld(FMLClientHandler.instance().getWorldClient()).getColony(colonyId);
        if (view == null)
        {
            return null;
        }

        return view.handleColonyViewCitizensMessage(citizenId, citizenBuffer);
    }
}
