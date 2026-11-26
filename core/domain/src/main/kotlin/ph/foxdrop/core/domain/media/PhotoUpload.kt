package ph.foxdrop.core.domain.media

/**
 * A photo ready to upload: JPEG bytes already shrunk on the phone (max 1600 px edge). The server re-encodes it and
 * drops EXIF/GPS data (DECISIONS 60).
 */
class PhotoUpload(val bytes: ByteArray, val fileName: String = "photo.jpg") {
    val sizeBytes: Int get() = bytes.size

    override fun equals(other: Any?): Boolean =
        other is PhotoUpload && fileName == other.fileName && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = 31 * fileName.hashCode() + bytes.contentHashCode()

    override fun toString(): String = "PhotoUpload($fileName, ${bytes.size} bytes)"
}
