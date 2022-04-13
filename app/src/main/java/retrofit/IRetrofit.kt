package retrofit

import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.http.*
import utils.Constant

interface IRetrofit {

    @GET(Constant.API.SEARCH_POI)
    fun searchPOI(@Query("searchKeyword")searchKeyword:String) : Call<JsonElement>
    //파라미터로 query를 넣을 건데, 넣어줄 때 매개변수 이름은 SearchTerm이고 타입은 String이다.
    //Call<JsonElement>은 반환

    @FormUrlEncoded
    @POST(Constant.API.SEARCH_ROUTE)
    fun searchRoute(@Field("startX")startX:Double,
                    @Field("startY")startY:Double,
                    @Field("endX")endX:Double,
                    @Field("endY")endY:Double,
                    @Field("startName")startName:String,
                    @Field("endName")endName:String
                    ):Call<JsonElement>


}