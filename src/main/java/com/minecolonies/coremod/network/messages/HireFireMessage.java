package com.minecolonies.coremod.network.messages;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.client.colony.IBuildingView;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.coremod.colony.CitizenData;
import com.minecolonies.coremod.colony.buildings.AbstractBuildingWorker;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Message class which manages the messages hiring or firing of citizens.
 */
public class HireFireMessage extends AbstractMessage<HireFireMessage, IMessage>
{
    /**
     * The Colony ID.
     */
    private IToken colonyId;

    /**
     * The dimension of the colony.
     */
    private int dimensionId;

    /**
     * The buildings position.
     */
    private BlockPos buildingId;

    /**
     * If hiring (true) else firing.
     */
    private boolean hire;

    /**
     * The citizen to hire/fire.
     */
    private int citizenID;

    /**
     * Empty public constructor.
     */
    public HireFireMessage()
    {
        super();
    }

    /**
     * Creates object for the player to hire or fire a citizen.
     *
     * @param building  view of the building to read data from
     * @param hire      hire or fire the citizens
     * @param citizenID the id of the citizen to fill the job.
     */
    public HireFireMessage(@NotNull final IBuildingView building, final boolean hire, final int citizenID)
    {
        super();
        this.colonyId = building.getColony().getID();
        this.dimensionId = building.getColony().getDimension();
        this.buildingId = building.getLocation().getInDimensionLocation();
        this.hire = hire;
        this.citizenID = citizenID;
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
        buildingId = BlockPosUtil.readFromByteBuf(buf);
        hire = buf.readBoolean();
        citizenID = buf.readInt();
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
        BlockPosUtil.writeToByteBuf(buf, buildingId);
        buf.writeBoolean(hire);
        buf.writeInt(citizenID);
    }

    @Override
    public void messageOnServerThread(final HireFireMessage message, final EntityPlayerMP player)
    {
        final World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dimensionId);
        final IColony colony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(world).getColony(colonyId);
        if (colony != null)
        {
            //Verify player has permission to change this huts settings
            if (!colony.getPermissions().hasPermission(player, Action.MANAGE_HUTS))
            {
                return;
            }

            if (message.hire)
            {
                final CitizenData citizen = (CitizenData) colony.getCitizen(message.citizenID);
                ((AbstractBuildingWorker) colony.getBuilding(message.buildingId)).setWorker(citizen);
            }
            else
            {
                ((AbstractBuildingWorker) colony.getBuilding(message.buildingId)).setWorker(null);
            }
        }
    }
}
