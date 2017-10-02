package com.minecolonies.coremod.commands;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.permissions.Rank;
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

import static com.minecolonies.coremod.commands.AbstractSingleCommand.Commands.DELETECOLONY;

/**
 * List all colonies.
 */
public class DeleteColonyCommand extends AbstractSingleCommand
{

    public static final  String DESC                       = "delete";
    private static final String NO_COLONY_FOUND_MESSAGE_ID = "Colony with ID %d not found.";
    private static final String NO_ARGUMENTS               = "Please define a colony to delete";

    /**
     * Initialize this SubCommand with it's parents.
     *
     * @param parents an array of all the parents.
     */
    public DeleteColonyCommand(@NotNull final String... parents)
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
    public boolean canRankUseCommand(@NotNull final IColony colony, @NotNull final EntityPlayer player)
    {
        return colony.getPermissions().getRank(player.getUniqueID()).equals(Rank.OWNER);
    }

    @Override
    public void execute(@NotNull final MinecraftServer server, @NotNull final ICommandSender sender, @NotNull final String... args) throws CommandException
    {
        final IToken colonyId;

        IColony colony = null;

        if (args.length == 0)
        {
            if (sender instanceof EntityPlayer)
            {
                colony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(sender.getEntityWorld()).getColonyByOwner((EntityPlayer) sender);
            }

            if (colony == null)
            {
                sender.getCommandSenderEntity().sendMessage(new TextComponentString(NO_ARGUMENTS));
                return;
            }
            colonyId = colony.getID();
        }
        else
        {
            colonyId = getIthArgument(args, 0, null);
            if(colonyId != null)
            {
                colony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(sender.getEntityWorld()).getColony(colonyId);
            }
        }

        if (colony == null)
        {
            sender.getCommandSenderEntity().sendMessage(new TextComponentString(NO_COLONY_FOUND_MESSAGE_ID));
            return;
        }
        final IToken finalColonyId = colony.getID();

        if (sender instanceof EntityPlayer)
        {
            final EntityPlayer player = (EntityPlayer) sender;
            if (!canPlayerUseCommand(player, DELETECOLONY, colonyId))
            {
                sender.getCommandSenderEntity().sendMessage(new TextComponentString(NOT_PERMITTED));
                return;
            }
        }

        server.addScheduledTask(() -> IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(sender.getEntityWorld()).deleteColony(finalColonyId));
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
