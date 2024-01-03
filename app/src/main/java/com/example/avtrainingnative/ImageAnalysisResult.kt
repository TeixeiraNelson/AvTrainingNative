package com.example.avtrainingnative

data class ImageAnalysisResult(val resultArray: ByteArray, val snr: Double) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageAnalysisResult

        if (!resultArray.contentEquals(other.resultArray)) return false
        return snr == other.snr
    }

    override fun hashCode(): Int {
        var result = resultArray.contentHashCode()
        result = 31 * result + snr.hashCode()
        return result
    }
}
