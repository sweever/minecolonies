package com.minecolonies.api.util;

import java.util.Random;
import java.util.UUID;

public class UpgradeUtils
{

    public static UUID generateUniqueIdFromInt(int id) {
        Random tokenGenerator = new Random(id);
        return new UUID(tokenGenerator.nextLong(), tokenGenerator.nextLong());
    }
}
