package com.minecolonies.coremod.commands;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.coremod.colony.Colony;
import com.mojang.authlib.GameProfile;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * List all colonies.
 */
public class RefreshColonyCommand extends AbstractSingleCommand
{
    public static final  String DESC                       = "refresh";
    private static final String NO_COLONY_FOUND_MESSAGE_ID = "Colony with ID %d not found.";
    private static final String NO_COLONY_FOUND_MESSAGE    = "Colony with mayor %s not found.";
    private static final String REFRESH                    = "Refresh succesful!";

    /**
     * Initialize this SubCommand with it's parents.
     *
     * @param parents an array of all the parents.
     */
    public RefreshColonyCommand(@NotNull final String... parents)
    {
        super(parents);
    }

    @NotNull
    @Override
    public String getCommandUsage(@NotNull final ICommandSender sender)
    {
        return super.getCommandUsage(sender) + "<ColonyId|OwnerName>";
    }

    @Override
    public void execute(@NotNull final MinecraftServer server, @NotNull final ICommandSender sender, @NotNull final String... args) throws CommandException
    {
        IToken colonyId = getIthArgument(args, 0, null);
        IColony tempColony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(server.getEntityWorld()).getColony(colonyId);

        if (colonyId == null && args.length >= 1)
        {
            final EntityPlayer player = server.getEntityWorld().getPlayerEntityByName(args[0]);
            if (player != null)
            {
                tempColony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(server.getEntityWorld()).getColonyByOwner(player);
            }
        }

        if (sender instanceof EntityPlayer)
        {
            if (tempColony == null)
            {
                tempColony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(server.getEntityWorld()).getColonyByOwner((EntityPlayer) sender);
            }

            final EntityPlayer player = (EntityPlayer) sender;

            if (!canPlayerUseCommand(player, Commands.REFRESH_COLONY, colonyId))
            {
                sender.getCommandSenderEntity().sendMessage(new TextComponentString(NOT_PERMITTED));
                return;
            }
        }

        if (tempColony == null)
        {
            if (colonyId == null && args.length != 0)
            {
                sender.sendMessage(new TextComponentString(String.format(NO_COLONY_FOUND_MESSAGE, args[0])));
            }
            else
            {
                sender.sendMessage(new TextComponentString(String.format(NO_COLONY_FOUND_MESSAGE_ID, colonyId)));
            }
            return;
        }

        sender.getCommandSenderEntity().sendMessage(new TextComponentString(REFRESH));

        if(tempColony instanceof Colony)
        {
            ((Colony) tempColony.getPermissions()).getPermissions().restoreOwnerIfNull();
        }
    }

    @NotNull
    @Override
    public List<String> getTabCompletionOptions(
                                                 @NotNull final MinecraftServer server,
                                                 @NotNull final ICommandSender sender,
                                                 @NotNull final String[] args,
                                                 @Nullable final BlockPos pos)
    {
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(@NotNull final String[] args, final int index)
    {
        return index == 0
                 && args.length > 0
                 && !args[0].isEmpty()
                 && getIthArgument(args, 0, Integer.MAX_VALUE) == Integer.MAX_VALUE;
    }
}
