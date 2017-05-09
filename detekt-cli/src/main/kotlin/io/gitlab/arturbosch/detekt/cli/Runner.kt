package io.gitlab.arturbosch.detekt.cli

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.YamlConfig
import io.gitlab.arturbosch.detekt.core.DetektFacade
import io.gitlab.arturbosch.detekt.core.PathFilter
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.core.ProjectComplexityProcessor
import io.gitlab.arturbosch.detekt.core.ProjectLLOCProcessor
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Artur Bosch
 */
class Runner(private val main: Main) {

	private val configURL: URL? = main.configResource
	private val configPath: Path? = main.config

	fun execute() {
		val pathFilters = with(Main) { main.filters.letIfNonEmpty { split(";").map(::PathFilter) } }
		val rules = with(Main) { main.rules.letIfNonEmpty { split(";").map { Paths.get(it) } } }
		val config = loadConfiguration()

		val start = System.currentTimeMillis()
		val settings = ProcessingSettings(main.project, config, pathFilters,
				main.parallel, true, rules, createProcessors())
		val detektion = DetektFacade.instance(settings).run()
		Output(detektion, main).report()
		val end = System.currentTimeMillis() - start
		println("\ndetekt run within $end ms")

		SmellThreshold(config, main).check(detektion)
	}

	private fun createProcessors() = listOf(ProjectLLOCProcessor(), ProjectComplexityProcessor(), DetektProgressListener())

	private fun loadConfiguration(): Config {
		return if (configPath != null) YamlConfig.load(configPath)
		else if (configURL != null) YamlConfig.loadResource(configURL)
		else if (main.formatting) object : Config {
			override fun subConfig(key: String): Config {
				return this
			}

			override fun <T : Any> valueOrDefault(key: String, default: () -> T): T {
				@Suppress("UNCHECKED_CAST")
				return when (key) {
					"autoCorrect" -> true as T
					"useTabs" -> (main.useTabs) as T
					else -> default()
				}
			}
		} else Config.empty
	}

	private fun <T> String?.letIfNonEmpty(init: String.() -> List<T>): List<T> =
			if (this == null || this.isEmpty()) listOf<T>() else this.init()

}
