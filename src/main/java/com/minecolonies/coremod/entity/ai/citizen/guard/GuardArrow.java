package com.minecolonies.coremod.entity.ai.citizen.guard;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.reference.ModAchievements;
import com.minecolonies.api.util.Log;
import com.minecolonies.coremod.entity.EntityCitizen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Arrow class for arrows shot by guards.
 */
public class GuardArrow extends EntityTippedArrow
{
    private static final String TAG_COLONY = "colony";
    private IColony colony;

    /**
     * Constructor for forge.
     *
     * @param worldin the world this is in.
     */
    public GuardArrow(final World worldin)
    {
        super(worldin);
    }

    /**
     * Create a new Arrow.
     *
     * @param worldIn the world this is shot in.
     * @param shooter the guard shooting
     */
    public GuardArrow(final World worldIn, final EntityCitizen shooter)
    {
        super(worldIn, shooter);
        this.colony = shooter.getColony();
    }

    @Override
    public void writeEntityToNBT(final NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setTag(TAG_COLONY, StandardFactoryController.getInstance().serialize(colony.getID()));
    }

    @Override
    public void readEntityFromNBT(final NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);

        NBTBase colonyIdNBTBase = compound.getTag(TAG_COLONY);
        IToken colonyId;

        if (colonyIdNBTBase instanceof NBTTagCompound) {
            colonyId = StandardFactoryController.getInstance().deserialize((NBTTagCompound) colonyIdNBTBase);
        } else {
            this.setDead();
            return;
        }
        colony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(getEntityWorld()).getColony(colonyId);
    }

    @Override
    protected void arrowHit(final EntityLivingBase targetEntity)
    {
        super.arrowHit(targetEntity);
        Log.getLogger().info("Arrow hit " + targetEntity + " with " + targetEntity.getHealth());
        if (targetEntity.getHealth() <= 0.0F)
        {
            if (targetEntity instanceof EntityPlayer)
            {
                final EntityPlayer player = (EntityPlayer) targetEntity;
                if (colony.getPermissions().isColonyMember(player))
                {
                    this.colony.triggerAchievement(ModAchievements.achievementPlayerDeathGuard);
                }
            }
            colony.incrementStatistic("mobs");
        }
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof GuardArrow))
        {
            return false;
        }
        if (!super.equals(o))
        {
            return false;
        }

        final GuardArrow that = (GuardArrow) o;
        return colony == null ? (that.colony != null) : colony.equals(that.colony);
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + (colony != null ? colony.hashCode() : 0);
        return result;
    }
}
