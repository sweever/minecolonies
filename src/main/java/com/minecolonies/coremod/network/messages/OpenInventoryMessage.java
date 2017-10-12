package com.minecolonies.coremod.network.messages;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.client.colony.IBuildingView;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.coremod.MineColonies;
import com.minecolonies.coremod.colony.CitizenDataView;
import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import com.minecolonies.coremod.entity.EntityCitizen;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Message sent to open an inventory.
 */
public class OpenInventoryMessage extends AbstractMessage<OpenInventoryMessage, IMessage>
{
    /***
     * The inventory name.
     */
    private String        name;
    /**
     * The inventory type.
     */
    private InventoryType inventoryType;
    /**
     * The entities id.
     */
    private int           entityID;
    /**
     * The position of the inventory block/entity.
     */
    private BlockPos      tePos;
    /**
     * The colony id the field or building etc is in.
     */
    private IToken        colonyId;

    /**
     * The dimension the colony is in.
     */
    private int dimensionId;

    /**
     * Empty public constructor.
     */
    public OpenInventoryMessage()
    {
        super();
    }

    /**
     * Creates an open inventory message for a citizen.
     *
     * @param citizen {@link CitizenDataView}
     */
    public OpenInventoryMessage(@NotNull final ICitizenData citizen)
    {
        super();
        inventoryType = InventoryType.INVENTORY_CITIZEN;
        name = citizen.getName();
        this.entityID = citizen.getEntityId();
    }

    /**
     * Creates an open inventory message for a building.
     *
     * @param building {@link AbstractBuilding.View}
     */
    public OpenInventoryMessage(@NotNull final IBuildingView building)
    {
        super();
        inventoryType = InventoryType.INVENTORY_CHEST;
        name = "";
        tePos = building.getLocation().getInDimensionLocation();
    }

    /**
     * Creates an open inventory message for a field.
     *
     * @param field    {@link AbstractBuilding.View}
     * @param colony the colony associated with the inventory.
     */
    public OpenInventoryMessage(final BlockPos field, final IColony colony)
    {
        super();
        inventoryType = InventoryType.INVENTORY_FIELD;
        name = "field";
        tePos = field;
        this.colonyId = colony.getID();
        this.dimensionId = colony.getDimension();
    }

    @Override
    public void fromBytes(@NotNull final ByteBuf buf)
    {
        inventoryType = InventoryType.values()[buf.readInt()];
        name = ByteBufUtils.readUTF8String(buf);
        switch (inventoryType)
        {
            case INVENTORY_CITIZEN:
                entityID = buf.readInt();
                break;
            case INVENTORY_CHEST:
                tePos = BlockPosUtil.readFromByteBuf(buf);
                break;
            case INVENTORY_FIELD:
                colonyId = StandardFactoryController.getInstance().readFromBuffer(buf);
                dimensionId = buf.readInt();
                tePos = BlockPosUtil.readFromByteBuf(buf);
        }
    }

    @Override
    public void toBytes(@NotNull final ByteBuf buf)
    {
        buf.writeInt(inventoryType.ordinal());
        ByteBufUtils.writeUTF8String(buf, name);
        switch (inventoryType)
        {
            case INVENTORY_CITIZEN:
                buf.writeInt(entityID);
                break;
            case INVENTORY_CHEST:
                BlockPosUtil.writeToByteBuf(buf, tePos);
                break;
            case INVENTORY_FIELD:
                StandardFactoryController.getInstance().writeToBuffer(buf, colonyId);
                buf.writeInt(dimensionId);
                BlockPosUtil.writeToByteBuf(buf, tePos);
                break;
        }
    }

    @Override
    public void messageOnServerThread(final OpenInventoryMessage message, final EntityPlayerMP player)
    {
        switch (message.inventoryType)
        {
            case INVENTORY_CITIZEN:
                doCitizenInventory(message, player);
                break;
            case INVENTORY_CHEST:
                doHutInventory(message, player);
                break;
            case INVENTORY_FIELD:
                doFieldInventory(message, player);
                break;
            default:
                break;
        }
    }

    private static void doCitizenInventory(final OpenInventoryMessage message, final EntityPlayerMP player)
    {
        @Nullable final EntityCitizen citizen = (EntityCitizen) player.world.getEntityByID(message.entityID);
        if (citizen != null && checkPermissions(citizen.getColony(), player))
        {
            if (!StringUtils.isNullOrEmpty(message.name))
            {
                citizen.getInventoryCitizen().setCustomName(message.name);
            }
            //TODO(OrionDevelopment): Convert next line to:
            //player.displayGUIChest(new IItemHandlerToIInventoryWrapper(citizen.getInventoryCitizen(), citizen.getInventoryCitizen()));
            player.displayGUIChest(citizen.getInventoryCitizen());
        }
    }

    private static void doHutInventory(final OpenInventoryMessage message, final EntityPlayerMP player)
    {
        final World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(message.dimensionId);
        final IColony colony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(world).getClosestColony(message.tePos);


        if (checkPermissions(colony, player))
        {
            @NotNull final TileEntityChest chest = (TileEntityChest) BlockPosUtil.getTileEntity(player.world, message.tePos);
            if (!StringUtils.isNullOrEmpty(message.name))
            {
                chest.setCustomName(message.name);
            }
            player.displayGUIChest(chest);
        }
    }

    private static void doFieldInventory(final OpenInventoryMessage message, final EntityPlayerMP player)
    {
        final World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(message.dimensionId);
        final IColony colony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(world).getClosestColony(message.tePos);

        if (checkPermissions(colony, player))
        {
            @NotNull final ItemStackHandler inventoryField = colony.getField(message.tePos).getInventoryField();
            if (!StringUtils.isNullOrEmpty(message.name))
            {
                // inventoryField.setCustomName(message.name);
            }
            player.openGui(MineColonies.instance, 1, player.getEntityWorld(), player.getPosition().getX(), player.getPosition().getY(), player.getPosition().getZ());
        }
    }

    private static boolean checkPermissions(final IColony colony, final EntityPlayerMP player)
    {
        //Verify player has permission to change this huts settings
        return colony.getPermissions().hasPermission(player, Action.MANAGE_HUTS);
    }

    /**
     * Type of inventory.
     */
    private enum InventoryType
    {
        INVENTORY_CITIZEN,
        INVENTORY_CHEST,
        INVENTORY_FIELD
    }
}
