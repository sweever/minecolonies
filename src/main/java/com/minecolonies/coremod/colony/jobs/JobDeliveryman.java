package com.minecolonies.coremod.colony.jobs;

import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.Delivery;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.NBTUtils;
import com.minecolonies.coremod.client.render.RenderBipedCitizen;
import com.minecolonies.coremod.colony.CitizenData;
import com.minecolonies.coremod.entity.ai.basic.AbstractAISkeleton;
import com.minecolonies.coremod.entity.ai.citizen.deliveryman.EntityAIWorkDeliveryman;
import com.minecolonies.coremod.sounds.DeliverymanSounds;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

import static com.minecolonies.api.util.constant.Suppression.UNCHECKED;

/**
 * Class of the deliveryman job.
 */
public class JobDeliveryman extends AbstractJob
{
    private static final String TAG_CURRENT_TASK = "currentTask";
    private static final String TAG_RETURNING    = "returning";

    private LinkedList<IToken<?>> taskQueue = new LinkedList<>();

    private boolean returning;

    /**
     * Instantiates the job for the deliveryman.
     *
     * @param entity the citizen who becomes a deliveryman
     */
    public JobDeliveryman(final CitizenData entity)
    {
        super(entity);
    }

    @Override
    public void readFromNBT(@NotNull final NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        taskQueue.clear();
        if (compound.hasKey(TAG_CURRENT_TASK))
        {
            final NBTTagList queuItems = compound.getTagList(TAG_CURRENT_TASK, Constants.NBT.TAG_COMPOUND);
            NBTUtils.streamCompound(queuItems)
              .map(tokenCompound -> (IToken) StandardFactoryController.getInstance().deserialize(tokenCompound))
              .forEach(taskQueue::add);
        }

        returning = false;
        if (compound.hasKey(TAG_RETURNING))
        {
            returning = compound.getBoolean(TAG_RETURNING);
        }
    }

    @NotNull
    @Override
    public String getName()
    {
        return "com.minecolonies.coremod.job.Deliveryman";
    }

    @NotNull
    @Override
    public RenderBipedCitizen.Model getModel()
    {
        return RenderBipedCitizen.Model.DELIVERYMAN;
    }

    @Override
    public void writeToNBT(@NotNull final NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        final NBTTagList queuList = taskQueue.stream().map(iToken -> StandardFactoryController.getInstance().serialize(iToken)).collect(NBTUtils.toNBTTagList());
        compound.setTag(TAG_CURRENT_TASK, queuList);
        compound.setBoolean(TAG_RETURNING, returning);
    }

    /**
     * Generate your AI class to register.
     *
     * @return your personal AI instance.
     */
    @NotNull
    @Override
    public AbstractAISkeleton<JobDeliveryman> generateAI()
    {
        return new EntityAIWorkDeliveryman(this);
    }

    @Override
    public SoundEvent getBedTimeSound()
    {
        if (getCitizen() != null)
        {
            return getCitizen().isFemale() ? DeliverymanSounds.Female.offToBed : null;
        }
        return null;
    }

    @Nullable
    @Override
    public SoundEvent getBadWeatherSound()
    {
        if (getCitizen() != null)
        {
            return getCitizen().isFemale() ? DeliverymanSounds.Female.badWeather : null;
        }
        return null;
    }

    @Nullable
    @Override
    public SoundEvent getMoveAwaySound()
    {
        if (getCitizen() != null)
        {
            return getCitizen().isFemale() ? DeliverymanSounds.Female.hostile : null;
        }
        return null;
    }

    /**
     * Returns whether or not the job has a currentTask.
     *
     * @return true if has currentTask, otherwise false.
     */
    public boolean hasTask()
    {
        return !taskQueue.isEmpty() || returning;
    }

    /**
     * Returns the {@link IRequest} of the current Task.
     *
     * @return {@link IRequest} of the current Task.
     */
    @SuppressWarnings(UNCHECKED)
    public IRequest<Delivery> getCurrentTask()
    {
        if (taskQueue.isEmpty())
        {
            return null;
        }
        return getColony().getRequestManager().getRequestForToken(taskQueue.peekFirst());
    }

    /**
     * Method used to add a request to the queue
     *
     * @param token The token of the requests to add.
     */
    public void addRequest(@NotNull final IToken<?> token)
    {
        taskQueue.add(token);
    }

    /**
     * Method called to mark the current request as finished.
     *
     * @param successful True when the processing was successful, false when not.
     */
    public void finishRequest(final boolean successful)
    {
        if (taskQueue.isEmpty())
        {
            return;
        }

        this.setReturning(true);
        final IToken<?> current = taskQueue.removeFirst();

        getColony().getRequestManager().updateRequestState(current, successful ? RequestState.COMPLETED : RequestState.CANCELLED);
    }

    /**
     * Called when a task that is being scheduled is being canceled.
     *
     * @param token token of the task to be deleted.
     */
    public void onTaskDeletion(@NotNull final IToken<?> token)
    {
        if (taskQueue.contains(token))
        {
            if (taskQueue.peek().equals(token))
            {
                this.setReturning(true);
            }

            taskQueue.remove(token);
        }
    }

    /**
     * Method to get the task queue of this job.
     *
     * @return The task queue.
     */
    public List<IToken<?>> getTaskQueue()
    {
        return ImmutableList.copyOf(taskQueue);
    }

    /**
     * Method used to check if this DMan is trying to return to the warehouse to clean up.
     *
     * @return True when this DMan is returning the warehouse to clean his inventory.
     */
    public boolean getReturning()
    {
        return returning;
    }

    /**
     * Method used to set if this DMan needs to return and clear his inventory.
     * A set task is preferred over the returning flag.
     *
     * @param returning True to return the DMan to the warehouse and clean, false not to.
     */
    public void setReturning(final boolean returning)
    {
        this.returning = returning;
    }
}
