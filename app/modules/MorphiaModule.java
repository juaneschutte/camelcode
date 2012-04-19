package modules;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.mapping.DefaultCreator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;
import com.yammer.metrics.HealthChecks;
import com.yammer.metrics.core.HealthCheck;
import models.Model;
import play.Application;
import play.Logger;

import java.net.UnknownHostException;

/**
 * @author Mathias Bogaert
 */
public class MorphiaModule extends AbstractModule {
    @Override
    protected void configure() {
        requireBinding(Application.class);
        requestStaticInjection(Model.class);
    }

    @Provides
    Datastore create(Mongo mongo, final Application application) {
        Morphia morphia = new Morphia();
        morphia.getMapper().getOptions().objectFactory = new DefaultCreator() {
            @Override
            protected ClassLoader getClassLoaderForClass(String clazz, DBObject object) {
                return application.classloader();
            }
        };
        morphia.mapPackage("models");

        Datastore datastore = morphia.createDatastore(mongo, application.configuration().getString("mongodb.db"));
        datastore.ensureIndexes();

        Logger.info("Connected to MongoDB [" + mongo.debugString() + "] database [" + datastore.getDB().getName() + "]");

        return datastore;
    }

    @Provides
    Mongo create(final Application application) {
        try {
            final Mongo mongo = new Mongo(new MongoURI(application.configuration().getString("mongodb.uri")));
            HealthChecks.register(new HealthCheck("mongo.connection") {
                @Override
                protected Result check() throws Exception {
                    try {
                        mongo.getDatabaseNames();
                        return Result.healthy(mongo.debugString());
                    } catch (MongoException e) {
                        return Result.unhealthy(e);
                    }
                }
            });

            return mongo;
        } catch (UnknownHostException e) {
            addError(e);
            return null;
        }
    }
}
