package com.minecolonies.api.client.colony;

import com.minecolonies.api.client.IView;
import com.minecolonies.api.colony.IColony;

public interface IColonyView<B extends IBuildingView> extends IColony<B>, IView
{
}
