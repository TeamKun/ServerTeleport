package net.teamfruit.serverteleport;

import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.text.ComponentBuilders;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerTeleportCommand implements Command {
    private final ProxyServer server;

    private final String servername;
    private final String langPrefix;
    private final String langUsage;
    private final String langNoServer;
    private final String langPlayerNum;
    private final String langPlayerName;
    private final String langSuccess;

    public ServerTeleportCommand(ProxyServer server, Toml toml) {
        this.server = server;

        this.servername = toml.getString("lobby-server");

        Toml lang = toml.getTable("lang");
        this.langPrefix = lang.getString("prefix");
        this.langUsage = lang.getString("usage");
        this.langNoServer = lang.getString("noserver");
        this.langPlayerNum = lang.getString("player-num");
        this.langPlayerName = lang.getString("player-name");
        this.langSuccess = lang.getString("success");
    }

    @Override
    public void execute(@NonNull CommandSource source, String[] args) {
        Player player = (Player) source;
        if (args.length < 1 || args[0] == null) {
            player.sendMessage(ComponentBuilders.text()
                    .content(langPrefix)
                    .append(langUsage)
                    .build()
            );
            return;
        }
        String target = args[0];
        String server = args.length < 2 ? servername : args[1];

        Collection<Player> players = Optional.ofNullable(target.startsWith("#") ? target.substring(1) : null)
                .flatMap(this.server::getServer)
                .map(RegisteredServer::getPlayersConnected)
                .orElse("@a".equals(target)
                        ? this.server.getAllPlayers()
                        : this.server.getPlayer(target).map(Arrays::asList).orElse(Collections.emptyList())
                );

        Optional<RegisteredServer> toConnect = this.server.getServer(server);
        if (!toConnect.isPresent()) {
            player.sendMessage(ComponentBuilders.text()
                    .append(ComponentBuilders.text()
                            .content(langPrefix))
                    .append(ComponentBuilders.text()
                            .append(langNoServer))
                    .build()
            );
        } else {
            player.sendMessage(ComponentBuilders.text()
                    .append(ComponentBuilders.text()
                            .content(langPrefix))
                    .append(ComponentBuilders.text()
                            .append(
                                    String.format(langSuccess,
                                            server,
                                            players.size() == 1
                                                    ? String.format(langPlayerName, players.stream().findFirst().get().getUsername())
                                                    : String.format(langPlayerNum, players.size())
                                    )
                            ))
                    .build()
            );
            players.forEach(p ->
                    p.createConnectionRequest(toConnect.get()).fireAndForget());
        }
    }

    @Override
    public List<String> suggest(CommandSource source, @NonNull String[] args) {
        if (args.length == 1)
            return Arrays.asList(
                    Stream.of("@a"),
                    this.server.getAllServers().stream().map(RegisteredServer::getServerInfo)
                            .map(ServerInfo::getName).map(e -> "#" + e),
                    this.server.getAllPlayers().stream().map(Player::getUsername)
            )
                    .stream()
                    .flatMap(Function.identity())
                    .collect(Collectors.toList());
        if (args.length == 2)
            return this.server.getAllServers()
                    .stream()
                    .map(e -> e.getServerInfo().getName())
                    .collect(Collectors.toList());
        return Collections.emptyList();
    }
}
