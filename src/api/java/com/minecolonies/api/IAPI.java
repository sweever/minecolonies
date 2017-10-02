package com.minecolonies.api;

import com.minecolonies.api.client.colony.IBuildingView;
import com.minecolonies.api.client.colony.IColonyView;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.management.IColonyManager;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.relauncher.Side;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The API of Minecolonies
 */
public interface IAPI
{

    final class Holder
    {
        private static IAPI api;

        public static IAPI getApi()
        {
            return api;
        }

        public static void setApi(final IAPI api)
        {
            if (Holder.api != null)
            {
                throw new IllegalStateException("API Already initialized.");
            }

            Holder.api = api;
        }
    }

    /**
     * Method to get the {@link IColonyManager} for the server side.
     * Is never null as it exists on even when the client is running.
     *
     * @return The {@link IColonyManager} for the server side.
     */
    @NotNull
    IColonyManager<? extends IBuilding, ? extends IColony> getServerColonyManager();

    /**
     *
     * @return
     */
    @Nullable
    IColonyManager<? extends IBuildingView, ? extends IColonyView> getClientColonyManager();

    /**
     * Method to get the {@link IColonyManager} for a specific side.
     *
     * @param side the side the {@link IColonyManager} is requested for.
     * @return The {@link IColonyManager} for the specific side.
     */
    @NotNull
    IColonyManager getColonyManagerForSpecificSide(@NotNull Side side);

    @NotNull
    DamageSource getConsoleDamageSource();
}
