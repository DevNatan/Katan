package org.katan.services.cache

import org.apache.logging.log4j.LogManager
import org.katan.config.KatanConfig
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.Protocol
import redis.clients.jedis.UnifiedJedis
import java.io.Closeable

internal class RedisCacheServiceImpl(
    private val config: KatanConfig
) : CacheService, Closeable {

    companion object {
        private val logger = LogManager.getLogger(RedisCacheServiceImpl::class.java)
    }

    private var client: UnifiedJedis? = null

    override suspend fun get(key: String): String {
        return pool { resource -> resource.get(key) }
    }

    override suspend fun set(key: String, value: String): String {
        return pool { resource -> resource.set(key, value) }
    }

    private inline fun <T> pool(block: (UnifiedJedis) -> T): T {
        if (client == null) {
            client = initClient()
        }

        return client!!.use(block)
    }

    private fun initClient(): UnifiedJedis {
        logger.info("Initializing Redis client...")

        val redisConfig = config.redis
        val connectionTimeout = redisConfig.connectionTimeout?.inWholeMilliseconds?.toInt()
        val soTimeout = redisConfig.soTimeout?.inWholeMilliseconds?.toInt()
        val clientConfig = DefaultJedisClientConfig.builder()
            .connectionTimeoutMillis(connectionTimeout ?: Protocol.DEFAULT_TIMEOUT)
            .timeoutMillis(soTimeout ?: Protocol.DEFAULT_TIMEOUT)
            .user(redisConfig.username)
            .password(redisConfig.password)
            .ssl(false)
            .database(redisConfig.database ?: Protocol.DEFAULT_DATABASE)
            .clientName("Katan")
            .build()

        if (redisConfig.clusters.isEmpty()) {
            val addr = HostAndPort(
                redisConfig.host ?: Protocol.DEFAULT_HOST,
                redisConfig.port ?: Protocol.DEFAULT_PORT
            )

            logger.info("Redis: $addr")
            return JedisPooled(
                addr,
                clientConfig
            )
        }

        val nodes = redisConfig.clusters.map {
            HostAndPort(
                it.host ?: Protocol.DEFAULT_HOST,
                it.port ?: Protocol.DEFAULT_PORT
            )
        }.toSet()
        logger.debug("Jedis cluster nodes (${nodes.size}):")
        logger.debug(nodes.joinToString(", "))

        return JedisCluster(
            nodes,
            clientConfig
        )
    }

    override fun close() {
        client?.close()
        logger.info("Redis client closed.")
    }
}
