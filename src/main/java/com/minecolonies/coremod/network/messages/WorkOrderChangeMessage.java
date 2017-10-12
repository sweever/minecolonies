package com.minecolonies.coremod.network.messages;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the WorkOrderChangeMessage which is responsible for changes in priority or removal of workOrders.
 */
public class WorkOrderChangeMessage extends AbstractMessage<WorkOrderChangeMessage, IMessage>
{
    /**
     * The Colony ID.
     */
    private IToken colonyId;

    /**
     * The dimension ID.
     */
    private int dimensionId;

    /**
     * The workOrder to remove or change priority.
     */
    private int workOrderId;

    /**
     * The priority.
     */
    private int priority;

    /**
     * Remove the workOrder or not.
     */
    private boolean removeWorkOrder;

    /**
     * Empty public constructor.
     */
    public WorkOrderChangeMessage()
    {
        super();
    }

    /**
     * Creates object for the player to hire or fire a citizen.
     *
     * @param building        view of the building to read data from
     * @param workOrderId     the workOrderId.
     * @param removeWorkOrder remove the workOrder?
     * @param priority        the new priority.
     */
    public WorkOrderChangeMessage(@NotNull final AbstractBuilding.View building, final int workOrderId, final boolean removeWorkOrder, final int priority)
    {
        super();
        this.colonyId = building.getColony().getID();
        this.dimensionId = building.getColony().getDimension();
        this.workOrderId = workOrderId;
        this.removeWorkOrder = removeWorkOrder;
        this.priority = priority;
    }

    /**
     * Transformation from a byteStream to the variables.
     *
     * @param buf the used byteBuffer.
     */
    @Override
    public void fromBytes(@NotNull final ByteBuf buf)
    {
        colonyId = StandardFactoryController.getInstance().readFromBuffer(buf);
        dimensionId = buf.readInt();
        workOrderId = buf.readInt();
        priority = buf.readInt();
        removeWorkOrder = buf.readBoolean();
    }

    /**
     * Transformation to a byteStream.
     *
     * @param buf the used byteBuffer.
     */
    @Override
    public void toBytes(@NotNull final ByteBuf buf)
    {
        StandardFactoryController.getInstance().writeToBuffer(buf, colonyId);
        buf.writeInt(dimensionId);
        buf.writeInt(workOrderId);
        buf.writeInt(priority);
        buf.writeBoolean(removeWorkOrder);
    }

    @Override
    public void messageOnServerThread(final WorkOrderChangeMessage message, final EntityPlayerMP player)
    {
        final World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dimensionId);
        final IColony colony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(world).getColony(colonyId);
        if (colony != null && colony.getPermissions().hasPermission(player, Action.ACCESS_HUTS))
        {
            //Verify player has permission to change this huts settings
            if (!colony.getPermissions().hasPermission(player, Action.MANAGE_HUTS))
            {
                return;
            }

            if (message.removeWorkOrder)
            {
                colony.getWorkManager().removeWorkOrder(message.workOrderId);
            }
            else
            {
                colony.getWorkManager().getWorkOrder(message.workOrderId).setPriority(message.priority);
            }
        }
    }
}


