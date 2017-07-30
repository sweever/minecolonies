package com.minecolonies.coremod.entity.ai.citizen.guard;

import com.minecolonies.api.util.CompatibilityUtils;
import com.minecolonies.api.util.constant.ToolType;
import com.minecolonies.coremod.colony.jobs.JobGuard;
import com.minecolonies.coremod.entity.ai.util.AIState;
import com.minecolonies.coremod.entity.ai.util.AITarget;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.coremod.entity.ai.util.AIState.*;

/**
 * Handles the AI of the guard entities.
 */
public class EntityAIRangeGuard extends AbstractEntityAIGuard implements IRangedAttackMob
{
    /**
     * Sets up some important skeleton stuff for every ai.
     *
     * @param job the job class
     */
    public EntityAIRangeGuard(@NotNull final JobGuard job)
    {
        super(job);
        super.registerTargets(
          new AITarget(GUARD_SEARCH_TARGET, this::searchTarget),
          new AITarget(GUARD_HUNT_DOWN_TARGET, this::huntDown),
          new AITarget(GUARD_GET_TARGET, this::getTarget),
          new AITarget(GUARD_PATROL, this::patrol),
          new AITarget(GUARD_RESTOCK, this::goToBuilding)
        );

        setupAbilities();
    }

    private void setupAbilities()
    {
        if (worker.getCitizenData() != null)
        {
            worker.setSkillModifier(2 * worker.getCitizenData().getIntelligence() + worker.getCitizenData().getStrength());
            worker.setCanPickUpLoot(true);
        }
    }

    @Override
    protected AIState searchTarget()
    {
        if (needsToolOrWeapon(ToolType.BOW))
            return AIState.GUARD_SEARCH_TARGET;
        else
            equipBow();

        return super.searchTarget();
    }

    private void equipBow()
    {
        int bowSlot = worker.findFirstSlotInInventoryWith(Items.BOW, -1);
        worker.setHeldItem(bowSlot);
    }

    /**
     * Follow the target and kill it.
     *
     * @return the next AIState.
     */
    protected AIState huntDown()
    {
        final double maxAttackDistance = 20.0D;

        chooseTarget();

        if (!targetEntity.isEntityAlive() || needsToolOrWeapon(ToolType.BOW))
        {
            stopHunting();
            return AIState.GUARD_GATHERING;
        }

        if (targetCanBeAttacked(maxAttackDistance))
        {
            attackEntityWithRangedAttack(targetEntity, 2);
            if (attacksExecuted >= getMaxAttacksUntilRestock())
                return AIState.GUARD_RESTOCK;
            return AIState.GUARD_HUNT_DOWN_TARGET;
        }

        final double maxRange = FOLLOW_RANGE + maxAttackDistance;
        if (shouldReturnToTarget(targetEntity.getPosition(), maxRange))
            return AIState.GUARD_PATROL;

        worker.setAIMoveSpeed((float) (1 + 0.25D * worker.getExperienceLevel()));
        worker.goToWorkSite(targetEntity.getPosition(), 3);

        return AIState.GUARD_SEARCH_TARGET;
    }

    private void chooseTarget()
    {
        if(canHuntDownLastAttacker())
            targetEntity = this.worker.getLastAttacker();
    }

    private void stopHunting()
    {
        targetEntity = null;
        worker.setAIMoveSpeed((float) 1.0D);
    }

    private boolean targetCanBeAttacked(double range)
    {
        boolean inLineOfSight = worker.getEntitySenses().canSee(targetEntity);
        boolean withinRange = worker.getDistanceToEntity(targetEntity) <= range;
        return inLineOfSight && withinRange;
    }

    private int getReloadTime()
    {
        final int baseReloadTime = 60;
        return baseReloadTime / (worker.getExperienceLevel() + 1);
    }

    @Override
    public void attackEntityWithRangedAttack(@NotNull final EntityLivingBase entityToAttack, final float baseDamage)
    {
        fireArrow(entityToAttack, baseDamage);
        faceTarget(entityToAttack);
        worker.swingArm(EnumHand.MAIN_HAND);
        worker.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, (float) getRandomPitch());
        worker.damageItemInHand(1);
        setDelay(getReloadTime());
        attacksExecuted += 1;
    }

    private void fireArrow(final EntityLivingBase entityToAttack, final float baseDamage)
    {
        final EntityTippedArrow arrowEntity = new GuardArrow(CompatibilityUtils.getWorld(this.worker), worker);

        final double xVector = entityToAttack.posX - worker.posX;
        final double yVector = entityToAttack.getEntityBoundingBox().minY + entityToAttack.height/3.0D - arrowEntity.posY;
        final double zVector = entityToAttack.posZ - worker.posZ;

        final double distance = (double) MathHelper.sqrt_double(xVector*xVector + zVector*zVector);
        double damage = baseDamage;

        final double hitChance = (worker.getExperienceLevel() + 1) / 15.0D;

        worker.resetActiveHand();
        final double heightCorrection = distance*0.2;
        arrowEntity.setThrowableHeading(xVector, yVector + heightCorrection, zVector, (float) 1.6D, (float) hitChance);

        if (worker.getHealth() <= 2)
            damage *= 2;

        addEffectsToArrow(arrowEntity, damage);
        CompatibilityUtils.getWorld(worker).spawnEntityInWorld(arrowEntity);
    }

    private void faceTarget(final EntityLivingBase entityToAttack)
    {
        worker.faceEntity(entityToAttack, 180F, 180F);
        worker.getLookHelper().setLookPositionWithEntity(entityToAttack, 180F, 180F);

        final double xDiff = targetEntity.posX - worker.posX;
        final double zDiff = targetEntity.posZ - worker.posZ;

        //0.01D is a small distance just to rotate worker
        final double goToX = xDiff > 0 ? 0.01D : -0.01D;
        final double goToZ = zDiff > 0 ? 0.01D : -0.01D;

        worker.moveEntity(goToX, 0, goToZ);
    }

    /*TODO:maybe move this into a util class*/
    private float getRandomPitch()
    {
        return (float)(1.0D / (worker.getRNG().nextDouble() * 0.4D + 0.8D));
    }

    /**
     * Method used to add potion/enchantment effects to the bow depending on his enchantments etc.
     *
     * @param arrowEntity the arrow to add these effects to.
     * @param baseDamage  the arrow base damage.
     */
    private void addEffectsToArrow(final EntityTippedArrow arrowEntity, final double baseDamage)
    {
        setDamage(arrowEntity, baseDamage);
        applyEnchantments(arrowEntity);
        applyPotionTip(arrowEntity);
    }

    private void setDamage(final EntityTippedArrow arrowEntity, final double baseDamage)
    {
        final double randomDmg = worker.getRandom().nextGaussian() * 0.25D;

        final double difficultyLevel = CompatibilityUtils.getWorld(worker).getDifficulty().getDifficultyId();
        final double difficultyDmg = difficultyLevel * 0.11D;

        arrowEntity.setDamage(baseDamage*2.0D + randomDmg + difficultyDmg);
    }

    private void applyEnchantments(final EntityTippedArrow arrowEntity)
    {
        final int powerLv = EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.POWER, worker);
        if (powerLv > 0)
            arrowEntity.setDamage(arrowEntity.getDamage() + (double) powerLv * 0.5D + 0.5D);

        final int punchLv = EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.PUNCH, worker);
        if (punchLv > 0)
            arrowEntity.setKnockbackStrength(punchLv);

        final DifficultyInstance difficulty = CompatibilityUtils.getWorld(worker).getDifficultyForLocation(new BlockPos(worker));
        boolean workerOnFire = worker.isBurning() && difficulty.func_190083_c() && worker.getRandom().nextBoolean();
        boolean hasFlameEnchant = EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.FLAME, worker) > 0;

        if (workerOnFire || hasFlameEnchant)
            arrowEntity.setFire(100);
    }

    private void applyPotionTip(final EntityTippedArrow arrowEntity)
    {
        final ItemStack holdItem = worker.getHeldItem(EnumHand.OFF_HAND);
        if (holdItem != null && holdItem.getItem() == Items.TIPPED_ARROW)
            arrowEntity.setPotionEffect(holdItem);
    }
}
