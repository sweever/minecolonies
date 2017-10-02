package com.minecolonies.api.client.colony;

import com.minecolonies.api.client.IView;
import com.minecolonies.api.colony.buildings.IBuilding;

public interface IBuildingView<B extends IBuildingView> extends IBuilding<B>, IView
{

}
