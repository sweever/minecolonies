package com.minecolonies.coremod.network.messages;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.client.colony.IColonyView;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.coremod.MineColonies;
import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Add or Update a AbstractBuilding.View to a ColonyView on the client.
 */
public class ColonyViewBuildingViewMessage implements IMessage, IMessageHandler<ColonyViewBuildingViewMessage, IMessage>
{

    private           IToken                colonyId;
    private int dimensionId;
    private           IToken             buildingId;
    private           BlockPos           buildingLocation;
    private           ByteBuf            buildingData;

    /**
     * Empty constructor used when registering the message.
     */
    public ColonyViewBuildingViewMessage()
    {
        super();
    }

    /**
     * Creates a message to handle colony views.
     *
     * @param building AbstractBuilding to add or update a view.
     */
    public ColonyViewBuildingViewMessage(@NotNull final AbstractBuilding building)
    {
        this.colonyId = building.getColony().getID();
        this.dimensionId = building.getColony().getDimension();
        this.buildingId = building.getID();
        this.buildingLocation = building.getLocation().getInDimensionLocation();
        this.buildingData = Unpooled.buffer();
        building.serializeToView(this.buildingData);
    }

    @Override
    public void fromBytes(@NotNull final ByteBuf buf)
    {
        colonyId = StandardFactoryController.getInstance().readFromBuffer(buf);
        dimensionId = buf.readInt();
        buildingId = StandardFactoryController.getInstance().readFromBuffer(buf);
        buildingLocation = BlockPosUtil.readFromByteBuf(buf);
        buildingData = Unpooled.buffer(buf.readableBytes());
        buf.readBytes(buildingData, buf.readableBytes());
    }

    @Override
    public void toBytes(@NotNull final ByteBuf buf)
    {
        StandardFactoryController.getInstance().writeToBuffer(buf, colonyId);
        buf.writeInt(dimensionId);
        StandardFactoryController.getInstance().writeToBuffer(buf, buildingId);
        BlockPosUtil.writeToByteBuf(buf, buildingLocation);
        buf.writeBytes(buildingData);
    }

    @Nullable
    @Override
    public IMessage onMessage(@NotNull final ColonyViewBuildingViewMessage message, final MessageContext ctx)
    {
        //Check if we are in the same dimension. Updates of a Colony that is not in our dimension are useless anyway.
        //Safety meassure since we should not be subscribed to these colonies anyway.
        if (FMLClientHandler.instance().getWorldClient().provider.getDimension() != dimensionId)
            return null;

        final IColonyView view = IAPI.Holder.getApi().getClientColonyManager().getControllerForWorld(FMLClientHandler.instance().getWorldClient()).getColony(colonyId);
        if (view != null)
        {
            return view.handleColonyBuildingViewMessage(buildingLocation, buildingId, message.buildingData);
        }
        else
        {
            MineColonies.getLogger().error(String.format("Colony view does not exist for ID #%s", colonyId));
            return null;
        }
    }
}
