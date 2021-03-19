package me.devnatan.katan.core

import br.com.devsrsouza.eventkt.EventScope
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.Config
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import me.devnatan.katan.api.*
import me.devnatan.katan.api.annotations.UnstableKatanApi
import me.devnatan.katan.api.cache.Cache
import me.devnatan.katan.api.cache.UnavailableCacheProvider
import me.devnatan.katan.api.command.CommandManager
import me.devnatan.katan.api.game.GameManager
import me.devnatan.katan.api.io.FileSystem
import me.devnatan.katan.api.io.FileSystemAccessor
import me.devnatan.katan.api.plugin.KatanInit
import me.devnatan.katan.api.plugin.KatanStarted
import me.devnatan.katan.api.security.crypto.Hash
import me.devnatan.katan.api.security.permission.DefaultPermissionKeys
import me.devnatan.katan.api.service.get
import me.devnatan.katan.common.util.get
import me.devnatan.katan.core.cache.RedisCacheProvider
import me.devnatan.katan.core.crypto.BcryptHash
import me.devnatan.katan.core.database.DatabaseManager
import me.devnatan.katan.core.database.jdbc.JDBCConnector
import me.devnatan.katan.core.docker.DockerEventsListener
import me.devnatan.katan.core.docker.DockerManager
import me.devnatan.katan.core.impl.account.AccountManagerImpl
import me.devnatan.katan.core.impl.cli.CommandManagerImpl
import me.devnatan.katan.core.impl.game.GameManagerImpl
import me.devnatan.katan.core.impl.permission.PermissionManagerImpl
import me.devnatan.katan.core.impl.plugin.DefaultPluginManager
import me.devnatan.katan.core.impl.server.DockerServerManager
import me.devnatan.katan.core.impl.services.ServiceManagerImpl
import me.devnatan.katan.core.repository.JDBCAccountsRepository
import me.devnatan.katan.core.repository.JDBCServersRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.util.*
import kotlin.system.exitProcess

@OptIn(UnstableKatanApi::class)
class KatanCore(
    val config: Config,
    override val environment: KatanEnvironment,
    override val translator: Translator
) :
    Katan, CoroutineScope by CoroutineScope(Job() + CoroutineName("Katan")) {

    companion object {

        const val DATABASE_DIALECT_FALLBACK = "H2"
        const val DEFAULT_VALUE = "default"
        val logger: Logger = LoggerFactory.getLogger(Katan::class.java)

    }

    val objectMapper = ObjectMapper()
    override val platform: Platform = currentPlatform()
    val docker = DockerManager(this)
    override lateinit var accountManager: AccountManagerImpl
    override lateinit var serverManager: DockerServerManager
    override val pluginManager = DefaultPluginManager(this)
    override val serviceManager = ServiceManagerImpl(this)
    override lateinit var gameManager: GameManager
    override lateinit var cache: Cache<Any>
    override val eventBus: EventScope = EventBus()
    lateinit var hash: Hash
    lateinit var databaseManager: DatabaseManager
    override val permissionManager = PermissionManagerImpl()
    private val dockerEventsListener = DockerEventsListener(this)
    override val commandManager: CommandManager = CommandManagerImpl()
    lateinit var internalFs: FileSystem
    override lateinit var fileSystem: FileSystemAccessor

    init {
        coroutineContext[Job]!!.invokeOnCompletion {
            logger.error("[FATAL ERROR]")
            logger.error("Katan main worker has been canceled and this is not expected to happen.")
            logger.error("This will cause unexpected problems in the application.")
            logger.error("See the logs files to extract more information. Exiting process.")
            logger.trace(null, it)
            exitProcess(1)
        }

        val value = config.get("timezone", DEFAULT_VALUE)
        if (value != DEFAULT_VALUE) {
            val timezone = TimeZone.getTimeZone(value)
            System.setProperty(Katan.TIMEZONE_PROPERTY, timezone.id)
            logger.info(translator.translate("katan.timezone", timezone.displayName))
        }
    }

    private fun caching() {
        val redis = config.getConfig("redis")
        if (!redis.get("use", false)) {
            logger.warn(translator.translate("katan.redis.disabled"))
            logger.warn(translator.translate("katan.redis.alert", "https://redis.io/"))
            return
        }

        try {
            // we have to use the pool instead of the direct client due to Katan nature,
            // the default instance of Jedis (without pool) is not thread-safe
            cache = RedisCacheProvider(JedisPool(JedisPoolConfig(), redis.get("host", "localhost")))
            logger.info(translator.translate("katan.redis.ready"))
        } catch (e: Throwable) {
            cache = UnavailableCacheProvider()
            logger.error(translator.translate("katan.redis.connection-failed"))
        }
    }

    suspend fun start() {
        logger.info(
            translator.translate(
                "katan.starting",
                Katan.VERSION,
                translator.translate("katan.env.$environment").toLowerCase(translator.locale)
            )
        )
        logger.info(translator.translate("katan.platform", "$platform"))
        docker.initialize()
        databaseManager = DatabaseManager(this)
        databaseManager.connect()
        pluginManager.loadPlugins()
        serverManager = DockerServerManager(this, JDBCServersRepository(databaseManager.database as JDBCConnector))
        accountManager = AccountManagerImpl(this, JDBCAccountsRepository(databaseManager.database as JDBCConnector))
        caching()

        for (defaultPermission in DefaultPermissionKeys.DEFAULTS)
            permissionManager.registerPermissionKey(defaultPermission)

        gameManager = GameManagerImpl(this)
        pluginManager.callHandlers(KatanInit)
        serverManager.loadServers()


        hash = when (val algorithm = config.getString("security.crypto.hash")) {
            DEFAULT_VALUE, BcryptHash.NAME -> BcryptHash()
            else -> serviceManager.get<Hash>().find {
                it.name == algorithm
            } ?: throw IllegalArgumentException("Unsupported hashing algorithm: $algorithm")
        }

        logger.info(translator.translate("katan.selected-hash", hash.name))
        accountManager.loadAccounts()
        dockerEventsListener.listen()

        pluginManager.callHandlers(KatanStarted)
    }

    suspend fun close() {
        pluginManager.disableAll()
        dockerEventsListener.close()
        internalFs.close()
        cache.close()
    }

}