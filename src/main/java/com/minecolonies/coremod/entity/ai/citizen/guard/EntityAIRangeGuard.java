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
     * Basic delay for the next shot.
     */
    private static final int BASE_RELOAD_TIME = 60;

    /**
     * Base damage which the power enchantments added.
     */
    private static final double BASE_POWER_ENCHANTMENT_DAMAGE = 0.5D;

    /**
     * Damage per power enchantment level.
     */
    private static final double POWER_ENCHANTMENT_DAMAGE_MULTIPLIER = 0.5D;

    /**
     * Multiply the base damage always with this.
     */
    private static final double BASE_DAMAGE_MULTIPLIER = 2.0D;

    /**
     * Multiply some random with this to get some random damage addition.
     */
    private static final double RANDOM_DAMAGE_MULTPLIER = 0.25D;

    /**
     * When the difficulty is higher the damage increases by this each level.
     */
    private static final double DIFFICULTY_DAMAGE_MULTIPLIER = 0.11D;

    /**
     * Chance that the arrow lights up the target when the target is on fire.
     */
    private static final int FIRE_EFFECT_TIME = 100;

    /**
     * The pitch will be divided by this to calculate it for the arrow sound.
     */
    private static final double PITCH_DIVIDER = 1.0D;

    /**
     * The base pitch, add more to this to change the sound.
     */
    private static final double BASE_PITCH = 0.8D;

    /**
     * Random is multiplied by this to get a random arrow sound.
     */
    private static final double PITCH_MULTIPLIER = 0.4D;

    /**
     * Quantity to be moved to rotate the entity without actually moving.
     */
    private static final double MOVE_MINIMAL = 0.01D;

    /**
     * Quantity the worker should turn around all at once.
     */
    private static final float TURN_AROUND = 180F;

    /**
     * Have to aim that bit higher to hit the target.
     */
    private static final double AIM_SLIGHTLY_HIGHER_MULTIPLIER = 0.20000000298023224D;

    /**
     * Normal volume at which sounds are played at.
     */
    private static final double BASIC_VOLUME = 1.0D;

    /**
     * Guard has to aim x higher to hit his target.
     */
    private static final double AIM_HEIGHT = 3.0D;

    /**
     * Used to calculate the chance that an arrow hits, if the worker levels is higher than 15 the chance gets worse again.
     * Because of the rising fire speed.
     */
    private static final double HIT_CHANCE_DIVIDER = 15.0D;

    /**
     * The arrow travell speed.
     */
    private static final double ARROW_SPEED = 1.6D;

    /**
     * Base speed of the guard he follows his target.
     */
    private static final int BASE_FOLLOW_SPEED = 1;

    /**
     * Base multiplier increasing the attack speed each level.
     */
    private static final double BASE_FOLLOW_SPEED_MULTIPLIER = 0.25D;

    /**
     * The start search distance of the guard to track/attack entities may get more depending on the level.
     */
    private static final double MAX_ATTACK_DISTANCE = 20.0D;

    /**
     * Damage per range attack.
     */
    private static final int DAMAGE_PER_ATTACK = 2;

    /**
     * When target is out of sight, try to move that close to the target.
     */
    private static final int MOVE_CLOSE = 3;

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
        chooseTarget();

        if (!targetEntity.isEntityAlive() || needsToolOrWeapon(ToolType.BOW))
        {
            stopHunting();
            return AIState.GUARD_GATHERING;
        }

        if (targetCanBeAttacked(MAX_ATTACK_DISTANCE))
        {
            attackEntityWithRangedAttack(targetEntity, DAMAGE_PER_ATTACK);
            if (attacksExecuted >= getMaxAttacksUntilRestock())
                return AIState.GUARD_RESTOCK;
            return AIState.GUARD_HUNT_DOWN_TARGET;
        }

        final double maxRange = FOLLOW_RANGE + MAX_ATTACK_DISTANCE;
        if (shouldReturnToTarget(targetEntity.getPosition(), maxRange))
            return AIState.GUARD_PATROL;

        worker.setAIMoveSpeed((float) (BASE_FOLLOW_SPEED + BASE_FOLLOW_SPEED_MULTIPLIER * worker.getExperienceLevel()));
        worker.isWorkerAtSiteWithMove(targetEntity.getPosition(), MOVE_CLOSE);

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
        return BASE_RELOAD_TIME / (worker.getExperienceLevel() + 1);
    }

    @Override
    public void attackEntityWithRangedAttack(@NotNull final EntityLivingBase entityToAttack, final float baseDamage)
    {
        shootArrow(entityToAttack, baseDamage);
        faceTarget(entityToAttack);
        worker.swingArm(EnumHand.MAIN_HAND);
        worker.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, (float) BASIC_VOLUME, getRandomPitch());
        worker.damageItemInHand(1);
        setDelay(getReloadTime());
        attacksExecuted += 1;
    }

    private void shootArrow(final EntityLivingBase entityToAttack, final float baseDamage)
    {
        final EntityTippedArrow arrowEntity = new GuardArrow(CompatibilityUtils.getWorld(this.worker), worker);

        final double xVector = entityToAttack.posX - worker.posX;
        final double yVector = entityToAttack.getEntityBoundingBox().minY + entityToAttack.height/AIM_HEIGHT - arrowEntity.posY;
        final double zVector = entityToAttack.posZ - worker.posZ;

        final double distance = (double) MathHelper.sqrt_double(xVector*xVector + zVector*zVector);
        double damage = baseDamage;

        final double hitChance = (worker.getExperienceLevel() + 1) / HIT_CHANCE_DIVIDER;

        worker.resetActiveHand();
        final double heightCorrection = distance*AIM_SLIGHTLY_HIGHER_MULTIPLIER;
        arrowEntity.setThrowableHeading(xVector, yVector + heightCorrection, zVector, (float) ARROW_SPEED, (float) hitChance);

        if (worker.getHealth() <= 2)
            damage *= 2;

        addEffectsToArrow(arrowEntity, damage);
        CompatibilityUtils.getWorld(worker).spawnEntityInWorld(arrowEntity);
    }

    private void faceTarget(final EntityLivingBase entityToAttack)
    {
        worker.faceEntity(entityToAttack, TURN_AROUND, TURN_AROUND);
        worker.getLookHelper().setLookPositionWithEntity(entityToAttack, TURN_AROUND, TURN_AROUND);

        final double xDiff = targetEntity.posX - worker.posX;
        final double zDiff = targetEntity.posZ - worker.posZ;

        //0.01D is a small distance just to rotate worker
        final double goToX = xDiff > 0 ? MOVE_MINIMAL : -MOVE_MINIMAL;
        final double goToZ = zDiff > 0 ? MOVE_MINIMAL : -MOVE_MINIMAL;

        worker.moveEntity(goToX, 0, goToZ);
    }

    /*TODO:maybe move this into a util class*/
    private float getRandomPitch()
    {
        return (float)(PITCH_DIVIDER / (worker.getRNG().nextDouble() * PITCH_MULTIPLIER + BASE_PITCH));
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
        final double randomDmg = worker.getRandom().nextGaussian() * RANDOM_DAMAGE_MULTPLIER;

        final double difficultyLevel = CompatibilityUtils.getWorld(worker).getDifficulty().getDifficultyId();
        final double difficultyDmg = difficultyLevel * DIFFICULTY_DAMAGE_MULTIPLIER;

        arrowEntity.setDamage(baseDamage*BASE_DAMAGE_MULTIPLIER + randomDmg + difficultyDmg);
    }

    private void applyEnchantments(final EntityTippedArrow arrowEntity)
    {
        final int powerLv = EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.POWER, worker);
        if (powerLv > 0)
            arrowEntity.setDamage(arrowEntity.getDamage() + (double) powerLv * POWER_ENCHANTMENT_DAMAGE_MULTIPLIER + BASE_POWER_ENCHANTMENT_DAMAGE);

        final int punchLv = EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.PUNCH, worker);
        if (punchLv > 0)
            arrowEntity.setKnockbackStrength(punchLv);

        final DifficultyInstance difficulty = CompatibilityUtils.getWorld(worker).getDifficultyForLocation(new BlockPos(worker));
        boolean workerOnFire = worker.isBurning() && difficulty.func_190083_c() && worker.getRandom().nextBoolean();
        boolean hasFlameEnchant = EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.FLAME, worker) > 0;

        if (workerOnFire || hasFlameEnchant)
            arrowEntity.setFire(FIRE_EFFECT_TIME);
    }

    private void applyPotionTip(final EntityTippedArrow arrowEntity)
    {
        final ItemStack holdItem = worker.getHeldItem(EnumHand.OFF_HAND);
        if (holdItem != null && holdItem.getItem() == Items.TIPPED_ARROW)
            arrowEntity.setPotionEffect(holdItem);
    }
}
