package com.minecolonies.coremod;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.colony.management.IColonyManager;
import com.minecolonies.coremod.colony.Colony;
import com.minecolonies.coremod.colony.ColonyView;
import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.relauncher.Side;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The actual API implementation of the Minecolonies API
 */
public final class API implements IAPI
{
    public final static API INSTANCE = new API();

    private API()
    {
        if (INSTANCE != null)
        {
            throw new IllegalStateException("API Already created");
        }

        IAPI.Holder.setApi(this);
    }

    @NotNull
    @Override
    public IColonyManager<AbstractBuilding, Colony> getServerColonyManager()
    {
        return null;
    }

    @Nullable
    @Override
    public IColonyManager<AbstractBuilding.View, ColonyView> getClientColonyManager()
    {
        return null;
    }

    @NotNull
    @Override
    public IColonyManager getColonyManagerForSpecificSide(@NotNull final Side side)
    {
        return null;
    }

    @NotNull
    @Override
    public DamageSource getConsoleDamageSource()
    {
        return null;
    }

}
