/*
 * Copyright 2019 Google LLC
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

package app.tivi.domain.observers

import androidx.paging.PagedList
import app.tivi.data.FlowPagedListBuilder
import app.tivi.data.repositories.popularshows.PopularShowsRepository
import app.tivi.data.resultentities.PopularEntryWithShow
import app.tivi.domain.PagingInteractor
import app.tivi.util.AppCoroutineDispatchers
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

class ObservePagedPopularShows @Inject constructor(
    dispatchers: AppCoroutineDispatchers,
    private val popularShowsRepository: PopularShowsRepository
) : PagingInteractor<ObservePagedPopularShows.Params, PopularEntryWithShow>() {
    override val dispatcher: CoroutineDispatcher = dispatchers.io

    override fun createObservable(params: Params): Flow<PagedList<PopularEntryWithShow>> {
        return FlowPagedListBuilder(
                popularShowsRepository.observeForPaging(),
                params.pagingConfig,
                boundaryCallback = params.boundaryCallback
        ).buildFlow()
    }

    data class Params(
        override val pagingConfig: PagedList.Config,
        override val boundaryCallback: PagedList.BoundaryCallback<PopularEntryWithShow>?
    ) : Parameters<PopularEntryWithShow>
}
