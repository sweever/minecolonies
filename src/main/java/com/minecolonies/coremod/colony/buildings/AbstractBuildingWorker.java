package com.minecolonies.coremod.colony.buildings;

import com.minecolonies.api.client.colony.IColonyView;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuildingWorker;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.entity.Citizen;
import com.minecolonies.coremod.colony.CitizenData;
import com.minecolonies.coremod.colony.Colony;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The abstract class for each worker building.
 */
public abstract class AbstractBuildingWorker extends AbstractBuildingHut implements IBuildingWorker<AbstractBuilding>
{
    /**
     * Tag used to store the worker to nbt.
     */
    private static final String TAG_WORKER = "worker";

    /**
     * The citizenData of the assigned worker.
     */
    private ICitizenData worker;

    /**
     * The abstract constructor of the building.
     *
     * @param c the colony
     * @param l the position
     */
    public AbstractBuildingWorker(@NotNull final Colony c, final BlockPos l)
    {
        super(c, l);
    }

    @Override
    public ICitizenData getWorker()
    {
        return worker;
    }

    /**
     * Set the worker of the current building.
     *
     * @param citizen {@link CitizenData} of the worker
     */
    public void setWorker(final ICitizenData citizen)
    {
        if (worker == citizen)
        {
            return;
        }

        // If we have a worker, it no longer works here
        if (worker != null)
        {
            final Citizen tempCitizen = worker.getCitizen();
            worker.setWorkBuilding(null);
            if (tempCitizen != null)
            {
                tempCitizen.setLastJob(getJobName());
            }
        }

        worker = citizen;

        // If we set a worker, inform it of such
        if (worker != null)
        {
            final Citizen tempCitizen = citizen.getCitizen();
            if (tempCitizen != null && !tempCitizen.getLastJob().equals(getJobName()))
            {
                citizen.resetExperienceAndLevel();
            }
            worker.setWorkBuilding(this);
        }

        markDirty();
    }

    @Override
    public boolean neededForWorker(@Nullable final ItemStack stack)
    {
        return false;
    }

    /**
     * Returns the {@link net.minecraft.entity.Entity} of the worker.
     *
     * @return {@link net.minecraft.entity.Entity} of the worker
     */
    @Nullable
    public Citizen getWorkerEntity()
    {
        if (worker == null)
        {
            return null;
        }
        return worker.getCitizen();
    }

    @Override
    public void readFromNBT(@NotNull final NBTTagCompound compound)
    {
        super.readFromNBT(compound);

        if (compound.hasKey(TAG_WORKER))
        {
            // Bypass setWorker, which marks dirty
            worker = getColony().getCitizen(compound.getInteger(TAG_WORKER));
            if (worker != null)
            {
                worker.setWorkBuilding(this);
            }
        }
    }

    @Override
    public void writeToNBT(@NotNull final NBTTagCompound compound)
    {
        super.writeToNBT(compound);

        if (worker != null)
        {
            compound.setInteger(TAG_WORKER, worker.getId());
        }
    }

    @Override
    public void onDestroyed()
    {
        if (hasWorker())
        {
            // EntityCitizen will detect the workplace is gone and fix up it's
            // Entity properly
            removeCitizen(worker);
        }

        super.onDestroyed();
    }

    /**
     * Returns whether or not the building has a worker.
     *
     * @return true if building has worker, otherwise false.
     */
    public boolean hasWorker()
    {
        return worker != null;
    }

    @Override
    public void removeCitizen(final ICitizenData citizen)
    {
        if (isWorker(citizen))
        {
            setWorker(null);
        }
    }

    /**
     * Returns if the {@link CitizenData} is the same as {@link #worker}.
     *
     * @param citizen {@link CitizenData} you want to compare
     * @return true if same citizen, otherwise false
     */
    public boolean isWorker(final ICitizenData citizen)
    {
        return citizen == worker;
    }

    /**
     * The abstract method which returns the name of the job.
     *
     * @return the job name.
     */
    @NotNull
    public abstract String getJobName();

    /**
     * @see AbstractBuilding#onUpgradeComplete(int)
     */
    @Override
    public void onWorldTick(@NotNull final TickEvent.WorldTickEvent event)
    {
        super.onWorldTick(event);

        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        // If we have no active worker, grab one from the Colony
        // TODO Maybe the Colony should assign jobs out, instead?
        if (!hasWorker() && (getBuildingLevel() > 0 || this instanceof BuildingBuilder)
              && !this.getColony().isManualHiring())
        {
            final ICitizenData joblessCitizen = getColony().getJoblessCitizen();
            if (joblessCitizen != null)
            {
                setWorker(joblessCitizen);
            }
        }
    }

    @Override
    public void serializeToView(@NotNull final ByteBuf buf)
    {
        super.serializeToView(buf);

        buf.writeInt(worker == null ? 0 : worker.getId());
    }

    /**
     * Available skills of the citizens.
     */
    public enum Skill
    {
        STRENGTH,
        ENDURANCE,
        CHARISMA,
        INTELLIGENCE,
        DEXTERITY,
        PLACEHOLDER
    }

    /**
     * AbstractBuildingWorker View for clients.
     */
    public static class View extends AbstractBuildingHut.View
    {
        private int workerId;

        /**
         * Creates the view representation of the building.
         *
         * @param c the colony.
         * @param l the location.
         */
        public View(final IColonyView c, @NotNull final BlockPos l, @NotNull final IToken id)
        {
            super(c, l, id);
        }

        /**
         * Returns the id of the worker.
         *
         * @return 0 if there is no worker else the correct citizen id.
         */
        public int getWorkerId()
        {
            return workerId;
        }

        /**
         * Sets the id of the worker.
         *
         * @param workerId the id to set.
         */
        public void setWorkerId(final int workerId)
        {
            this.workerId = workerId;
        }

        @Override
        public void deserialize(@NotNull final ByteBuf buf)
        {
            super.deserialize(buf);

            workerId = buf.readInt();
        }

        @NotNull
        public Skill getPrimarySkill()
        {
            return Skill.PLACEHOLDER;
        }

        @NotNull
        public Skill getSecondarySkill()
        {
            return Skill.PLACEHOLDER;
        }
    }
}
