package com.minecolonies.api.reference;

import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.colony.requestsystem.token.TokenDataSerializer;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;

/**
 * Holds the references to data deserializers special to minecolonies.
 */
public class ModDataDeserializers
{

    public static DataSerializer<IToken> VARTOKEN;

    public static void init() {
        VARTOKEN = new TokenDataSerializer();
        DataSerializers.registerSerializer(VARTOKEN);
    }
}
