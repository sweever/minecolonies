package com.minecolonies.api.client.colony;

import com.minecolonies.api.client.IView;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IColonyView<B extends IBuildingView> extends IColony<B>, IView
{
    /**
     * Populate a ColonyView from the network data.
     *
     * @param buf               {@link ByteBuf} to read from.
     * @param isNewSubscription Whether this is a new subscription of not.
     * @return null == no response.
     */
    @Nullable
    IMessage handleColonyViewMessage(@NotNull ByteBuf buf, boolean isNewSubscription);

    /**
     * Update permissions.
     *
     * @param buf buffer containing permissions.
     * @return null == no response
     */
    @Nullable
    IMessage handlePermissionsViewMessage(@NotNull ByteBuf buf);

    /**
     * Update a ColonyView's workOrders given a network data ColonyView update
     * packet. This uses a full-replacement - workOrders do not get updated and
     * are instead overwritten.
     *
     * @param buf Network data.
     * @return null == no response.
     */
    @Nullable
    IMessage handleColonyViewWorkOrderMessage(ByteBuf buf);

    /**
     * Update a ColonyView's citizens given a network data ColonyView update
     * packet. This uses a full-replacement - citizens do not get updated and
     * are instead overwritten.
     *
     * @param id  ID of the citizen.
     * @param buf Network data.
     * @return null == no response.
     */
    @Nullable
    IMessage handleColonyViewCitizensMessage(int citizenId, ByteBuf buf);

    /**
     * Remove a citizen from the ColonyView.
     *
     * @param citizen citizen ID.
     * @return null == no response.
     */
    @Nullable
    IMessage handleColonyViewRemoveCitizenMessage(int citizen);

    /**
     * Remove a building from the ColonyView.
     *
     * @param buildingId location of the building.
     * @return null == no response.
     */
    @Nullable
    IMessage handleColonyViewRemoveBuildingMessage(BlockPos buildingId);

    /**
     * Remove a workOrder from the ColonyView.
     *
     * @param workOrderId id of the workOrder.
     * @return null == no response
     */
    @Nullable
    IMessage handleColonyViewRemoveWorkOrderMessage(int workOrderId);

    /**
     * Update a ColonyView's buildings given a network data ColonyView update
     * packet. This uses a full-replacement - buildings do not get updated and
     * are instead overwritten.
     *
     * @param buildingId location of the building.
     * @param buf        buffer containing ColonyBuilding information.
     * @return null == no response.
     */
    @Nullable
    IMessage handleColonyBuildingViewMessage(BlockPos buildingLocation, IToken buildingId, @NotNull ByteBuf buf);
}
