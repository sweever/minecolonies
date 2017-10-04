package com.minecolonies.coremod.network.messages;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.client.colony.IColonyView;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.coremod.MineColonies;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Message to execute the renaiming of the townHall.
 */
public class TownHallRenameMessage extends AbstractMessage<TownHallRenameMessage, IMessage>
{
    private static final int MAX_NAME_LENGTH  = 25;
    private static final int SUBSTRING_LENGTH = MAX_NAME_LENGTH - 1;
    private IToken colonyId;
    private int dimensionId;
    private String name;

    /**
     * Empty public constructor.
     */
    public TownHallRenameMessage()
    {
        super();
    }

    /**
     * Object creation for the town hall rename message.
     *
     * @param colony Colony the rename is going to occur in.
     * @param name   New name of the town hall.
     */
    public TownHallRenameMessage(@NotNull final IColonyView colony, final String name)
    {
        super();
        this.colonyId = colony.getID();
        this.dimensionId = colony.getDimension();
        this.name = (name.length() <= MAX_NAME_LENGTH) ? name : name.substring(0, SUBSTRING_LENGTH);
    }

    @Override
    public void fromBytes(@NotNull final ByteBuf buf)
    {
        colonyId = StandardFactoryController.getInstance().readFromBuffer(buf);
        name = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(@NotNull final ByteBuf buf)
    {
        StandardFactoryController.getInstance().writeToBuffer(buf, colonyId);
        ByteBufUtils.writeUTF8String(buf, name);
    }

    @Override
    public void messageOnServerThread(final TownHallRenameMessage message, final EntityPlayerMP player)
    {
        final World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dimensionId);
        final IColony colony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(world).getColony(message.colonyId);
        if (colony != null)
        {
            //Verify player has permission to change this huts settings
            if (!colony.getPermissions().hasPermission(player, Action.MANAGE_HUTS))
            {
                return;
            }
            message.name = (message.name.length() <= MAX_NAME_LENGTH) ? message.name : message.name.substring(0, SUBSTRING_LENGTH);
            colony.setName(message.name);
            MineColonies.getNetwork().sendToAll(message);
        }
    }
}
