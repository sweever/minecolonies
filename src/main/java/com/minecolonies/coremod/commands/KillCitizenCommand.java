package com.minecolonies.coremod.commands;

import com.minecolonies.api.IAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.permissions.Rank;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.entity.Citizen;
import com.minecolonies.coremod.colony.CitizenData;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static com.minecolonies.coremod.commands.AbstractSingleCommand.Commands.KILLCITIZENS;

/**
 * List all colonies.
 */
public class KillCitizenCommand extends AbstractCitizensCommands
{

    public static final  String DESC                = "kill";
    private static final String CITIZEN_DESCRIPTION = "§2ID: §f %d §2 Name: §f %s";
    private static final String REMOVED_MESSAGE     = "Has been removed";
    private static final String COORDINATES_XYZ     = "§4x=§f%s §4y=§f%s §4z=§f%s";

    /**
     * The damage source used to kill citizens.
     */
    private static final DamageSource CONSOLE_DAMAGE_SOURCE = new DamageSource("Console");

    /**
     * Initialize this SubCommand with it's parents.
     *
     * @param parents an array of all the parents.
     */
    public KillCitizenCommand(@NotNull final String... parents)
    {
        super(parents);
    }

    @NotNull
    @Override
    public String getCommandUsage(@NotNull final ICommandSender sender)
    {
        return super.getCommandUsage(sender) + "<ColonyId> <CitizenId>";
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
        return false;
    }

    @Override
    public Commands getCommand()
    {
        return KILLCITIZENS;
    }

    @Override
    void executeSpecializedCode(@NotNull final MinecraftServer server, final ICommandSender sender, final IColony colony, final int citizenId)
    {
        final CitizenData citizenData = (CitizenData) colony.getCitizen(citizenId);
        final Citizen entityCitizen = citizenData.getCitizen();
        sender.sendMessage(new TextComponentString(String.format(CITIZEN_DESCRIPTION, citizenData.getId(), citizenData.getName())));
        final BlockPos position = entityCitizen.getPosition();
        sender.sendMessage(new TextComponentString(String.format(COORDINATES_XYZ, position.getX(), position.getY(), position.getZ())));
        sender.sendMessage(new TextComponentString(REMOVED_MESSAGE));
        server.addScheduledTask(() -> entityCitizen.onDeath(CONSOLE_DAMAGE_SOURCE));
    }

    @Override
    public boolean canPlayerUseCommand(final EntityPlayer player, final Commands theCommand, final IToken colonyId)
    {
        final IColony colony = IAPI.Holder.getApi().getServerColonyManager().getControllerForWorld(player.getEntityWorld()).getColony(colonyId);


        return super.canPlayerUseCommand(player, theCommand, colonyId)
                 && colony != null && colony.getPermissions().getRank(player.getUniqueID()).equals(Rank.OWNER);
    }
}
