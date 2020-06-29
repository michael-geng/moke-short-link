package org.mokesoft.art.shortlink.vertx;

import org.mokesoft.art.shortlink.service.ShortLinkAsyncService;
import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class SpringWorker extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(SpringWorker.class);

    @Autowired
    private ShortLinkAsyncService shortLinkAsyncService;

    @Override
    public void start() throws Exception {
        ServiceBinder binder = new ServiceBinder(vertx);
        //Future<Void> bookFuture = Future.future();

        binder.setAddress(ShortLinkAsyncService.ADDRESS).register(ShortLinkAsyncService.class, shortLinkAsyncService)
                .completionHandler(ar -> {
                    if (ar.succeeded()) {
                        logger.info("Async services registered");
                    } else {
                        logger.error("Async services register error");
                    }
                });


    }

}