package com.lzb.clipdev.ui.mode

import com.lzb.clipboardmonitor.domain.model.ContentType
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Inject
import javax.inject.Singleton

enum class ResultRenderStyle {
    JSON_TREE,
    PLAIN_TEXT
}

data class ResultModeSpec(
    val primaryActionLabel: String,
    val typeLabel: String,
    val secondaryActionLabel: String,
    val renderStyle: ResultRenderStyle
)

interface ResultModeRegistry {
    fun resolve(contentType: ContentType): ResultModeSpec
}

@Singleton
class DefaultResultModeRegistry @Inject constructor(
    private val modeSpecs: Map<String, @JvmSuppressWildcards ResultModeSpec>
) : ResultModeRegistry {
    private val defaultSpec = ResultModeSpec(
        primaryActionLabel = "处理",
        typeLabel = "内容",
        secondaryActionLabel = "识别",
        renderStyle = ResultRenderStyle.PLAIN_TEXT
    )

    override fun resolve(contentType: ContentType): ResultModeSpec {
        return modeSpecs[contentType.name] ?: defaultSpec
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ResultModeBindModule {
    @Binds
    @Singleton
    abstract fun bindResultModeRegistry(
        impl: DefaultResultModeRegistry
    ): ResultModeRegistry
}

@Module
@InstallIn(SingletonComponent::class)
object ResultModeProvideModule {
    @Provides
    @IntoMap
    @StringKey("JSON")
    fun provideJsonModeSpec(): ResultModeSpec = ResultModeSpec(
        primaryActionLabel = "格式化",
        typeLabel = "JSON",
        secondaryActionLabel = "Kotlin",
        renderStyle = ResultRenderStyle.JSON_TREE
    )

    @Provides
    @IntoMap
    @StringKey("URL")
    fun provideUrlModeSpec(): ResultModeSpec = ResultModeSpec(
        primaryActionLabel = "总结",
        typeLabel = "URL",
        secondaryActionLabel = "打开",
        renderStyle = ResultRenderStyle.PLAIN_TEXT
    )

    @Provides
    @IntoMap
    @StringKey("CODE")
    fun provideCodeModeSpec(): ResultModeSpec = ResultModeSpec(
        primaryActionLabel = "解释",
        typeLabel = "代码",
        secondaryActionLabel = "分析",
        renderStyle = ResultRenderStyle.PLAIN_TEXT
    )

    @Provides
    @IntoMap
    @StringKey("TEXT")
    fun provideTextModeSpec(): ResultModeSpec = ResultModeSpec(
        primaryActionLabel = "摘要",
        typeLabel = "文本",
        secondaryActionLabel = "翻译",
        renderStyle = ResultRenderStyle.PLAIN_TEXT
    )

    @Provides
    @IntoMap
    @StringKey("UNKNOWN")
    fun provideUnknownModeSpec(): ResultModeSpec = ResultModeSpec(
        primaryActionLabel = "处理",
        typeLabel = "内容",
        secondaryActionLabel = "识别",
        renderStyle = ResultRenderStyle.PLAIN_TEXT
    )
}
