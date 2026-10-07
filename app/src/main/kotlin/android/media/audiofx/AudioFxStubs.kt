package android.media.audiofx

open class DynamicsProcessing(priority: Int, audioSession: Int, config: Config?) {
    var enabled: Boolean = false
    fun release() {}
    fun setInputGainByChannelIndex(channelIndex: Int, gainDb: Float) {}
    fun setPreEqBandAllChannelsTo(bandIndex: Int, band: EqBand) {}

    class Config {
        class Builder(
            variant: Int,
            channelCount: Int,
            preEqInUse: Boolean,
            preEqBandCount: Int,
            mbcInUse: Boolean,
            mbcBandCount: Int,
            postEqInUse: Boolean,
            postEqBandCount: Int,
            limiterInUse: Boolean
        ) {
            fun build(): Config = Config()
        }
    }

    class EqBand(val enabled: Boolean, val cutoffFrequency: Float, val gain: Float)

    companion object {
        const val VARIANT_FAVOR_FREQUENCY_RESOLUTION = 0
    }
}
