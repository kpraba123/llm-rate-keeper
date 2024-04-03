package com.ebay.llm.qos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import com.ebay.llm.qos.config.Client;
import com.ebay.llm.qos.config.ClientModel;
import com.ebay.llm.qos.config.ConfigLoader;
import com.ebay.llm.qos.config.ModelClientConfig;
import com.ebay.llm.qos.constant.TokenStoreEnum;
import com.ebay.llm.qos.store.TokenStore;
import com.ebay.llm.qos.store.redis.RedisTokenStore;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class RateLimiterTest {

  @Mock
  private TokenStore tokenStore;
  @Mock
  private RedisClient redisClient;
  @Mock
  private ConfigLoader configLoader;
  @Mock
  private ModelClientConfig modelClientConfig;
  @Mock
  private Client client;
  @Mock
  private ClientModel clientModel;

  private RateLimiter rateLimiter;

  @Mock
  private StatefulRedisConnection<String, String> redisConnection;
  @Mock
  private RedisAsyncCommands<String, String> redisAsyncCommands;

  @Mock
  private RedisAsyncCommands<String, String> asyncCommands;

  @Mock
  private RedisTokenStore redisTokenStore;

  @BeforeEach
  void setUp() throws IOException {
    MockitoAnnotations.openMocks(this);
    when(configLoader.loadConfig(anyString())).thenReturn(modelClientConfig);
    when(modelClientConfig.getClientConfig("clientId")).thenReturn(
        client); // Ensure the client ID matches the one used in the test case
    when(client.getModelConfig(anyString())).thenReturn(clientModel);
    when(clientModel.getTokensLimitPerMinute()).thenReturn(100L);
    when(clientModel.getTokensLimitPerDay()).thenReturn(1000L);

    // Define behavior for RedisClient
    when(redisClient.connect()).thenReturn(redisConnection);
    when(redisConnection.async()).thenReturn(redisAsyncCommands);
    when(redisTokenStore.hasTokens(anyString(), anyString(), anyLong(), anyLong())).thenReturn(
        true);

    rateLimiter = new RateLimiter(TokenStoreEnum.REDIS, redisClient, true);
  }

  @Test
  void getTokenLimits_returnsTokenLimits() {
    Pair<Long, Long> tokenLimits = rateLimiter.getTokenLimits("1", "modelA");
    assertEquals(80L, tokenLimits.getFirst());
    assertEquals(4800L, tokenLimits.getSecond());
  }

  @Test
  @Disabled
  void isAllowed_returnsTrue_whenTokenStoreHasTokens() throws IOException {
    // In RateLimiterTest.java
    when(tokenStore.hasTokens(anyString(), anyString(), anyInt(), anyInt())).thenAnswer(
        invocation -> {
          Object[] args = invocation.getArguments();
          String clientId = (String) args[0];
          String modelId = (String) args[1];
          int tokensPerMinute = (Integer) args[2];
          int tokensPerDay = (Integer) args[3];
          return clientId.equals("1") && modelId.equals("modelA") && tokensPerMinute > 0
              && tokensPerDay > 0;
        });
    RateLimiter rateLimiter = new RateLimiter(TokenStoreEnum.REDIS, redisClient, false);

    // Act
    boolean result = rateLimiter.isAllowed("modelA", "1", 1);

    // Assert
    assertTrue(result);
  }
}