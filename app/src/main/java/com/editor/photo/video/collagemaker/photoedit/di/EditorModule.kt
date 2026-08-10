package com.editor.photo.video.collagemaker.photoedit.di

import android.content.Context
import com.editor.photo.video.collagemaker.photoedit.data.repository.AIRepositoryImpl
import com.editor.photo.video.collagemaker.photoedit.data.repository.CacheRepositoryImpl
import com.editor.photo.video.collagemaker.photoedit.data.repository.EditorRepositoryImpl
import com.editor.photo.video.collagemaker.photoedit.data.repository.ExportRepositoryImpl
import com.editor.photo.video.collagemaker.photoedit.domain.repository.AIRepository
import com.editor.photo.video.collagemaker.photoedit.domain.repository.CacheRepository
import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.domain.repository.ExportRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditorEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Provides [EditorEngine].
 *
 * Scoped to ViewModelComponent (@ViewModelScoped) so a fresh EditorEngine is created
 * per EditorSessionViewModel instance and torn down with it - matching the previous
 * behavior where EditorSessionViewModel constructed it directly in its field initializer.
 */
@Module
@InstallIn(ViewModelComponent::class)
object EditorEngineModule {

    @Provides
    @ViewModelScoped
    fun provideEditorEngine(@ApplicationContext context: Context): EditorEngine {
        return EditorEngine(context)
    }
}

/**
 * Binds domain repository interfaces to their data-layer implementations.
 *
 * ViewModelScoped to preserve the previous lifecycle: one repository instance per
 * editing session (i.e. per EditorSessionViewModel), not a single app-wide singleton.
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class RepositoryModule {

    @Binds
    @ViewModelScoped
    abstract fun bindEditorRepository(impl: EditorRepositoryImpl): EditorRepository

    @Binds
    @ViewModelScoped
    abstract fun bindCacheRepository(impl: CacheRepositoryImpl): CacheRepository

    @Binds
    @ViewModelScoped
    abstract fun bindExportRepository(impl: ExportRepositoryImpl): ExportRepository

    @Binds
    @ViewModelScoped
    abstract fun bindAIRepository(impl: AIRepositoryImpl): AIRepository
}
