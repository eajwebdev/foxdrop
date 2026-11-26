package ph.foxdrop.core.network.province

import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.common.result.map
import ph.foxdrop.core.domain.province.ProvinceOption
import ph.foxdrop.core.domain.province.ProvinceRepository
import ph.foxdrop.core.network.DataEnvelope
import ph.foxdrop.core.network.FoxApiClient

@Serializable
internal data class ProvinceDto(
    val id: String,
    val name: String,
    @SerialName("region_name") val regionName: String = "",
    @SerialName("is_serviceable") val isServiceable: Boolean = false,
    @SerialName("is_available") val isAvailable: Boolean = false,
) {
    fun toDomain() = ProvinceOption(id, name, regionName, isServiceable, isAvailable)
}

internal class ApiProvinceRepository @Inject constructor(private val api: FoxApiClient) : ProvinceRepository {
    override suspend fun directory(): FoxResult<List<ProvinceOption>> =
        api.call { http -> http.get("provinces").body<DataEnvelope<List<ProvinceDto>>>().data }
            .map { rows -> rows.map(ProvinceDto::toDomain) }
}
