package ph.foxdrop.core.media.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ph.foxdrop.core.media.AndroidImageCompressor
import ph.foxdrop.core.media.ImageCompressor

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MediaModule {
    @Binds
    abstract fun imageCompressor(impl: AndroidImageCompressor): ImageCompressor
}
