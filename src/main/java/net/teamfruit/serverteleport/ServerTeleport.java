package net.teamfruit.serverteleport;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(id = "serverteleport",
        name = "ServerTeleport",
        version = "${project.version}",
        description = "Move players between server",
        authors = {"Kamesuta"}
)
public class ServerTeleport {
    private final ProxyServer server;
    private final Logger logger;

    private Toml loadConfig(Path path) {
        File folder = path.toFile();
        File file = new File(folder, "config.toml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if (!file.exists()) {
            try {
                InputStream input = this.getClass().getResourceAsStream("/" + file.getName());
                Throwable t = null;

                try {
                    if (input != null) {
                        Files.copy(input, file.toPath());
                    } else {
                        file.createNewFile();
                    }
                } catch (Throwable e) {
                    t = e;
                    throw e;
                } finally {
                    if (input != null) {
                        if (t != null) {
                            try {
                                input.close();
                            } catch (Throwable ex) {
                                t.addSuppressed(ex);
                            }
                        } else {
                            input.close();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return (new Toml()).read(file);
    }

    @Inject
    public ServerTeleport(ProxyServer server, CommandManager commandManager, Logger logger, @DataDirectory Path folder) {
        this.server = server;
        this.logger = logger;

        Toml toml = this.loadConfig(folder);
        if (toml == null) {
            logger.warn("Failed to load config.toml. Shutting down.");
        } else {
            commandManager.register(new ServerTeleportCommand(server, toml), "stp", "servertp");
            logger.info("Plugin has enabled!");
        }
    }
}