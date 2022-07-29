package org.jetlinks.community.network.coap.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.MessageDeliverer;

@Slf4j
public class CoapServerMessageDeliverer implements MessageDeliverer {

    @Override
    public void deliverRequest(Exchange exchange) {

    }

    @Override
    public void deliverResponse(Exchange exchange, Response response) {

    }
}
