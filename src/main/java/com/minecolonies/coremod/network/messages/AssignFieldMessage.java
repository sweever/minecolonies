package com.minecolonies.coremod.network.messages;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.coremod.MineColonies;
import com.minecolonies.coremod.colony.buildings.BuildingFarmer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Message which handles the assignment of fields to farmers.
 */
public class AssignFieldMessage extends AbstractMessage<AssignFieldMessage, IMessage>
{

    private IToken   colonyId;
    private int      dimensionId;
    private BlockPos buildingId;
    private boolean  assign;
    private BlockPos field;

    /**
     * Empty standard constructor.
     */
    public AssignFieldMessage()
    {
        super();
    }

    /**
     * Creates the message to assign a field.
     *
     * @param building the farmer to assign to or release from.
     * @param assign   assign if true, free if false.
     * @param field    the field to assign or release.
     */
    public AssignFieldMessage(@NotNull final BuildingFarmer.View building, final boolean assign, final BlockPos field)
    {
        super();
        this.colonyId = building.getColony().getID();
        this.dimensionId = building.getColony().getDimension();
        this.buildingId = building.getLocation().getInDimensionLocation();
        this.assign = assign;
        this.field = field;
    }

    @Override
    public void fromBytes(@NotNull final ByteBuf buf)
    {
        colonyId = StandardFactoryController.getInstance().readFromBuffer(buf);
        dimensionId = buf.readInt();
        buildingId = BlockPosUtil.readFromByteBuf(buf);
        assign = buf.readBoolean();
        field = BlockPosUtil.readFromByteBuf(buf);
    }

    @Override
    public void toBytes(@NotNull final ByteBuf buf)
    {
        StandardFactoryController.getInstance().writeToBuffer(buf, colonyId);
        buf.writeInt(dimensionId);
        BlockPosUtil.writeToByteBuf(buf, buildingId);
        buf.writeBoolean(assign);
        BlockPosUtil.writeToByteBuf(buf, field);
    }

    @Override
    public void messageOnServerThread(final AssignFieldMessage message, final EntityPlayerMP player)
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

            @Nullable final IBuilding building = colony.getBuilding(message.buildingId, BuildingFarmer.class);
            if (building instanceof BuildingFarmer)
            {
                BuildingFarmer buildingFarmer = (BuildingFarmer) building;
                if (message.assign)
                {
                    buildingFarmer.assignField(message.field);
                }
                else
                {
                    buildingFarmer.freeField(message.field);
                }
            } else {
                MineColonies.getLogger().warn("Tried to set a field status on a Building that is not a farm: " + building);
            }
        }
    }
}

