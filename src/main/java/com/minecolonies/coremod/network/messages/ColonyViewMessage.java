package com.minecolonies.coremod.network.messages;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.client.colony.IColonyView;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.coremod.colony.Colony;
import com.minecolonies.coremod.colony.ColonyView;
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
public class ColonyViewMessage implements IMessage, IMessageHandler<ColonyViewMessage, IMessage>
{
    private IToken colonyId;
    private int dimensionId;
    private boolean isNewSubscription;
    private ByteBuf colonyBuffer;

    /**
     * Empty constructor used when registering the message.
     */
    public ColonyViewMessage()
    {
        super();
    }

    /**
     * Add or Update a ColonyView on the client.
     *
     * @param colony            Colony of the view to update.
     * @param isNewSubscription Boolean whether or not this is a new subscription.
     */
    public ColonyViewMessage(@NotNull final Colony colony, final boolean isNewSubscription)
    {
        this.colonyId = colony.getID();
        this.dimensionId = colony.getDimension();
        this.isNewSubscription = isNewSubscription;
        this.colonyBuffer = Unpooled.buffer();
        ColonyView.serializeNetworkData(colony, colonyBuffer, isNewSubscription);
    }

    @Override
    public void fromBytes(@NotNull final ByteBuf buf)
    {
        colonyId = StandardFactoryController.getInstance().readFromBuffer(buf);
        dimensionId = buf.readInt();
        isNewSubscription = buf.readBoolean();
        colonyBuffer = buf;
    }

    @Override
    public void toBytes(@NotNull final ByteBuf buf)
    {
        StandardFactoryController.getInstance().writeToBuffer(buf, colonyId);
        buf.writeInt(dimensionId);
        buf.writeBoolean(isNewSubscription);
        buf.writeBytes(colonyBuffer);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public IMessage onMessage(@NotNull final ColonyViewMessage message, final MessageContext ctx)
    {
        //Check if we are in the same dimension. Updates of a Colony that is not in our dimension are useless anyway.
        //Safety meassure since we should not be subscribed to these colonies anyway.
        if (FMLClientHandler.instance().getWorldClient().provider.getDimension() != dimensionId)
            return null;

        IColonyView view = IAPI.Holder.getApi().getClientColonyManager().getControllerForWorld(FMLClientHandler.instance().getWorldClient()).getColony(colonyId);
        if (view == null)
        {
            view = IAPI.Holder.getApi().getClientColonyManager().getControllerForWorld(FMLClientHandler.instance().getWorldClient()).createColony(colonyId);
        }

        return view.handleColonyViewMessage(colonyBuffer, isNewSubscription);
    }
}
