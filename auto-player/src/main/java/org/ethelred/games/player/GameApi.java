package org.ethelred.games.player;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.ethelred.games.bot.BotApi;

public interface GameApi extends BotApi {
    @RequestLine("PUT /api/join/{shortCode}")
    GameResponse joinGame(@Param("shortCode") String shortCode);

    @RequestLine("POST /api/player/name")
    @Headers("Content-Type: application/json")
    void setName(String name);

    @RequestLine("POST {path}")
    GameResponse action(@Param("path") String path,Action action);
    @RequestLine("GET {path}")
    GameResponse poll(@Param("path") String path);
}

/*
{
    "path": "/api/nuo/1058013860681072640",
    "playerView": {
        "self": {
            "id": 1058013860676878336,
            "name": "Unknown"
        },
        "shortCode": "PYBP",
        "availableActions": [
            {
                "name": "playerReady",
                "possibleArguments": [

                ]
            }
        ],
        "players": [
            {
                "id": 1058013860676878336,
                "name": "Unknown",
                "ready": false
            }
        ],
        "reversedDirection": false,
        "current": null,
        "wildColor": null,
        "nuoSelf": null,
        "nuoPlayers": null
    }
}
 */
