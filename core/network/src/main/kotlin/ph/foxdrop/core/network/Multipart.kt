package ph.foxdrop.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import ph.foxdrop.core.domain.media.PhotoUpload

/** Photos on a slow provincial connection take a while: allow 2 minutes per upload request. */
private const val UPLOAD_TIMEOUT_MS = 120_000L

/** A JPEG part named [name] (`valid_id`, `selfie`, …). */
internal fun FormBuilder.photo(name: String, photo: PhotoUpload) {
    append(
        name,
        photo.bytes,
        Headers.build {
            append(HttpHeaders.ContentType, "image/jpeg")
            append(HttpHeaders.ContentDisposition, "filename=\"${photo.fileName}\"")
        },
    )
}

/** A text part, skipped when null or blank. */
internal fun FormBuilder.field(name: String, value: String?) {
    if (!value.isNullOrBlank()) append(name, value)
}

/** POSTs a multipart form and decodes `{"data": ...}`. */
internal suspend inline fun <reified T> HttpClient.upload(path: String, noinline parts: FormBuilder.() -> Unit): T =
    submitFormWithBinaryData(url = path, formData = formData(parts)) {
        timeout { requestTimeoutMillis = UPLOAD_TIMEOUT_MS }
    }.data<T>()
