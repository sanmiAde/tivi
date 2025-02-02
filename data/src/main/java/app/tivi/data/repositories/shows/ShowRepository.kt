/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tivi.data.repositories.shows

import app.tivi.data.entities.Success
import app.tivi.data.entities.TiviShow
import app.tivi.data.entities.TiviShow.Companion.EMPTY_SHOW
import app.tivi.data.instantInPast
import app.tivi.data.resultentities.ShowDetailed
import app.tivi.extensions.asyncOrAwait
import app.tivi.inject.Tmdb
import app.tivi.inject.Trakt
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import org.threeten.bp.Instant

@Singleton
class ShowRepository @Inject constructor(
    private val showStore: ShowStore,
    private val showLastRequestStore: ShowLastRequestStore,
    private val showImagesLastRequestStore: ShowImagesLastRequestStore,
    private val tmdbShowImagesDataSource: TmdbShowImagesDataSource,
    @Trakt private val traktShowDataSource: ShowDataSource,
    @Tmdb private val tmdbShowDataSource: ShowDataSource
) {
    fun observeShow(showId: Long) = showStore.observeShowDetailed(showId)

    suspend fun getShow(showId: Long): TiviShow? {
        return showStore.getShow(showId)
    }

    /**
     * Updates the show with the given id from all network sources, saves the result to the database
     */
    suspend fun updateShow(showId: Long) {
        asyncOrAwait("update_show_$showId") {
            val localShow = showStore.getShowOrEmpty(showId)

            val traktDeferred = async {
                traktShowDataSource.getShow(localShow)
            }
            val tmdbDeferred = async {
                tmdbShowDataSource.getShow(localShow)
            }

            val traktResult = traktDeferred.await()
            val tmdbResult = tmdbDeferred.await()

            showStore.updateShowFromSources(
                    showId,
                    traktResult.get() ?: EMPTY_SHOW,
                    tmdbResult.get() ?: EMPTY_SHOW
            )

            if (traktResult is Success) {
                // If the network requests were successful, update the last request timestamp
                showLastRequestStore.updateLastRequest(showId)
            }
        }
    }

    suspend fun updateShowImages(showId: Long) {
        asyncOrAwait("update_show_images_$showId") {
            val show = showStore.getShow(showId)
                    ?: throw IllegalArgumentException("Show with ID $showId does not exist")
            when (val result = tmdbShowImagesDataSource.getShowImages(show)) {
                is Success -> {
                    showStore.saveImages(showId, result.get().map { it.copy(showId = showId) })
                    showImagesLastRequestStore.updateLastRequest(showId)
                }
            }
        }
    }

    suspend fun needsImagesUpdate(showId: Long, expiry: Instant = instantInPast(days = 30)): Boolean {
        return showImagesLastRequestStore.isRequestBefore(showId, expiry)
    }

    suspend fun needsUpdate(showId: Long, expiry: Instant = instantInPast(days = 7)): Boolean {
        return showLastRequestStore.isRequestBefore(showId, expiry)
    }

    suspend fun needsInitialUpdate(showId: Long): Boolean {
        return !showLastRequestStore.hasBeenRequested(showId)
    }

    suspend fun searchShows(query: String): List<ShowDetailed> {
        return if (query.isNotBlank()) {
            showStore.searchShows(query)
        } else {
            emptyList()
        }
    }
}
