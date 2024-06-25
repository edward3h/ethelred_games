package org.ethelred.games.nuo

import com.agorapulse.gru.Gru
import com.agorapulse.gru.http.Http
import com.agorapulse.gru.jsonunit.MatchesPattern
import org.ethelred.games.server.DaggerTestGameEngineComponent
import org.ethelred.games.server.Main
import picocli.CommandLine
import spock.lang.AutoCleanup
import spock.lang.Specification

class ServerTest extends Specification {
    @AutoCleanup
    def server = new Main()

    def port = getFreePort()

    def getFreePort() {
        def port
        try (def serverSocket = new ServerSocket(0)) {
            port = serverSocket.localPort
        }
        port
    }

    def getGru() {
        Gru.create(Http.create(this))
                .prepare("http://localhost:$port")
    }

    def setup() {
        server.engineFactory = DaggerTestGameEngineComponent.create()
        server.port = port
        def cmd = new CommandLine(server)
        cmd.execute()
    }

    def 'get list of games'() {
        expect:
            gru.test {
                get '/api/games'
                expect {
                    json inline('["nuo"]')
                }
            }
    }

    def 'should get a player cookie'() {
        expect:
            gru.test {
                get '/api/games'
                expect {
                    cookie {
                        name('playerId')
                        value('1001')
                    }
                }
            }
    }

    def 'accept player cookie'() {
        expect:
        gru.test {
            get('/api/games') {cookie('playerId', '1')}
            expect {
                json inline('["nuo"]')
            }
        }
    }

    def 'start game'() {
        expect:
        // player 1001 creates game 1002
        gru.test {
            post('/api/nuo')
            expect {
                cookie {
                    name('playerId')
                    value('1001')
                }
                json(inline('''
{"path":"/api/nuo/1002", 
    "playerView":{
        "availableActions":[
        {"name":"addBot","possibleArguments":[]},
        {"name":"playerReady","possibleArguments":[]}
        ],
        "players":[
            {"name":"Unknown","self":true,"ready":false}
        ],
        "reversedDirection":false,
        "self":{"id":1001,"name":"Unknown"},
        "shortCode":"ABCD",
        "status":"PRESTART"
    }
}            
'''))
            }
        }

        // player 1003 joins game 1002
        gru.test {
            put ('/api/join/ABCD')
            expect {
                cookie {
                    name('playerId')
                    value('1003')
                }
                json(inline('''
{"path":"/api/nuo/1002", 
    "playerView":{
        "availableActions":[
        {"name":"addBot","possibleArguments":[]},
        {"name":"playerReady","possibleArguments":[]}
        ],
        "players":[
            {"name":"Unknown","self":false,"ready":false},
            {"name":"Unknown","self":true,"ready":false}
        ],
        "reversedDirection":false,
        "self":{"id":1003,"name":"Unknown"},
        "shortCode":"ABCD",
        "status":"PRESTART"
    }
}       
'''))
            }
        }

        // player 1001 set name
        gru.test {
            post('/api/player/name') {
                cookie('playerId', '1001')
                content(inline("player1"), 'text/plain')
            }
            expect {
                status(204)
            }
        }

        // player 1003 set name
        gru.test {
            post('/api/player/name') {
                cookie('playerId', '1003')
                content(inline("player2"), 'text/plain')
            }
            expect {
                status(204)
            }
        }

        // player 1004 set name
        gru.test {
            post('/api/player/name') {
                cookie('playerId', '1004')
                content(inline("Gwynnethilde Pendragon-Thorfinnsson"), 'text/plain')
            }
            expect {
                status(400)
            }
        }

        // player 1001 is ready
        gru.test {
            post('/api/nuo/1002') {
                cookie('playerId', '1001')
                json(name: "playerReady")
            }
        }

        // player 1003 is ready
        gru.test {
            post('/api/nuo/1002') {
                cookie('playerId', '1003')
                json(name: "playerReady")
            }
            expect {
                json(inline('''
{"path":"/api/nuo/1002",
"playerView":{
    "current":"${json-unit.matches:card}",
    "players":[
        {"cardCount":7,"name":"player1","self":false,"turn":true}, 
        {"cardCount":7,"name":"player2","self":true,"turn":false}
    ],
    "reversedDirection":false,
    "self":{
    "id":1003,"name":"player2",
    "hand":["${json-unit.matches:card}", "${json-unit.matches:card}", "${json-unit.matches:card}", "${json-unit.matches:card}", "${json-unit.matches:card}", "${json-unit.matches:card}", "${json-unit.matches:card}"]
    
    },
    "shortCode":"ABCD",
    "status":"IN_PROGRESS",
    "log":"${json-unit.ignore}"
}
}

'''))
                json {
                    withMatcher("card", MatchesPattern.matchesPattern(/[rgbyx][0-9sdrxw]/))
                }
            }
        }
    }
}
