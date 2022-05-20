package net.teamfruit.serverteleport;

import com.moandjiezana.toml.Toml;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerTeleportCommand implements Command {
    private final ProxyServer server;

    private final String langPrefix;
    private final String langUsage;
    private final String langNoServer;
    private final String langNoPermission;
    private final String langPlayerNum;
    private final String langPlayerName;
    private final String langSuccess;

    public ServerTeleportCommand(ProxyServer server, Toml toml) {
        this.server = server;

        // Localization
        Toml lang = toml.getTable("lang");
        this.langPrefix = lang.getString("prefix");
        this.langUsage = lang.getString("usage");
        this.langNoServer = lang.getString("noserver");
        this.langNoPermission = lang.getString("nopermission");
        this.langPlayerNum = lang.getString("player-num");
        this.langPlayerName = lang.getString("player-name");
        this.langSuccess = lang.getString("success");
    }

    public int execute(CommandContext<CommandSource> ctx) {
        String[] args = ctx.getArgument("message", String.class).split(" ");
        // Permission Validation
        if (!ctx.getSource().hasPermission("servertp")) {
            ctx.getSource().sendMessage(Component.text()
                    .content(langPrefix)
                    .append(Component.text(langNoPermission))
                    .build()
            );
            return 1;
        }

        // Argument Validation
        if (args.length < 2) {
            ctx.getSource().sendMessage(Component.text()
                    .content(langPrefix)
                    .append(Component.text(langUsage))
                    .build()
            );
            return 1;
        }
        String srcArg = args[0];
        String dstArg = args[1];

        // Destination Validation
        Optional<RegisteredServer> dstOptional = dstArg.startsWith("#")
                ? this.server.getServer(dstArg.substring(1))
                : this.server.getPlayer(dstArg).flatMap(Player::getCurrentServer).map(ServerConnection::getServer);
        if (!dstOptional.isPresent()) {
            ctx.getSource().sendMessage(Component.text()
                    .append(Component.text().content(langPrefix))
                    .append(Component.text(langNoServer))
                    .build()
            );
            return 1;
        }
        RegisteredServer dst = dstOptional.get();

        // Source Validation
        List<Player> src = (
                srcArg.startsWith("#")
                        ? this.server.getServer(srcArg.substring(1)).map(RegisteredServer::getPlayersConnected).orElseGet(Collections::emptyList)
                        : "@a".equals(srcArg)
                        ? this.server.getAllPlayers()
                        : this.server.getPlayer(srcArg).map(Arrays::asList).orElseGet(Collections::emptyList)
        )
                .stream()
                .filter(p -> !dstOptional.equals(p.getCurrentServer().map(ServerConnection::getServer)))
                .collect(Collectors.toList());

        // Send Message
        ctx.getSource().sendMessage(Component.text()
                .append(Component.text()
                        .content(langPrefix))
                .append(Component.text(String.format(langSuccess,
                                dstArg,
                                src.size() == 1
                                        ? String.format(langPlayerName, src.get(0).getUsername())
                                        : String.format(langPlayerNum, src.size())
                        ))
                )
                .build()
        );

        // Run Redirect
        src.forEach(p ->
                p.createConnectionRequest(dst).fireAndForget());
        return 1;
    }

    private List<String> candidate(String arg, List<String> candidates) {
        if (arg.isEmpty())
            return candidates;
        if (candidates.contains(arg))
            return Arrays.asList(arg);
        return candidates.stream().filter(e -> e.startsWith(arg)).collect(Collectors.toList());
    }

    public List<String> suggest(CommandSource player, @NonNull String[] args) {
        // Permission Validation
        if (!player.hasPermission("servertp"))
            return Collections.emptyList();

        // Source Suggestion
        if (args.length == 1)
            return candidate(args[0],
                    Stream.of(
                            Stream.of("@a"),
                            this.server.getAllServers().stream().map(RegisteredServer::getServerInfo)
                                    .map(ServerInfo::getName).map(e -> "#" + e),
                            this.server.getAllPlayers().stream().map(Player::getUsername)
                    )
                            .flatMap(Function.identity())
                            .collect(Collectors.toList())
            );

        // Destination Suggestion
        if (args.length == 2)
            return candidate(args[1],
                    Stream.of(
                            this.server.getAllServers().stream().map(RegisteredServer::getServerInfo)
                                    .map(ServerInfo::getName).map(e -> "#" + e),
                            this.server.getAllPlayers().stream().map(Player::getUsername)
                    )
                            .flatMap(Function.identity())
                            .collect(Collectors.toList())
            );

        return Collections.emptyList();
    }
}
