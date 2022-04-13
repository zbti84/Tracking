package retrofit

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import utils.Constant

//코틀린에서 object는 무조건 싱글톤
object RetrofitClient {

    //레트로핏 클라이언트 선언
    //private lateinit var retrofitClient:Retrofit  아래와 완전히 같은 의미
    private var retrofitClient: Retrofit?=null

    //레트로핏 클라이언트 가져오기
    //함수명 : getClient
    //매개변수 : baseUrl
    //매개변수 타입 : String
    //리턴타입 : Retrofit
    //? : 리턴타입이 있을 수도 있고 없을 수도 있고
    fun getPOIClient(baseUrl: String):Retrofit?{
        Log.d("로그","RetrofitClient의 getPOIClient호출됨")

        // okhttp 인스턴스 생성
        // 거의 맨 끝에서 사용됨.
        var client=OkHttpClient.Builder()


        //인터셉터를 통한 기본 파라미터 설정
        val baseParameterInterceptor:Interceptor=(object :Interceptor{
            override fun intercept(chain: Interceptor.Chain): Response {
                Log.d("로그","RetrofitClient의 getPOIClient의 intercept함수 호출")

                //오리지널 리퀘스트
                val originalRequest=chain.request()

                //기본 쿼리 파라미터 추가하기
                val addedUrl=originalRequest.url.newBuilder().
                addQueryParameter("version",Constant.API.VERSION).
                addQueryParameter("format", Constant.API.FORMAT).
                addQueryParameter("count", Constant.API.COUNT).
                addQueryParameter("callback", Constant.API.CALLBACK).
                addQueryParameter("appkey", Constant.API.APPKEY).
                build()

                //최종요청
                val finalRequest = originalRequest.newBuilder().
                                                    url(addedUrl).
                                                    method(originalRequest.method,originalRequest.body).
                                                    build()
                Log.d("로그","POI 최종요청 URL : {$finalRequest}")
                val response = chain.proceed(finalRequest)

                if(response.code != 200){
                    Handler(Looper.getMainLooper()).post{
                        Log.d("로그","POI : 200이 아닙니다.")
                    }
                }
                return response
            }
        })

        // 위에서 설정한 기본파라매터 인터셉터를 okhttp 클라이언트에 추가한다.
        client.addInterceptor(baseParameterInterceptor)


        if(retrofitClient==null){
            retrofitClient=Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client.build())
                .build()
        }
        return retrofitClient  //끝끝내 리턴하는 것은 client
    }




    fun getRouteClient(baseUrl: String):Retrofit?{
        Log.d("로그","RetrofitClient의 getRouteClient호출됨")

        // okhttp 인스턴스 생성
        // 거의 맨 끝에서 사용됨.
        var client=OkHttpClient.Builder()


        //인터셉터를 통한 기본 파라미터 설정
        val baseParameterInterceptor:Interceptor=(object :Interceptor{
            override fun intercept(chain: Interceptor.Chain): Response {
                Log.d("로그","RetrofitClient의 getRouteClient의 intercept함수 호출")

                //오리지널 리퀘스트
                val originalRequest=chain.request()

                //기본 쿼리 파라미터 추가하기
                val addedUrl=originalRequest.url.newBuilder().
                addQueryParameter("version",Constant.API.VERSION).
                addQueryParameter("appkey", Constant.API.APPKEY).
                build()

                //최종요청
                val finalRequest = originalRequest.newBuilder().
                                url(addedUrl).
                                method(originalRequest.method,originalRequest.body).
                                build()
                Log.d("로그","Route 최종요청 URL : {$finalRequest}")
                val response = chain.proceed(finalRequest)

                if(response.code != 200){
                    Handler(Looper.getMainLooper()).post{
                        Log.d("로그","Route : 200이 아닙니다.")
                    }
                }
                return response
            }
        })

        // 위에서 설정한 기본파라매터 인터셉터를 okhttp 클라이언트에 추가한다.
        client.addInterceptor(baseParameterInterceptor)


        if(retrofitClient==null){
            retrofitClient=Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client.build())
                .build()
        }
        return retrofitClient  //끝끝내 리턴하는 것은 client
    }
}