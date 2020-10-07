package net.teamfruit.serverteleport;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.text.ComponentBuilders;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerTeleportCommand implements Command {
    private final ProxyServer server;

    public ServerTeleportCommand(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(@NonNull CommandSource source, String[] args) {
        Player player = (Player) source;
        if (args.length < 1 || args[0] == null) {
            player.sendMessage(ComponentBuilders.text()
                    .content("[かめすたプラグイン] ")
                    .color(TextColor.LIGHT_PURPLE)
                    .append("/stp <誰> <鯖>")
                    .color(TextColor.GREEN)
                    .build()
            );
            return;
        }
        String target = args[0];
        String server = args.length < 2 ? ServerTeleport.servername : args[1];

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
                            .color(TextColor.LIGHT_PURPLE)
                            .content("[かめすたプラグイン] "))
                    .append(ComponentBuilders.text()
                            .color(TextColor.RED)
                            .append("転送先鯖が見つかりません"))
                    .build()
            );
        } else {
            player.sendMessage(ComponentBuilders.text()
                    .append(ComponentBuilders.text()
                            .color(TextColor.LIGHT_PURPLE)
                            .content("[かめすたプラグイン] "))
                    .append(ComponentBuilders.text()
                            .color(TextColor.GREEN)
                            .append((players.size() == 1
                                    ? players.stream().findFirst().get().getUsername() + " "
                                    : players.size() + "人のプレイヤー") + "を " + server + " に転送します"))
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
