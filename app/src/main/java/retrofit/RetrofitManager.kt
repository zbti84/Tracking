package retrofit

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import model.POI
import model.Route
import retrofit2.Call
import retrofit2.Response
import utils.Constant
import kotlin.collections.ArrayList

class RetrofitManager {

    companion object{
        val instance=RetrofitManager()  //자기 자신을 싱글턴으로 만들어냄
    }

    //명칭검색 api호출
    fun searchPOI(searchKeyword:String?,completion:(Constant.RESPONSE_STATE,ArrayList<POI>?)->Unit){

        // 레트로핏 인터페이스 가져오기
        val iRetrofitROI : IRetrofit? = RetrofitClient.getPOIClient(Constant.API.BASE_URL)?.create(IRetrofit::class.java)

        //언랩핑작업 optional설정 때문에 Manager의 searchPOI의 keyword와 인터페이스의 keyword의 optional차이
        val keyword = searchKeyword.let {
            it  //keyword
        }?: ""

        val call = iRetrofitROI?.searchPOI(searchKeyword = keyword).let {
            it  //call
        }?: return


        //본격적인 요청
        call.enqueue(object : retrofit2.Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d("로그", "RetrofitManager - searchPOI- onFailure() called / t: $t")

                completion(Constant.RESPONSE_STATE.FAIL, null)
                //성공실패여부와 결과값을 같이 보냄
            }

            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d("로그", "RetrofitManager - searchPOI- onResponse() called ")
                when(response.code()){
                    200->{ //응답코드가 200일때만 작동할 수 있게 예를 들어 응답이 없는 경우는 작동하지 않음.
                        response.body()?.let{//body가 있다면

                            var parsePOIDataArray = ArrayList<POI>() //model.POI데이터를 받아 넣는 리스트

                            val body = it.asJsonObject

                            val searchPoiInfo=body.get("searchPoiInfo").asJsonObject

                            val total = searchPoiInfo.get("totalCount").asInt


                            // 데이터가 없으면 no_content 로 보낸다.
                            if(total==0){
                                completion(Constant.RESPONSE_STATE.NO_CONTENT, null)
                            }
                            else{ // 데이터가 있다면

                                val pois = searchPoiInfo.get("pois").asJsonObject

                                val poi = pois.getAsJsonArray("poi")

                                poi.forEach { poiItem ->
                                    val poiItemObject = poiItem.asJsonObject
                                    val name = poiItemObject.get("name").asString
                                    val frontLat= poiItemObject.get("frontLat").asString
                                    val frontLon= poiItemObject.get("frontLon").asString

                                    val POIItem = POI(
                                        name=name,
                                        frontLat=frontLat,
                                        frontLon=frontLon
                                    )
                                    parsePOIDataArray.add(POIItem)
                                }
                                completion(Constant.RESPONSE_STATE.OKAY,parsePOIDataArray)
                            }
                        }
                    }
                }
            }
        })
    }


    //경로탐색 api호출
    fun searchRoute(startX:Double, startY:Double, endX:Double, endY:Double, startname:String,endname:String,completion: (Constant.RESPONSE_STATE, ArrayList<Route>?) -> Unit){
        val iRetrofitRoute : IRetrofit? = RetrofitClient.getRouteClient(Constant.API.BASE_URL)?.create(IRetrofit::class.java)

        val startX=startX
        val startY=startY
        val endX=endX
        val endY=endY
        val startname=startname
        val endname=endname

        val call=iRetrofitRoute?.searchRoute(startX = startX,startY=startY,endX=endX,endY=endY,startName = startname,endName = endname).let{
            it
        }?:return

        call.enqueue(object : retrofit2.Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d("로그", "RetrofitManager - searchRoute - onFailure() called / t: $t")

                completion(Constant.RESPONSE_STATE.FAIL, null)
            }

            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d("로그", "RetrofitManager - searchRoute- onResponse() called ")
                when(response.code()){
                    200->{
                        response.body()?.let {
                            var parseRouteDataArray = ArrayList<Route>()

                            val body = it.asJsonObject

                            val features=body.getAsJsonArray("features")

                            var i=0

                            features.forEach { featuresItem->
                                val featureObject = featuresItem.asJsonObject
                                val geometry=featureObject.get("geometry").asJsonObject
                                val coordinates=geometry.get("coordinates")//JsonElement

                                //json타입을 List로
                                val jsonarr = Gson().fromJson(coordinates,ArrayList::class.java).listIterator()

                                while (jsonarr.hasNext()){
                                    val next = jsonarr.next()
                                    if(next !is Double){
                                        var RouteItem = Route(
                                            coordinates = next as ArrayList<Double>
                                        )
                                        parseRouteDataArray.add(RouteItem)
                                    }

                                }
                                i++
                            }
                            completion(Constant.RESPONSE_STATE.OKAY,parseRouteDataArray)
                        }
                    }
                }
            }

        })

    }



}