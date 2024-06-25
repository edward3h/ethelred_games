package org.ethelred.games.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import io.javalin.websocket.WsContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.ethelred.games.core.Action;
import org.ethelred.games.core.Channel;
import org.ethelred.games.core.Engine;
import org.ethelred.games.core.PlayerView;
import org.ethelred.games.nuo.NuoGameDefinition;
import org.jetbrains.annotations.VisibleForTesting;
import picocli.CommandLine;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import static io.javalin.apibuilder.ApiBuilder.*;

/**
 * Server application.
 *
 * @author eharman
 * @since 2021-04-14
 */
@CommandLine.Command(name = "server", mixinStandardHelpOptions = true)
public class Main implements Runnable
{
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static final String PLAYER_ID_KEY = "playerId";
    public static final String PLAYER_NAME_KEY = "playerName";
    @VisibleForTesting
    public
    GameEngineComponent engineFactory = DaggerGameEngineComponent.create();

    @CommandLine.Mixin
    private NodeRunner.NodeOptions nodeOptions;

    @CommandLine.Option(names = {"--port"})
    @VisibleForTesting
    public
    int port = 7000;

    public static void main(String[] args)
    {
        new CommandLine(new Main()).execute(args);
    }

    private final Map<Channel, WsContext> channelToWs = new ConcurrentHashMap<>();

    private Javalin server;

    private ObjectMapper objectMapper;
    private LongSupplier idSupplier;

    @Override
    public void run()
    {
        LOGGER.info("Server starting");
        server = Javalin.create(javalinConfig -> {
            javalinConfig.requestLogger.http((ctx, ms) -> LOGGER.debug("Request {} {}", ctx.method(), ctx.url()));
            if (nodeOptions.isEnabled()) {
                javalinConfig.jetty.server(this::configureJetty);
            }
        });
        objectMapper = engineFactory.mapper();
        idSupplier = engineFactory.idSupplier();
        var engine = engineFactory.engine();
        for (var game :
                engineFactory.gameDefinitions()) {
            engine.registerGame(game);
        }
        engine.registerGame(new NuoGameDefinition());
        engine.registerCallback(this::onMessage);
        server.updateConfig(cfg -> cfg.jsonMapper(new JavalinJackson(objectMapper)));
        attach(server, engine);
        server.start(port);
        if (nodeOptions.isEnabled()) {
            try (var runner = new NodeRunner(nodeOptions)) {
                runner.run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Server configureJetty() {
        var server = new Server();
        var wrapper = new NonApiWrapper();
        var context = new ServletContextHandler();
        var holder = context.addServlet(ProxyServlet.Transparent.class, "/");
        holder.setInitParameter("proxyTo", "http://localhost:3000");
        wrapper.setHandler(context);
        server.setHandler(new HandlerCollection(wrapper));
        server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT + " \"%{X-Forwarded-For}i\""));
        return server;
    }

    @SuppressWarnings("unused") // called from groovy tests
    @VisibleForTesting
    void close()
    {
        server.stop();
    }

    private void onMessage(Channel channel, PlayerView message)
    {
        var ctx = channelToWs.get(channel);
        if (ctx != null)
        {
            ctx.send(message);
        }
    }

    private void attach(Javalin server, Engine engine)
    {
        server.routes(() -> {
            before(ctx -> {
                var cookieVal = ctx.cookie(PLAYER_ID_KEY);
                if (cookieVal == null)
                {
                    var playerId = idSupplier.getAsLong();
                    ctx.attribute(PLAYER_ID_KEY, playerId);
                    ctx.cookie(PLAYER_ID_KEY, String.valueOf(playerId));
                } else {
                    ctx.attribute(PLAYER_ID_KEY, Long.parseLong(cookieVal));
                }
            });
            path("api", () -> {
                get("games", ctx -> ctx.json(engine.gameTypes()));
                post("player/name", ctx -> {
                    var name = ctx.body();
                    if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 2) {
                        name = name.substring(1, name.length() - 1);
                    }

                    if(name.length() > 20){
                        ctx.json("Bad request: Player name is too long.").status(400);
                        return;
                    }

                    engine.playerName(getPlayerId(ctx), name);
                    ctx.cookie(PLAYER_NAME_KEY, URLEncoder.encode(name, StandardCharsets.UTF_8));
                    ctx.status(HttpStatus.NO_CONTENT);
                });
                post("{game}", ctx -> {
                    long playerId = getPlayerId(ctx);
                    handlePlayerName(ctx, engine, playerId);
                    String gameType = ctx.pathParam("game");
                    var channel = new Channel(idSupplier.getAsLong(), gameType, playerId);
                    ctx.json(
                            new ServerPlayerView(channel, engine.createGame(channel))
                    );
                });
                put("join/{shortCode}", ctx -> {
                    long playerId = getPlayerId(ctx);
                    handlePlayerName(ctx, engine, playerId);
                    ctx.json(new ServerPlayerView(engine.joinGame(playerId, ctx.pathParam("shortCode"))));
                });
                get("{game}/{gameId}", ctx -> {
                    var channel = new Channel(
                            ctx.pathParamAsClass("gameId", Long.class).get(),
                            ctx.pathParam("game"),
                            getPlayerId(ctx));
                    var view = engine.playerView(channel);
                    ctx.json(new ServerPlayerView(channel, view));
                });
                post("{game}/{gameId}", ctx -> {
                    var channel = new Channel(
                            ctx.pathParamAsClass("gameId", Long.class).get(),
                            ctx.pathParam("game"),
                            getPlayerId(ctx));
                    var view = engine.message(channel, ctx.bodyAsClass(Action.class));

                    ctx.json(new ServerPlayerView(channel, view.playerView(), view.message()));

                });
                ws("{game}/{gameId}", ws -> {
                    ws.onConnect(ctx -> {
                        var channel = new Channel(
                            ctx.pathParamAsClass("gameId", Long.class).get(),
                            ctx.pathParam("game"),
                            getPlayerId(ctx));
                        channelToWs.put(channel, ctx);
                    });
                    ws.onMessage(ctx -> {
                        var channel = new Channel(
                            ctx.pathParamAsClass("gameId", Long.class).get(),
                            ctx.pathParam("game"),
                            getPlayerId(ctx));
                        engine.message(channel, ctx.messageAsClass(Action.class));
                    });
                    ws.onClose(ctx -> {
                        var channel = new Channel(
                            ctx.pathParamAsClass("gameId", Long.class).get(),
                            ctx.pathParam("game"),
                            getPlayerId(ctx));
                        channelToWs.remove(channel);
                    });
                });
            });
        });
    }
    
    private long getPlayerId(WsContext ctx)
    {
        return Objects.requireNonNull(ctx.attribute(PLAYER_ID_KEY));
    }

    private long getPlayerId(Context ctx)
    {
        return Objects.requireNonNull(ctx.attribute(PLAYER_ID_KEY));
    }

    private void handlePlayerName(Context context, Engine engine, long playerId) {
        var name = context.cookie(PLAYER_NAME_KEY);
        if (name != null) {
            engine.playerName(playerId, name);
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ServerPlayerView(String path, PlayerView playerView, String message) {
        public ServerPlayerView(Channel channel, PlayerView playerView, String message) {
            this("/api/%s/%d".formatted(channel.gameType(), channel.gameId()), playerView, message);
        }

        public ServerPlayerView(Channel channel, PlayerView playerView) {
            this(channel, playerView, null);
        }

        public ServerPlayerView(Engine.ChannelAndView channelAndView) {
            this(channelAndView.channel(), channelAndView.view(), null);
        }
    }
}
