package com.editor.photo.video.collagemaker.photoedit.di

import android.content.Context
import com.editor.photo.video.collagemaker.photoedit.data.repository.AIRepositoryImpl
import com.editor.photo.video.collagemaker.photoedit.data.repository.AssetRepositoryImpl
import com.editor.photo.video.collagemaker.photoedit.data.repository.CacheRepositoryImpl
import com.editor.photo.video.collagemaker.photoedit.data.repository.EditorRepositoryImpl
import com.editor.photo.video.collagemaker.photoedit.data.repository.ExportRepositoryImpl
import com.editor.photo.video.collagemaker.photoedit.domain.repository.AIRepository
import com.editor.photo.video.collagemaker.photoedit.domain.repository.AssetRepository
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

@Module
@InstallIn(ViewModelComponent::class)
object EditorEngineModule {

    @Provides
    @ViewModelScoped
    fun provideEditorEngine(@ApplicationContext context: Context): EditorEngine {
        return EditorEngine(context)
    }
}

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

    @Binds
    @ViewModelScoped
    abstract fun bindAssetRepository(impl: AssetRepositoryImpl): AssetRepository
}
