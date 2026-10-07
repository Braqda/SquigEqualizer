package android.os

interface IBinder

open class Binder : IBinder

object Build {
    object VERSION {
        const val SDK_INT: Int = 30
    }
    object VERSION_CODES {
        const val O: Int = 26
    }
}
