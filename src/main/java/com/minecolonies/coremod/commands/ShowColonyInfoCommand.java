package com.minecolonies.coremod.commands;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.coremod.colony.Colony;
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

import static com.minecolonies.coremod.commands.AbstractSingleCommand.Commands.SHOWCOLONYINFO;

/**
 * List all colonies.
 */
public class ShowColonyInfoCommand extends AbstractSingleCommand
{

    public static final  String DESC                       = "info";
    private static final String ID_TEXT                    = "§2ID: §f";
    private static final String NAME_TEXT                  = "§2 Name: §f";
    private static final String MAYOR_TEXT                 = "§2Mayor: §f";
    private static final String COORDINATES_TEXT           = "§2Coordinates: §f";
    private static final String COORDINATES_XYZ            = "§4x=§f%s §4y=§f%s §4z=§f%s";
    private static final String CITIZENS                   = "§2Citizens: §f";
    private static final String NO_COLONY_FOUND_MESSAGE    = "Colony with mayor %s not found.";
    private static final String NO_COLONY_FOUND_MESSAGE_ID = "Colony with ID %d not found.";

    /**
     * Initialize this SubCommand with it's parents.
     *
     * @param parents an array of all the parents.
     */
    public ShowColonyInfoCommand(@NotNull final String... parents)
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
        IColony tempColony = null;

        if(colonyId != null)
        {
            tempColony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(sender.getEntityWorld()).getColony(colonyId);
        }

        if (colonyId == null && args.length >= 1)
        {
            final EntityPlayer player = server.getEntityWorld().getPlayerEntityByName(args[0]);
            if (player != null)
            {
                tempColony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(sender.getEntityWorld()).getColonyByOwner(player);
            }
        }

        if (sender instanceof EntityPlayer)
        {
            final UUID mayorID = sender.getCommandSenderEntity().getUniqueID();
            if (colonyId == null)
            {
                tempColony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(sender.getEntityWorld()).getColonyByOwner(mayorID);
            }

            final EntityPlayer player = (EntityPlayer) sender;

            if (!canPlayerUseCommand(player, SHOWCOLONYINFO, colonyId))
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

        final BlockPos position = tempColony.getCenter();
        sender.sendMessage(new TextComponentString(ID_TEXT + tempColony.getID() + NAME_TEXT + tempColony.getName()));

        if(tempColony instanceof Colony)
        {
            final String mayor = ((Colony) tempColony).getPermissions().getOwnerName();
            sender.sendMessage(new TextComponentString(MAYOR_TEXT + mayor));
        }
        sender.sendMessage(new TextComponentString(CITIZENS + tempColony.getCitizens().size() + "/" + tempColony.getMaxCitizens()));
        sender.sendMessage(new TextComponentString(COORDINATES_TEXT + String.format(COORDINATES_XYZ, position.getX(), position.getY(), position.getZ())));
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
