package utils

object Constant {
    object API {
        const val BASE_URL : String = "https://apis.openapi.sk.com/tmap/"

        const val APPKEY : String = "l7xx79d570bd9bf74163a98c47d00783f59f"
        const val VERSION : String ="1"
        const val FORMAT : String="json"
        const val CALLBACK : String="result"
        const val COUNT : String="2"


        const val SEARCH_POI : String = "pois"
        const val SEARCH_ROUTE : String = "routes/pedestrian"

    }
    enum class RESPONSE_STATE {
        OKAY,
        FAIL,
        NO_CONTENT
    }

}