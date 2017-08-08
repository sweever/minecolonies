package com.minecolonies.coremod;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.colony.management.IColonyManager;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.relauncher.Side;
import org.jetbrains.annotations.NotNull;

/**
 * The actual com.minecolonies.coremod.API implementation of the Minecolonies com.minecolonies.coremod.API
 */
public final class API implements IAPI
{
    public final static API INSTANCE = new API();

    private static final DamageSource CONSOLE_SOURCE = new DamageSource("console").setDamageBypassesArmor().setDamageAllowedInCreativeMode().setDamageIsAbsolute();

    private API()
    {
        if (INSTANCE != null)
        {
            throw new IllegalStateException("com.minecolonies.coremod.API Already created");
        }

        IAPI.Holder.setApi(this);
    }

    @NotNull
    @Override
    public IColonyManager getColonyManager()
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
        return CONSOLE_SOURCE;
    }
}
