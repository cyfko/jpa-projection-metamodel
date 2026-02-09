module io.github.cyfko.jpametamodel {
    requires transitive io.github.cyfko.projection;

    uses io.github.cyfko.jpametamodel.providers.PersistenceRegistryProvider;
    uses io.github.cyfko.jpametamodel.providers.ProjectionRegistryProvider;

    exports io.github.cyfko.jpametamodel.api;
    exports io.github.cyfko.jpametamodel.providers;
    exports io.github.cyfko.jpametamodel;
}