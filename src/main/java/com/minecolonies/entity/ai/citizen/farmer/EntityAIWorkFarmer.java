package com.minecolonies.entity.ai.citizen.farmer;

import com.minecolonies.blocks.BlockHutField;
import com.minecolonies.colony.Colony;
import com.minecolonies.colony.buildings.BuildingFarmer;
import com.minecolonies.colony.jobs.JobFarmer;
import com.minecolonies.entity.EntityCitizen;
import com.minecolonies.entity.ai.basic.AbstractEntityAIInteract;
import com.minecolonies.entity.ai.util.AIState;
import com.minecolonies.entity.ai.util.AITarget;
import com.minecolonies.util.BlockUtils;
import com.minecolonies.util.InventoryUtils;
import com.minecolonies.util.Utils;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.IPlantable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.minecolonies.entity.ai.util.AIState.*;

import java.util.List;

/**
 * Farmer AI class.
 * Created: December 20, 2014
 */
public class EntityAIWorkFarmer extends AbstractEntityAIInteract<JobFarmer>
{
    /**
     * The standard delay the farmer should have.
     */
    private static final int     STANDARD_DELAY      = 7;
    /**
     * The bonus the farmer gains each update is level/divider.
     */
    private static final int     DELAY_DIVIDER       = 10;
    /**
      * The EXP Earned per harvest.
      */
    private static final double  XP_PER_HARVEST      = 0.5;
    /**
     * How long to wait after looking to decide what to do.
     */
    private static final int     LOOK_WAIT           = 100;
    /**
     * How long to wait after a cycle to run another.
     */
    private static final int     CYCLE_WAIT          = 2400;
    /**
     * How long to cut from CYCLE_WAIT per field.
     */
    private static final int     FIELD_COMP          = 240;
    /**
     * Changed after finished harvesting in order to dump the inventory.
     */
    private              boolean shouldDumpInventory = false;

    /**
     * The offset to work at relative to the scarecrow.
     */
    @Nullable
    private BlockPos workingOffset;

    /**
     * Defines if the farmer should request seeds for the current field.
     */
    private boolean requestSeeds = true;

    /**
     * Defines if the farmer should try to get the seeds from his chest.
     */
    private boolean shouldTryToGetSeed = true;

    /**
     * Variables used in handleOffset
     */
    private int totalDis;
    private int dist;
    private boolean horizontal;

    /**
     * Constructor for the Farmer.
     * Defines the tasks the Farmer executes.
     *
     * @param job a farmer job to use.
     */
    public EntityAIWorkFarmer(@NotNull JobFarmer job)
    {
        super(job);
        super.registerTargets(
          new AITarget(IDLE, () -> START_WORKING),
          new AITarget(START_WORKING, this::startWorkingAtOwnBuilding),
          new AITarget(PREPARING, this::prepareForFarming),
          new AITarget(FARMER_INITIALIZE, this::initialize),
          new AITarget(FARMER_OBSERVE, this::lookAtField),
          new AITarget(FARMER_WORK, this::cycle)
        );
        worker.setSkillModifier(2 * worker.getCitizenData().getEndurance() + worker.getCitizenData().getCharisma());
        worker.setCanPickUpLoot(true);
    }

    /**
     * Redirects the farmer to his building.
     *
     * @return the next state.
     */
    private AIState startWorkingAtOwnBuilding()
    {
        if (walkToBuilding())
        {
            return getState();
        }
        return PREPARING;
    }

    /**
     * Prepares the farmer for farming.
     * Also requests the tools and checks if the farmer has sufficient fields.
     *
     * @return the next AIState
     */
    @NotNull
    private AIState prepareForFarming()
    {
        @Nullable final BuildingFarmer building = getOwnBuilding();

        if (building == null || building.getBuildingLevel() < 1)
        {
            return AIState.PREPARING;
        }

        building.syncWithColony(world);

        if (building.getFarmerFields().size() < getOwnBuilding().getBuildingLevel() && !building.assignManually())
        {
            searchAndAddFields();
        }

        if (building.hasNoFields())
        {
            chatSpamFilter.talkWithoutSpam("entity.farmer.noFreeFields");
            return AIState.PREPARING;
        }

        //If the farmer has no currentField and there is no field which needs work, check fields.
        if (building.getCurrentField() == null && building.getFieldToWorkOn() == null)
        {
            building.resetFields();
            walkToBuilding();
            setDelay(CYCLE_WAIT - (building.getFarmerFields().size()*FIELD_COMP));
            return AIState.IDLE;
        }

        @Nullable final Field currentField = building.getCurrentField();

        if (currentField.needsWork())
        {
            if (currentField.isInitialized())
            {
                walkToBlock(currentField.getLocation());
                return AIState.FARMER_OBSERVE;
            }
            else if (canGoPlanting(currentField, building) && !checkForHoe())
            {
                return walkToBlock(currentField.getLocation()) ? AIState.PREPARING : AIState.FARMER_INITIALIZE;
            }
            else if (containsPlants(currentField))
            {
                currentField.setInitialized(true);
                currentField.setNeedsWork(false);
            }
        }
        else
        {
            getOwnBuilding().setCurrentField(null);
        }
        return AIState.PREPARING;
    }

    /**
     * Searches and adds a field that has not been taken yet for the farmer and then adds it to the list.
     */
    private void searchAndAddFields()
    {
        final Colony colony = worker.getColony();
        if (colony != null)
        {
            @Nullable final Field newField = colony.getFreeField(worker.getName());

            if (newField != null && getOwnBuilding() != null)
            {
                getOwnBuilding().addFarmerFields(newField);
            }
        }
    }

    /**
     * Checks if the farmer is ready to plant.
     *
     * @param currentField the field to plant.
     * @return true if he is ready.
     */
    private boolean canGoPlanting(@NotNull final Field currentField, @NotNull final BuildingFarmer buildingFarmer)
    {
        if (currentField.getSeed() == null)
        {
            chatSpamFilter.talkWithoutSpam("entity.farmer.noSeedSet");
            buildingFarmer.setCurrentField(null);
            return false;
        }

        if (shouldTryToGetSeed)
        {
            final int slot = worker.findFirstSlotInInventoryWith(currentField.getSeed());
            final ItemStack seeds = new ItemStack(currentField.getSeed());
            if (slot != -1)
            {
                requestSeeds = false;
            }
            if (!walkToBuilding())
            {
                if (isInHut(seeds))
                {
                    requestSeeds = false;
                    isInHut(seeds);
                }
                shouldTryToGetSeed = requestSeeds;
                if (requestSeeds)
                {
                    chatSpamFilter.talkWithoutSpam("entity.farmer.NeedSeed", currentField.getSeed().getItemStackDisplayName(seeds));
                }
            }
        }

        return !shouldTryToGetSeed;
    }

    /**
      * The main work cycle of the Famer.
      * This checks each block, harvests, tills, and plants.
      */
    private AIState cycle()
    {
        @Nullable final BuildingFarmer buildingFarmer = getOwnBuilding();

        if (buildingFarmer == null || checkForHoe() || buildingFarmer.getCurrentField() == null)
        {
            return AIState.PREPARING;
        }

        @Nullable final Field field = buildingFarmer.getCurrentField();

        if (workingOffset != null)
        {
            final BlockPos position = field.getLocation().down().south(workingOffset.getZ()).east(workingOffset.getX());
            // Still moving to the block
            if (walkToBlock(position.up()))
            {
                return AIState.FARMER_WORK;
            }

            // harvest the block if able to.
            if (harvestIfAble(position))
            {
                setDelay(STANDARD_DELAY - this.worker.getLevel() / DELAY_DIVIDER);
            }
        }

        if (!handleOffsetHarvest(field))
        {
            resetVariables();
            shouldDumpInventory = true;
            field.setNeedsWork(false);
            return AIState.IDLE;
        }
        return AIState.FARMER_WORK;
    }

    /**
      * This (re)initializes a field.
      * Checks the block above to see if it is a plant, if so, breaks it. Then tills.
      */
    private AIState initialize()
    {
        @Nullable final BuildingFarmer buildingFarmer = getOwnBuilding();

        if (buildingFarmer == null || checkForHoe() || buildingFarmer.getCurrentField() == null)
        {
            return AIState.PREPARING;
        }

        @Nullable final Field field = buildingFarmer.getCurrentField();

        if (workingOffset != null)
        {
            final BlockPos position = field.getLocation().down().south(workingOffset.getZ()).east(workingOffset.getX());
            // Still moving to the block
            if (walkToBlock(position.up()))
            {
                return AIState.FARMER_INITIALIZE;
            }

            // Check to see if the block is a plant, and if it is, break it.
            final IBlockState blockState = world.getBlockState(position.up());

            if (blockState.getBlock() instanceof IGrowable || blockState.getBlock() instanceof BlockCrops)
            {
                mineBlock(position.up());
                setDelay(STANDARD_DELAY - this.worker.getLevel() / DELAY_DIVIDER);
                return AIState.FARMER_INITIALIZE;
            }

            // hoe the block if able to.
            if (hoeIfAble(position, field))
            {
                setDelay(STANDARD_DELAY - this.worker.getLevel() / DELAY_DIVIDER);
                return AIState.FARMER_INITIALIZE;
            }

            if (shouldPlant(position, field) && !plantCrop(field.getSeed(), position))
            {
                resetVariables();
                return AIState.PREPARING;
            }
        }

        if (!handleOffset(field))
        {
            resetVariables();
            shouldDumpInventory = true;
            field.setInitialized(true);
            field.setNeedsWork(false);
            return AIState.IDLE;
        }

        return AIState.FARMER_INITIALIZE;
    }

    /**
      * Farmer looks at field to see if it's harvestable.
      * Checks to see if there are any harvestable crops,
      * if so go to FARMER_WORK, if not, set needs work to false and go to IDLE.
      */
    private AIState lookAtField()
    {
        @Nullable final BuildingFarmer buildingFarmer = getOwnBuilding();

        if (buildingFarmer == null || checkForHoe() || buildingFarmer.getCurrentField() == null)
        {
            return AIState.PREPARING;
        }

        @Nullable final Field field = buildingFarmer.getCurrentField();

        setDelay(LOOK_WAIT);
        if (handleOffsetHarvest(field))
        {
            return AIState.FARMER_WORK;
        }
        else
        {
            if (containsPlants(field))
            {
                field.setNeedsWork(false);
                return AIState.PREPARING;
            }
            else
            {
                field.setInitialized(false);
                return AIState.PREPARING;
            }
        }
    }

    /**
     * Checks to see if field contains plants.
     *
     * @param field the field to check.
     * @return Boolean if there were plants found.
     */
    private boolean containsPlants(final Field field)
    {
        BlockPos position;
        IBlockState blockState;

        while (handleOffset(field))
        {
            // Check to see if the block is a plant, and if it is, break it.
            position = field.getLocation().down().south(workingOffset.getZ()).east(workingOffset.getX());
            blockState = world.getBlockState(position.up());

            if (blockState.getBlock() instanceof IGrowable || blockState.getBlock() instanceof BlockCrops)
            {
                return true;
            }
        }
        return false;
    }

    /**
      * Checks if we can harvest, and does so if we can.
      *
      * @return true if we harvested.
      */
    private boolean harvestIfAble(final BlockPos position)
    {
        if (shouldHarvest(position))
        {
            worker.addExperience(XP_PER_HARVEST);
            return harvestCrop(position.up());
        }
        return false;
    }

    /**
      * Checks if we can hoe, and does so if we can.
      *
      * @param position the position to check
      * @param field the field that we are working with.
      */
    private boolean hoeIfAble(final BlockPos position, final Field field)
    {
        if (shouldHoe(position, field))
        {
            equipHoe();
            worker.swingArm(worker.getActiveHand());
            world.setBlockState(position, Blocks.FARMLAND.getDefaultState());
            worker.damageItemInHand(1);
            mineBlock(position.up());
            return true;
        }
        return false;
    }

    /**
     * Checks if the ground should be hoed and the block above removed.
     *
     * @param position the position to check.
     * @param field    the field close to this position.
     * @return true if should be hoed.
     */
    private boolean shouldHoe(@NotNull final BlockPos position, @NotNull final Field field)
    {
        return !field.isNoPartOfField(world, position)
                 && !BlockUtils.isBlockSeed(world, position.up())
                 && !(world.getBlockState(position).getBlock() instanceof BlockHutField)
                 && (world.getBlockState(position).getBlock() == Blocks.DIRT || world.getBlockState(position).getBlock() == Blocks.GRASS);
    }

    /**
     * Sets the hoe as held item.
     */
    private void equipHoe()
    {
        worker.setHeldItem(getHoeSlot());
    }

    /**
     * Handles the offset of the field for the farmer.
     *
     * @param field the field object.
     * @return true if successful.
     */
    private boolean handleOffset(@NotNull final Field field)
    {
        if (workingOffset == null)
        {
            workingOffset = new BlockPos(0, 0,0);
            totalDis = 1;
            dist = 0;
            horizontal = true;
        }
        else
        {
            if (workingOffset.getZ() >= field.getWidthPlusZ() && workingOffset.getX() <= -field.getLengthMinusX())
            {
                workingOffset = null;
                return false;
            }
            else
            {
                if (totalDis == dist)
                {
                    horizontal = !horizontal;
                    dist = 0;
                    if (horizontal)
                    {
                        totalDis++;
                    }
                }
                if (horizontal)
                {
                    workingOffset = new BlockPos(workingOffset.getX(),0,workingOffset.getZ()-Math.pow(-1,totalDis));
                }
                else
                {
                    workingOffset = new BlockPos(workingOffset.getX()-Math.pow(-1,totalDis),0,workingOffset.getZ());
                }
                dist++;
            }

            /*
            else if (
                        (
                            //If we're checking an even row
                            ((field.getLengthPlusX() - absZ) % 2 == 0)
                            && workingOffset.getX() >= field.getLengthPlusX()
                        )
                        ||
                        (
                            //If we're checking an odd row
                            ((field.getLengthPlusX() - absZ) % 2 == 1)
                            && workingOffset.getX() <= -field.getLengthMinusX()
                        )
                    )
            {
                workingOffset = new BlockPos(workingOffset.getX(), 0, workingOffset.getZ() + 1);
            }
            else if ((field.getLengthPlusX() - absZ) % 2 == 0)
            {
                workingOffset = new BlockPos(workingOffset.getX() + 1, 0, workingOffset.getZ());
            }
            else
            {
                workingOffset = new BlockPos(workingOffset.getX() - 1, 0, workingOffset.getZ());
            }*/
        }
        return true;
    }

    /**
     * Handles the offset of the field for the farmer.
     * Skips to the next harvestable crop, returns true if one was found.
     *
     * @param field the field object.
     * @return true if a harvestable crop was found.
     */
    private boolean handleOffsetHarvest(@NotNull final Field field)
    {
        if (workingOffset == null)
        {
            handleOffset(field);
        }

        BlockPos position = field.getLocation().down().south(workingOffset.getZ()).east(workingOffset.getX());

        while (!shouldHarvest(position))
        {
            if (!handleOffset(field))
            {
                return false;
            }
            position = field.getLocation().down().south(workingOffset.getZ()).east(workingOffset.getX());
        }
        return true;
    }

    /**
     * Resets the basic variables of the class.
     */
    private void resetVariables()
    {
        requestSeeds = true;
        shouldTryToGetSeed = true;
    }

    /**
     * Get's the slot in which the hoe is in.
     *
     * @return slot number
     */
    private int getHoeSlot()
    {
        return InventoryUtils.getFirstSlotContainingTool(getInventory(), Utils.HOE);
    }

    /**
     * Checks if the ground should be planted.
     *
     * @param position the position to check.
     * @param field    the field close to this position.
     * @return true if should be hoed.
     */
    private boolean shouldPlant(@NotNull final BlockPos position, @NotNull final Field field)
    {
        @Nullable final ItemStack itemStack = BlockUtils.getItemStackFromBlockState(world.getBlockState(position.up()));

        if (itemStack != null && itemStack.getItem() == field.getSeed())
        {
            requestSeeds = false;
        }

        return !field.isNoPartOfField(world, position) && !(world.getBlockState(position.up()).getBlock() instanceof BlockCrops)
                 && !(world.getBlockState(position).getBlock() instanceof BlockHutField) && world.getBlockState(position).getBlock() == Blocks.FARMLAND;
    }

    /**
     * Plants the crop at a given location.
     *
     * @param item     the crop.
     * @param position the location.
     */
    private boolean plantCrop(Item item, @NotNull BlockPos position)
    {
        final int slot = worker.findFirstSlotInInventoryWith(item);
        if (slot == -1)
        {
            return false;
        }
        else
        {
            @NotNull final IPlantable seed = (IPlantable) item;
            world.setBlockState(position.up(), seed.getPlant(world, position));
            getInventory().decrStackSize(slot, 1);
            requestSeeds = false;
            //Flag 1+2 is needed for updates
            return true;
        }
    }

    /**
     * Checks if the crop should be harvested.
     *
     * @param position the position to check.
     * @return true if should be hoed.
     */
    private boolean shouldHarvest(@NotNull final BlockPos position)
    {
        final IBlockState state = world.getBlockState(position.up());

        if (state.getBlock() instanceof IGrowable && state.getBlock() instanceof BlockCrops)
        {
            @NotNull final BlockCrops block = (BlockCrops) state.getBlock();
            return !block.canGrow(world, position.up(), state, false);
        }

        return false;
    }

    /**
     * Called to check when the InventoryShouldBeDumped.
     *
     * @return true if the conditions are met
     */
    @Override
    protected boolean wantInventoryDumped()
    {
        if (shouldDumpInventory)
        {
            shouldDumpInventory = false;
            return true;
        }
        return false;
    }

    /**
     * Returns the farmer's work building.
     *
     * @return building instance
     */
    @Override
    protected BuildingFarmer getOwnBuilding()
    {
        return (BuildingFarmer) worker.getWorkBuilding();
    }

    /**
     * Override this method if you want to keep some items in inventory.
     * When the inventory is full, everything get's dumped into the building chest.
     * But you can use this method to hold some stacks back.
     *
     * @param stack the stack to decide on
     * @return true if the stack should remain in inventory
     */
    @Override
    protected boolean neededForWorker(@Nullable final ItemStack stack)
    {
        return stack != null && Utils.isHoe(stack);
    }

    /**
     * Returns the farmer's worker instance. Called from outside this class.
     *
     * @return citizen object
     */
    @Nullable
    public EntityCitizen getCitizen()
    {
        return worker;
    }

    /**
      * This method allows us to harvest crops and turn the block beneath to dirt.
      *
      * @params position the position of the crop to harvest
      */
    private boolean harvestCrop(final BlockPos position)
    {
        final IBlockState curBlockState = world.getBlockState(position);

        if (!(curBlockState.getBlock() instanceof IGrowable) || !(curBlockState.getBlock() instanceof BlockCrops))
        {
            return false;
        }

        final BlockCrops crops = (BlockCrops) curBlockState.getBlock();

        if (!crops.isMaxAge(curBlockState))
        {
            return false;
        }

        final ItemStack tool = worker.getHeldItemMainhand();

        //calculate fortune enchantment
        final int fortune = Utils.getFortuneOf(tool);

        final List<ItemStack> drops = crops.getDrops(world, position, curBlockState, fortune);

        world.setBlockToAir(position);

        //add the drops to the citizen
        for (final ItemStack item : drops)
        {
            InventoryUtils.setStack(worker.getInventoryCitizen(), item);
        }

        //set the block below to dirt.
        world.setBlockState(position.down(), Blocks.DIRT.getDefaultState());

        this.incrementActionsDone();
        return true;

    }
}
