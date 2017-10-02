package com.minecolonies.api.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Interface that defines a client side only view.
 */
public interface IView
{
    /**
     * Open the associated gui for this {@link IView}.
     */
    @SideOnly(Side.CLIENT)
    void openGui();
}
