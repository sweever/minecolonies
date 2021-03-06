package com.minecolonies.coremod.colony.requestsystem.resolvers;

import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.jetbrains.annotations.NotNull;

/**
 * ----------------------- Not Documented Object ---------------------
 */
public abstract class AbstractRequestResolver<R extends IRequestable> implements IRequestResolver<R>
{

    /**
     * The default priority of a resolver.
     * 100
     */
    protected static final int CONST_DEFAULT_RESOLVER_PRIORITY = 100;

    @NotNull
    private final ILocation location;

    @NotNull
    private final IToken<?> token;

    public AbstractRequestResolver(@NotNull final ILocation location, @NotNull final IToken<?> token)
    {
        this.location = location;
        this.token = token;
    }

    @Override
    public IToken<?> getRequesterId()
    {
        return token;
    }

    @NotNull
    @Override
    public ILocation getRequesterLocation()
    {
        return location;
    }

    @NotNull
    @Override
    public ITextComponent getDisplayName(@NotNull final IToken<?> token)
    {
        return new TextComponentString("Request System");
    }

    @Override
    public int getPriority()
    {
        return CONST_DEFAULT_RESOLVER_PRIORITY;
    }
}
