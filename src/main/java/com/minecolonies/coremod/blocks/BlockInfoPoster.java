package com.minecolonies.coremod.blocks;

import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.coremod.tileentities.TileEntityInfoPoster;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static net.minecraft.util.EnumFacing.NORTH;
import static net.minecraft.util.EnumFacing.fromAngle;

/**
 * Class for the minecolonies info Poster.
 */
public class BlockInfoPoster extends AbstractBlockMinecoloniesContainer<BlockInfoPoster>
{
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    /**
     * This blocks name.
     */
    private static final String BLOCK_NAME = "blockInfoPoster";

    /**
     * Constructor for the Substitution block.
     * sets the creative tab, as well as the resistance and the hardness.
     */
    public BlockInfoPoster()
    {
        super(Material.WOOD);
        initBlock();
    }

    /**
     * initialize the block
     * sets the creative tab, as well as the resistance and the hardness.
     */
    private void initBlock()
    {
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, NORTH));
        setRegistryName(BLOCK_NAME);
        setUnlocalizedName(String.format("%s.%s", Constants.MOD_ID.toLowerCase(Locale.ENGLISH), BLOCK_NAME));
    }

    @Override
    public TileEntity createNewTileEntity(@NotNull final World worldIn, final int meta)
    {
        return new TileEntityInfoPoster();
    }

    @NotNull
    @Deprecated
    @Override
    public IBlockState getStateFromMeta(final int meta)
    {
        EnumFacing enumfacing = EnumFacing.getFront(meta);

        if (enumfacing.getAxis() == EnumFacing.Axis.Y)
        {
            enumfacing = NORTH;
        }

        return this.getDefaultState().withProperty(FACING, enumfacing);
    }

    @Override
    public int getMetaFromState(final IBlockState state)
    {
        return state.getValue(FACING).getIndex();
    }

    @NotNull
    @Deprecated
    @Override
    public IBlockState withRotation(@NotNull final IBlockState state, final Rotation rot)
    {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @NotNull
    @Deprecated
    @Override
    public IBlockState withMirror(@NotNull final IBlockState state, final Mirror mirrorIn)
    {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
    }

    @Deprecated
    @Override
    public boolean isFullCube(final IBlockState state)
    {
        return false;
    }

    @Deprecated
    @Override
    public boolean isPassable(final IBlockAccess worldIn, final BlockPos pos)
    {
        return true;
    }

    @Deprecated
    @Override
    public boolean isOpaqueCube(final IBlockState state)
    {
        return false;
    }

    @Override
    public void onBlockPlacedBy(final World worldIn, final BlockPos pos, final IBlockState state, final EntityLivingBase placer, final ItemStack stack)
    {
        @NotNull final EnumFacing enumFacing = (placer == null) ? NORTH : fromAngle(placer.rotationYaw);
        this.getDefaultState().withProperty(FACING, enumFacing);
    }

    @NotNull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public boolean hasTileEntity(final IBlockState state)
    {
        return true;
    }
}
