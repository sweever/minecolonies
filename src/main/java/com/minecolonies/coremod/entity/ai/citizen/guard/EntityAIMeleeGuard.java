package com.minecolonies.coremod.entity.ai.citizen.guard;

import com.minecolonies.api.compatibility.Compatibility;
import com.minecolonies.api.util.InventoryFunctions;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.constant.ToolType;
import com.minecolonies.coremod.colony.jobs.JobGuard;
import com.minecolonies.coremod.entity.ai.util.AIState;
import com.minecolonies.coremod.entity.ai.util.AITarget;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static com.minecolonies.coremod.entity.ai.util.AIState.*;

/**
 * Handles the AI of the guard entities.
 */
public class EntityAIMeleeGuard extends AbstractEntityAIGuard
{
    //Movement
    private static final float BASE_FOLLOW_SPEED = 1;
    private static final float BASE_FOLLOW_SPEED_MULTIPLIER = 0.25F;

    /** Quantity to be moved to rotate the entity without actually moving. */
    private static final double MOVE_MINIMAL = 0.01D;
    private static final double MAX_INSTANT_ROTATION = 180D;

    //Attack
    private static final double MIN_ATTACK_DISTANCE = 2.0D;
    private static final double DAMAGE_PER_ATTACK = 0.5;
    private static final int EXP_PER_MOD_DEATH = 15;
    private static final int BASE_RELOAD_TIME = 30;
    private static final int FIRE_CHANCE_MULTIPLIER = 4;

    //Sound
    private static final double BASIC_VOLUME = 1.0D;

    private static final double PITCH_DIVIDER = 1.0D;
    private static final double BASE_PITCH = 0.8D;
    private static final double PITCH_MULTIPLIER = 0.4D;

    /**
     * Sets up some important skeleton stuff for every ai.
     *
     * @param job the job class
     */
    public EntityAIMeleeGuard(@NotNull final JobGuard job)
    {
        super(job);
        super.registerTargets(
          new AITarget(GUARD_SEARCH_TARGET, this::searchTarget),
          new AITarget(GUARD_GET_TARGET, this::getTarget),
          new AITarget(GUARD_HUNT_DOWN_TARGET, this::huntDown),
          new AITarget(GUARD_PATROL, this::patrol),
          new AITarget(GUARD_RESTOCK, this::goToBuilding)
        );

        setupAbilities();
    }

    private void setupAbilities()
    {
        if (worker.getCitizenData() != null)
        {
            final int strength = worker.getCitizenData().getStrength();
            final int endurance = worker.getCitizenData().getEndurance();
            final int modifier = 2*strength + endurance;
            worker.setSkillModifier(modifier);
            worker.setCanPickUpLoot(true);
        }
    }

    @Override
    protected AIState searchTarget()
    {
        if (needsToolOrWeapon(ToolType.SWORD))
            return AIState.GUARD_SEARCH_TARGET;
        equipSword();
        return super.searchTarget();
    }

    private void equipSword()
    {
        final Predicate<ItemStack> isValidSword = stack -> !ItemStackUtils.isEmpty(stack) &&
                                                            ItemStackUtils.doesItemServeAsWeapon(stack);
        //TODO this is not a good way to do this
        InventoryFunctions.matchFirstInProviderWithSimpleAction(worker, isValidSword, worker::setHeldItem);
    }

    /**
     * Follow the target and kill it.
     *
     * @return the next AIState.
     */
    protected AIState huntDown()
    {
        if(worker.getColony() == null)
            return AIState.GUARD_GATHERING;

        chooseTarget();

        if (!targetEntity.isEntityAlive() || needsToolOrWeapon(ToolType.SWORD))
        {
            stopHunting();
            return AIState.GUARD_GATHERING;
        }

        if (targetCanBeAttacked())
        {
            boolean killedEnemy = attackEntity(targetEntity, (float) DAMAGE_PER_ATTACK);

            if (killedEnemy)
                return AIState.GUARD_GATHERING;

            if (shouldRestock())
                return AIState.GUARD_RESTOCK;

            return AIState.GUARD_HUNT_DOWN_TARGET;
        }

        if (shouldReturnToTarget(targetEntity.getPosition(), FOLLOW_RANGE))
            return AIState.GUARD_PATROL;

        chaseTarget();

        return AIState.GUARD_SEARCH_TARGET;
    }

    private void chooseTarget()
    {
        if (canHuntDownLastAttacker())
            targetEntity = this.worker.getLastAttacker();
    }

    private void stopHunting()
    {
        targetEntity = null;
        worker.addExperience(EXP_PER_MOD_DEATH);
        worker.setAIMoveSpeed((float) 1.0D);
    }

    private boolean targetCanBeAttacked()
    {
        final boolean inSight = worker.canEntityBeSeen(targetEntity);
        final boolean inReach = worker.getDistanceToEntity(targetEntity) <= MIN_ATTACK_DISTANCE;
        return inSight && inReach;
    }

    private void chaseTarget()
    {
        final float followSpeed = BASE_FOLLOW_SPEED + BASE_FOLLOW_SPEED_MULTIPLIER * worker.getExperienceLevel();
        worker.setAIMoveSpeed(followSpeed);
        worker.isWorkerAtSiteWithMove(targetEntity.getPosition(), (int) MIN_ATTACK_DISTANCE);
    }

    private boolean attackEntity(@NotNull final EntityLivingBase entityToAttack, final float baseDamage)
    {
        applyDamage(baseDamage);
        applyEnchantments();

        boolean hasKilledTarget = targetEntity.getHealth() <= 0.0F;
        if (hasKilledTarget)
            this.onKilledEntity(targetEntity);

        faceTarget(entityToAttack);
        worker.swingArm(EnumHand.MAIN_HAND);
        worker.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, (float) BASIC_VOLUME, (float) getRandomPitch());
        worker.damageItemInHand(1);
        setDelay(getReloadTime());
        attacksExecuted += 1;
        currentSearchDistance = START_SEARCH_DISTANCE;

        return hasKilledTarget;
    }

    private void applyDamage(double baseDamage)
    {
        final boolean doLowHealthBoost = worker.getHealth() <= 2;
        double damageToBeDealt = doLowHealthBoost ? 2*baseDamage : baseDamage;

        worker.resetActiveHand();

        final ItemStack heldItem = worker.getHeldItem(EnumHand.MAIN_HAND);
        damageToBeDealt += getItemDamageBonus(heldItem);

        targetEntity.attackEntityFrom(new DamageSource(worker.getName()), (float) damageToBeDealt);
        targetEntity.setRevengeTarget(worker);
    }

    private double getItemDamageBonus(ItemStack item)
    {
        double bonusDamage = 0.0D;

        if (item == null)
            return bonusDamage;

        if (ItemStackUtils.doesItemServeAsWeapon(item))
        {
            if (item.getItem() instanceof ItemSword)
                bonusDamage += ((ItemSword) item.getItem()).getDamageVsEntity();
            else
                bonusDamage += Compatibility.getAttackDamage(item);
        }

        bonusDamage += EnchantmentHelper.getModifierForCreature(item, targetEntity.getCreatureAttribute());

        return bonusDamage;
    }

    private void applyEnchantments()
    {
        final int fireAspectModifier = EnchantmentHelper.getFireAspectModifier(worker);
        if (fireAspectModifier > 0)
            targetEntity.setFire(fireAspectModifier * FIRE_CHANCE_MULTIPLIER);
    }

    private void faceTarget(@NotNull final EntityLivingBase entityToAttack)
    {
        worker.faceEntity(entityToAttack, (float) MAX_INSTANT_ROTATION, (float) MAX_INSTANT_ROTATION);
        worker.getLookHelper().setLookPositionWithEntity(entityToAttack, (float) MAX_INSTANT_ROTATION, (float) MAX_INSTANT_ROTATION);

        final double xDiff = targetEntity.posX - worker.posX;
        final double zDiff = targetEntity.posZ - worker.posZ;

        final double goToX = xDiff > 0 ? MOVE_MINIMAL : -MOVE_MINIMAL;
        final double goToZ = zDiff > 0 ? MOVE_MINIMAL : -MOVE_MINIMAL;

        worker.moveEntity(goToX, 0, goToZ);
    }

    private double getRandomPitch()
    {
        return PITCH_DIVIDER / (worker.getRNG().nextDouble() * PITCH_MULTIPLIER + BASE_PITCH);
    }

    private int getReloadTime()
    {
        return BASE_RELOAD_TIME / (worker.getExperienceLevel() + 1);
    }
}
