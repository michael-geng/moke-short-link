package org.mokesoft.art.shortlink.handler;

import org.mokesoft.art.shortlink.common.HttpUtils;
import org.mokesoft.art.shortlink.common.ResponseResult;
import org.mokesoft.art.shortlink.form.ShortLinkForm;
import org.mokesoft.art.shortlink.service.ShortLinkAsyncService;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;


public class ShortLinkHandler {
    private static final Logger logger = LoggerFactory.getLogger(ShortLinkHandler.class);

    public static String shortLink(RoutingContext rc, ShortLinkAsyncService shortLinkAsyncService) {
        ShortLinkForm requestForm = new ShortLinkForm(rc.request().getParam("url"), rc.request().getParam("expired"));

        logger.info("short link request param：{}", requestForm.toString());

        shortLinkAsyncService.save(requestForm, ar -> {
            if (ar.succeeded()) {
                HttpUtils.fireSuccessResponse(rc.response(), HTTP_OK, ar.result());
            } else {
                logger.error("shortLink error:{} ", ar.cause().getMessage());
                HttpUtils.fireResponse(rc.response(), HTTP_INTERNAL_ERROR, ResponseResult.error(ar.cause().getMessage()));
            }
        });
        return "";
    }

    public static void get(RoutingContext rc, ShortLinkAsyncService shortLinkAsyncService) {
        shortLinkAsyncService.get(rc.request().getParam("hash"), ar -> {
            if (ar.succeeded()) {
                if (ar.result() == null || ar.result().isEmpty()) {
                    rc.response().setStatusCode(404).putHeader("content-type", "text/html; charset=utf-8").end("地址不存在");
                } else {
                    rc.response().putHeader("location", ar.result()).setStatusCode(302).end();
                }

            } else {
                //rc.fail(ar.cause());
                rc.response().setStatusCode(404).putHeader("content-type", "text/html; charset=utf-8").end(ar.cause().getMessage());
            }
        });
    }
}

