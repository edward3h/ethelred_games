package org.ethelred.games.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Component;
import org.ethelred.games.core.Engine;
import org.ethelred.games.core.GameDefinition;
import org.ethelred.games.server.bind.BonkersModule;
import org.ethelred.games.server.bind.EngineModule;
import org.ethelred.games.server.bind.IdModule;
import org.ethelred.games.server.bind.NuoModule;

import javax.inject.Singleton;
import java.util.Set;
import java.util.function.LongSupplier;

@Singleton
@Component(modules = {IdModule.class, EngineModule.class, NuoModule.class, BonkersModule.class})
public interface GameEngineComponent
{
    Engine engine();

    ObjectMapper mapper();

    Set<GameDefinition<?>> gameDefinitions();

    LongSupplier idSupplier();
}
