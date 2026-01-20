package org.ethelred.games.server.bind;

import dagger.Provides;
import dagger.multibindings.IntoSet;
import org.ethelred.games.core.GameDefinition;
import org.ethelred.games.bonkers.BonkersGameDefinition;

@dagger.Module
public interface BonkersModule {
    @Provides
    @IntoSet
    static GameDefinition<?> provideGame() {
        return new BonkersGameDefinition();
    }
}
