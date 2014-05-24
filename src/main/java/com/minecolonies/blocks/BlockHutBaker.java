package com.minecolonies.blocks;

import com.minecolonies.tileentities.TileEntityHutBaker;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockHutBaker extends BlockHut
{
    public final String name = "blockHutBaker";

    protected BlockHutBaker()
    {
        super();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2)
    {
        return new TileEntityHutBaker();
    }
}