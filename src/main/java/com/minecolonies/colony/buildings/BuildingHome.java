package com.minecolonies.colony.buildings;

import com.minecolonies.colony.CitizenData;
import com.minecolonies.colony.Colony;
import com.minecolonies.colony.ColonyView;
import com.minecolonies.entity.EntityCitizen;
import com.minecolonies.lib.EnumGUI;
import com.minecolonies.util.Utils;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import java.util.*;

public class BuildingHome extends BuildingHut
{
    private List<CitizenData> residents = new ArrayList<CitizenData>();

    private static final String TAG_RESIDENTS = "residents";

    public BuildingHome(Colony c, ChunkCoordinates l)
    {
        super(c, l);
    }

    @Override
    public String getSchematicName() { return "Citizen"; }

    @Override
    public int getMaxInhabitants() { return 2; }

    @Override
    public int getGuiId() { return EnumGUI.CITIZEN.getID(); }

    @Override
    public void onDestroyed()
    {
        for (CitizenData citizen : residents)
        {
            if (citizen != null)
            {
                citizen.setHomeBuilding(null);
            }
        }

        super.onDestroyed();
    }

    @Override
    public void onWorldTick(TickEvent.WorldTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        if (residents.size() < getMaxInhabitants())
        {
            //  'Capture' as many citizens into this house as possible
            addHomelessCitizens();
        }
    }

    public void addHomelessCitizens()
    {
        for (CitizenData citizen : getColony().getCitizens().values())
        {
            if (citizen.getHomeBuilding() == null)
            {
                addResident(citizen);

                if (residents.size() >= getMaxInhabitants())
                {
                    break;
                }
            }
        }
    }

    private void addResident(CitizenData citizen)
    {
        residents.add(citizen);
        citizen.setHomeBuilding(this);

        markDirty();
    }

    @Override
    public void removeCitizen(CitizenData citizen)
    {
        if (residents.contains(citizen))
        {
            citizen.setHomeBuilding(null);
            residents.remove(citizen);
        }
    }

    public boolean hasResident(CitizenData citizen)
    {
        return residents.contains(citizen);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);

        residents.clear();

        NBTTagList nbtTagCitizenList = compound.getTagList(TAG_RESIDENTS, Constants.NBT.TAG_STRING);
        for(int i = 0; i < nbtTagCitizenList.tagCount(); i++)
        {
            UUID uuid = UUID.fromString(nbtTagCitizenList.getStringTagAt(i));

            CitizenData citizen = getColony().getCitizen(uuid);
            if (citizen != null)
            {
                //  Bypass addResident (which marks dirty)
                residents.add(citizen);
                citizen.setHomeBuilding(this);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);

        NBTTagList nbtTagCitizenList = new NBTTagList();
        for(CitizenData resident : residents)
        {
            nbtTagCitizenList.appendTag(new NBTTagString(resident.getId().toString()));
        }
        compound.setTag(TAG_RESIDENTS, nbtTagCitizenList);
    }

    public static class View extends BuildingHut.View
    {
        public View(ColonyView c, ChunkCoordinates l)
        {
            super(c, l);
        }

        public GuiScreen getGui(EntityPlayer player, World world, int guiId, int x, int y, int z)
        {
            if (guiId == EnumGUI.CITIZEN.getID())
            {
                //return new GuiHutCitizen(this, player, world, x, y, z);
            }

            return null;
        }
    }
}