package com.skycatdev.antiscan.impl;

import org.junit.jupiter.api.Test;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UtilsTest {
    @Test void oversizedDownloadIsCancelledBeforeParsing() {
        var subscriber = Utils.boundedBody().apply(mock(HttpResponse.ResponseInfo.class));
        var subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.allocate(8 * 1024 * 1024 + 1)));
        verify(subscription).cancel();
        assertThat(subscriber.getBody().toCompletableFuture()).isCompletedExceptionally();
    }
}
