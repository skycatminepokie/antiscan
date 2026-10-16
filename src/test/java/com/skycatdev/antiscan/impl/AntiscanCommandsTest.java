package com.skycatdev.antiscan.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.skycatdev.antiscan.Antiscan;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;
import java.util.function.Predicate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AntiscanCommandsTest {
    @Test void unbanParsesIpv4AndIpv6AndRejectsHostnames() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        var source = mock(CommandSourceStack.class);
        // Isolate argument parsing from operator permissions, which are unchanged.
        try (var permissions = mockStatic(Permissions.class, invocation ->
                (Predicate<CommandSourceStack>) ignored -> true)) {
            AntiscanCommands.registerCommands(dispatcher, null, null);
            for (String ip : new String[]{"192.0.2.71", "2001:db8:0:0:0:0:0:71"}) {
                for (int i = 0; i < 61; i++)
                    Antiscan.RATES.blocked(ip, new Object(), ConnectionRates.Kind.LOGIN, System.currentTimeMillis());
                assertThat(Antiscan.RATES.list(System.currentTimeMillis())).containsKey(ip);
                String argument = ip.contains(":") ? "2001:db8::71" : ip;
                assertThat(dispatcher.execute("antiscan tempban unban " + argument, source)).isEqualTo(1);
                assertThat(Antiscan.RATES.list(System.currentTimeMillis())).doesNotContainKey(ip);
            }
            assertThat(dispatcher.execute("antiscan tempban unban example.com", source)).isZero();
        }
    }
}
