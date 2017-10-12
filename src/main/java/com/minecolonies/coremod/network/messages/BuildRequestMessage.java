package com.minecolonies.coremod.network.messages;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.client.colony.IBuildingView;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Adds a entry to the builderRequired map.
 * Created: May 26, 2014
 *
 * @author Colton
 */
public class BuildRequestMessage extends AbstractMessage<BuildRequestMessage, IMessage>
{
    /**
     * The int mode for a build job.
     */
    public static final int BUILD  = 0;
    /**
     * The int mode for a repair job.
     */
    public static final int REPAIR = 1;
    /**
     * The id of the building.
     */
    private IToken buildingId;
    /**
     * The id of the colony.
     */
    private IToken   colonyId;
    /**
     * The id of the Dimension the build should take place in.
     */
    private int dimensionId;
    /**
     * The mode id.
     */
    private int      mode;

    /**
     * Empty constructor used when registering the message.
     */
    public BuildRequestMessage()
    {
        super();
    }

    /**
     * Creates a build request message.
     *
     * @param building AbstractBuilding of the request.
     * @param mode     Mode of the request, 1 is repair, 0 is build.
     */
    public BuildRequestMessage(@NotNull final IBuildingView building, final int mode)
    {
        super();
        this.colonyId = building.getColony().getID();
        this.dimensionId = building.getColony().getDimension();
        this.buildingId = building.getID();
        this.mode = mode;
    }

    @Override
    public void fromBytes(@NotNull final ByteBuf buf)
    {
        colonyId = StandardFactoryController.getInstance().readFromBuffer(buf);
        dimensionId = buf.readInt();
        buildingId = StandardFactoryController.getInstance().readFromBuffer(buf);
        mode = buf.readInt();
    }

    @Override
    public void toBytes(@NotNull final ByteBuf buf)
    {
        StandardFactoryController.getInstance().writeToBuffer(buf, colonyId);
        buf.writeInt(dimensionId);
        StandardFactoryController.getInstance().writeToBuffer(buf, buildingId);
        buf.writeInt(mode);
    }

    @Override
    public void messageOnServerThread(final BuildRequestMessage message, final EntityPlayerMP player)
    {
        final World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dimensionId);
        final IColony colony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(world).getColony(colonyId);

        if (colony == null)
        {
            return;
        }

        final IBuilding building = colony.getBuilding(message.buildingId);
        if (building == null)
        {
            return;
        }

        //Verify player has permission to change this huts settings
        if (!colony.getPermissions().hasPermission(player, Action.MANAGE_HUTS))
        {
            return;
        }

        if (building.hasWorkOrder())
        {
            building.removeWorkOrder();
        }
        else
        {
            switch (message.mode)
            {
                case BUILD:
                    building.requestUpgrade();
                    break;
                case REPAIR:
                    building.requestRepair();
                    break;
                default:
                    break;
            }
        }
    }
}
