package com.minecolonies.api.colony.buildings;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.entity.Citizen;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Interface describing buildings that can be worked in.
 */
public interface IBuildingWorker<B extends IBuildingWorker> extends IBuilding<B>
{
    /**
     * Override this method if you want to keep some items in inventory.
     * When the inventory is full, everything get's dumped into the building chest.
     * But you can use this method to hold some stacks back.
     *
     * @param stack the stack to decide on
     * @return true if the stack should remain in inventory
     */
    boolean neededForWorker(@Nullable ItemStack stack);

    /**
     * Create a {@link IJob} for a newly assigned {@link Citizen} from its {@link ICitizenData}
     * @param citizen the data of the citizen
     */
    IJob createJob(ICitizenData citizen);
}
