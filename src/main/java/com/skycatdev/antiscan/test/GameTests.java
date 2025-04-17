package com.skycatdev.antiscan.test;

//? if >=1.21.5

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.test.TestContext;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class GameTests {
    //? if <1.21.5
    /*@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)*/
    //? if >=1.21.5
    @GameTest
    public void loads(TestContext context) {
        MixinEnvironment.getCurrentEnvironment().audit();
        context.complete();
    }
}
